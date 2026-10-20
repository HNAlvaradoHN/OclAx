package io.github.hnalvaradohn.oclax.share

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import io.github.hnalvaradohn.oclax.R

class ShareReceiverActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val incomingIntent = intent

        Thread {
            val message = try {
                val result = ShareIngestor(applicationContext).ingest(incomingIntent)
                when {
                    result.storedCount == 0 -> getString(R.string.share_none_compatible)
                    result.clipboardKind != ShareIngestor.ClipboardKind.NONE ->
                        getString(R.string.share_saved_clipboard)
                    else -> getString(R.string.share_saved)
                }
            } catch (error: Exception) {
                getString(
                    R.string.share_save_failed,
                    error.message ?: getString(R.string.unknown_error),
                )
            }

            runOnUiThread {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish()
            }
        }.start()
    }
}
