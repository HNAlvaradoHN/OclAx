package io.github.hnalvaradohn.oclax.share

import android.app.Activity
import android.os.Bundle
import android.widget.Toast

class ShareReceiverActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val incomingIntent = intent

        Thread {
            val message = try {
                val result = ShareIngestor(applicationContext).ingest(incomingIntent)
                when {
                    result.storedCount == 0 -> "No había contenido compatible."
                    result.clipboardKind != ShareIngestor.ClipboardKind.NONE ->
                        "Guardado en OclAx y listo para pegar."
                    else -> "Guardado en OclAx."
                }
            } catch (error: Exception) {
                "No se pudo guardar en OclAx: " + (error.message ?: "error desconocido")
            }

            runOnUiThread {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                finish()
            }
        }.start()
    }
}
