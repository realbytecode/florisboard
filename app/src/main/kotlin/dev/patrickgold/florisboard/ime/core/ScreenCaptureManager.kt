package dev.patrickgold.florisboard.ime.core

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.RequestScreenCaptureActivity
import dev.patrickgold.florisboard.lib.devtools.LogTopic
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.florisboard.lib.devtools.flogInfo
import dev.patrickgold.florisboard.lib.devtools.flogWarning
import java.lang.ref.WeakReference


/** Continuous MediaProjection session; fresh screenshot each AI-key press, single permission dialog. */
object ScreenCaptureManager {

    // ---------------------------------------------------------------------------------------------

    interface ScreenCaptureListener {
        fun onScreenshotCaptured(bitmap: Bitmap)
        fun onPermissionDenied()
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var latestBitmap: Bitmap? = null            // most recent frame
    private var latestTimestampNs = 0L                  // timestamp of ^ (nanoseconds)
    private var lastDeliveredTimestampNs = 0L           // last frame we sent to service


    private var listenerRef: WeakReference<ScreenCaptureListener>? = null

    // ---------------------------------------------------------------------------------------------

    fun setListener(l: ScreenCaptureListener?) {
        listenerRef = l?.let { WeakReference(it) }
    }

    /** Entry-point from the AI-suggest key. */
    fun requestScreenshot(context: Context) {
        if (mediaProjection == null) {
            startPermissionFlow(context)
        } else {
            deliverFreshFrame(context)
        }
    }


    // -------------------------------- Permission flow -------------------------------------------

    private fun startPermissionFlow(ctx: Context) {
        val intent = Intent(ctx, RequestScreenCaptureActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(RESULT_RECEIVER, buildResultReceiver(ctx))
        }
        ctx.startActivity(intent)
        flogInfo { "RequestScreenCaptureActivity launched." }
    }

    private fun buildResultReceiver(ctx: Context): ResultReceiver {
        val mpm = ctx.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(code: Int, data: Bundle) {
                if (code != Activity.RESULT_OK) {
                    listenerRef?.get()?.onPermissionDenied(); return
                }
                val svc = ctx as? Service ?: run {
                    flogError { "Context is not a Service" }; return
                }
                // Promote foreground ------------------------------------------------------------
                createNotificationChannel(ctx)
                val notif = NotificationCompat.Builder(ctx, NOTIF_CHANNEL)
                    .setSmallIcon(R.drawable.ic_ai_suggest)
                    .setContentTitle(ctx.getString(R.string.floris_app_name))
                    .setContentText(ctx.getString(R.string.screen_sharing_active))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build()
                svc.startForeground(
                    NOTIF_ID, notif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )

                // Obtain projection -------------------------------------------------------------
                val projIntent: Intent = data.getParcelable(RESULT_DATA)!!
                val projCode = data.getInt(RESULT_CODE)
                mediaProjection = mpm.getMediaProjection(projCode, projIntent).apply {
                    registerCallback(object : MediaProjection.Callback() {
                        override fun onStop() {
                            flogWarning(LogTopic.IMS_EVENTS) { "MediaProjection stopped by system." }
                            release()
                        }
                    }, Handler(Looper.getMainLooper()))
                }
                flogInfo { "MediaProjection session started." }
                setupVirtualDisplay(ctx)
                deliverFreshFrame(ctx)   // first frame
            }
        }
    }

    // ----------------------------- VirtualDisplay / ImageReader ---------------------------------

    private fun setupVirtualDisplay(ctx: Context) {
        if (virtualDisplay != null && imageReader != null) return
        val mp = mediaProjection ?: return

        val dm = DisplayMetrics().apply {
            val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.defaultDisplay.getRealMetrics(this)
        }
        imageReader = ImageReader.newInstance(
            dm.widthPixels, dm.heightPixels,
            PixelFormat.RGBA_8888, /*maxImages*/2
        ).also { ir ->
            // Permanent listener keeps pipeline warm
            ir.setOnImageAvailableListener({ reader ->
                reader.acquireLatestImage()?.let { img ->
                    val planes = img.planes
                    val buf = planes[0].buffer
                    val w = img.width
                    val h = img.height
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * w
                    latestBitmap = Bitmap.createBitmap(
                        w + rowPadding / pixelStride, h, Bitmap.Config.ARGB_8888
                    ).apply { copyPixelsFromBuffer(buf) }
                    latestTimestampNs = img.timestamp
                    img.close()
                }
            }, Handler(Looper.getMainLooper()))
        }

        virtualDisplay = mp.createVirtualDisplay(
            "FlorisScreenCapture",
            dm.widthPixels, dm.heightPixels, dm.densityDpi,
            android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface, null, null
        )
    }

    // -------------------------------- Frame delivery --------------------------------------------

    /** Waits up to 300 ms for a frame newer than the last delivered one, then sends it. */
    private fun deliverFreshFrame(ctx: Context) {
        setupVirtualDisplay(ctx)             // ensures the permanent listener is running

        val start = SystemClock.elapsedRealtime()
        val handler = Handler(Looper.getMainLooper())

        fun tryDeliver() {
            if (latestBitmap != null && latestTimestampNs > lastDeliveredTimestampNs) {
                lastDeliveredTimestampNs = latestTimestampNs

                // Copy so FlorisImeService can save/manipulate safely.
                val safeCopy = latestBitmap!!.copy(
                    latestBitmap!!.config ?: Bitmap.Config.ARGB_8888,
                    /* mutable = */ false
                )
                listenerRef?.get()?.onScreenshotCaptured(safeCopy)
                return
            }

            // No fresh frame yet → poll every display frame, but stop after 300 ms.
            if (SystemClock.elapsedRealtime() - start < 300) {
                handler.postDelayed(::tryDeliver, 16)      // ~1 frame at 60 Hz
            } else {
                flogWarning { "Timed out waiting for fresh screenshot." }
            }
        }

        tryDeliver()
    }

    // -------------------------------- Cleanup ---------------------------------------------------

    fun release() {
        virtualDisplay?.release(); virtualDisplay = null
        imageReader?.close(); imageReader = null
        mediaProjection?.stop(); mediaProjection = null
        latestBitmap = null

        // THE ONLY FIX NEEDED: Ensure the notification is dismissed when the session is released.
        (listenerRef?.get() as? Service)?.stopForeground(true)

        listenerRef?.clear()
        flogInfo { "ScreenCaptureManager released." }
    }

    // -------------------------------- Notification helpers -------------------------------------

    private fun createNotificationChannel(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(NOTIF_CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    NOTIF_CHANNEL, "Screen Capture",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    // -------------------------------- Constants -------------------------------------------------

    private const val NOTIF_CHANNEL = "screen_capture"
    private const val NOTIF_ID = 1

    const val RESULT_RECEIVER = "result_receiver"
    const val RESULT_CODE = "result_code"
    const val RESULT_DATA = "result_data"
}
