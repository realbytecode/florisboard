package dev.patrickgold.florisboard.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.content.ContextCompat
import dev.patrickgold.florisboard.ime.core.ScreenCaptureManager.RESULT_CODE
import dev.patrickgold.florisboard.ime.core.ScreenCaptureManager.RESULT_DATA
import dev.patrickgold.florisboard.ime.core.ScreenCaptureManager.RESULT_RECEIVER

/**
 * Minimal, transparent Activity whose only job is to show the
 * system “Start recording or casting?” dialog and relay the result
 * back to `ScreenCaptureManager` via a ResultReceiver.
 */
class RequestScreenCaptureActivity : ComponentActivity() {

    private val launcher = registerForActivityResult(StartActivityForResult()) { res ->
        val rr = intent.getParcelableExtra<android.os.ResultReceiver>(RESULT_RECEIVER)
        val bundle = Bundle().apply {
            putInt(RESULT_CODE, res.resultCode)
            res.data?.let { putParcelable(RESULT_DATA, it) }
        }
        rr?.send(res.resultCode, bundle)
        finish() // Always close; no UI should linger.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val captureIntent = (getSystemService(android.media.projection.MediaProjectionManager::class.java))
            .createScreenCaptureIntent()
        launcher.launch(captureIntent)
    }
}
