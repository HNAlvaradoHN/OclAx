package io.github.hnalvaradohn.oclax.platform.transfer

import android.content.Context

internal enum class RuntimeStartupStage {
    START_REQUESTED,
    PREPARING_PRIVATE_CONFIG,
    PRIVATE_CONFIG_READY,
    PROCESS_STARTED,
    REST_READY,
    PRIVATE_MODE_VERIFIED,
    STOPPED,
}

internal data class RuntimeStartupSnapshot(
    val stage: RuntimeStartupStage?,
    val failureStage: RuntimeStartupStage?,
    val failureMessage: String?,
    val exitCode: Int?,
    val logOffset: Long,
)

internal class RuntimeStartupDiagnostics(context: Context) {
    companion object {
        private const val PREFS_NAME = "oclax_transfer_runtime_diagnostics"
        private const val PREF_STAGE = "stage"
        private const val PREF_FAILURE_STAGE = "failure_stage"
        private const val PREF_FAILURE_MESSAGE = "failure_message"
        private const val PREF_EXIT_CODE = "exit_code"
        private const val PREF_HAS_EXIT_CODE = "has_exit_code"
        private const val PREF_LOG_OFFSET = "log_offset"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun beginAttempt(logOffset: Long) {
        prefs.edit()
            .clear()
            .putString(PREF_STAGE, RuntimeStartupStage.START_REQUESTED.name)
            .putLong(PREF_LOG_OFFSET, logOffset.coerceAtLeast(0L))
            .commit()
    }

    fun markStage(stage: RuntimeStartupStage) {
        prefs.edit()
            .putString(PREF_STAGE, stage.name)
            .remove(PREF_FAILURE_STAGE)
            .remove(PREF_FAILURE_MESSAGE)
            .remove(PREF_EXIT_CODE)
            .putBoolean(PREF_HAS_EXIT_CODE, false)
            .apply()
    }

    fun markFailure(
        stage: RuntimeStartupStage,
        message: String?,
    ) {
        prefs.edit()
            .putString(PREF_FAILURE_STAGE, stage.name)
            .putString(PREF_FAILURE_MESSAGE, sanitizeRuntimeDiagnosticText(message))
            .apply()
    }

    fun markExited(exitCode: Int) {
        prefs.edit()
            .putInt(PREF_EXIT_CODE, exitCode)
            .putBoolean(PREF_HAS_EXIT_CODE, true)
            .apply()
    }

    fun snapshot(): RuntimeStartupSnapshot = RuntimeStartupSnapshot(
        stage = prefs.getString(PREF_STAGE, null)?.let(::runtimeStageOrNull),
        failureStage = prefs.getString(PREF_FAILURE_STAGE, null)?.let(::runtimeStageOrNull),
        failureMessage = prefs.getString(PREF_FAILURE_MESSAGE, null),
        exitCode = if (prefs.getBoolean(PREF_HAS_EXIT_CODE, false)) {
            prefs.getInt(PREF_EXIT_CODE, -1)
        } else {
            null
        },
        logOffset = prefs.getLong(PREF_LOG_OFFSET, 0L).coerceAtLeast(0L),
    )

    private fun runtimeStageOrNull(value: String): RuntimeStartupStage? =
        runCatching { RuntimeStartupStage.valueOf(value) }.getOrNull()
}

internal fun formatRuntimeStartupFailure(
    snapshot: RuntimeStartupSnapshot,
    safeLogLine: String?,
): String {
    val stage = snapshot.failureStage ?: snapshot.stage
    val stageText = when (stage) {
        RuntimeStartupStage.START_REQUESTED -> "Android recibió la solicitud, pero el servicio no avanzó."
        RuntimeStartupStage.PREPARING_PRIVATE_CONFIG -> "El fallo ocurrió preparando la configuración privada."
        RuntimeStartupStage.PRIVATE_CONFIG_READY -> "La configuración quedó lista, pero el proceso nativo no arrancó."
        RuntimeStartupStage.PROCESS_STARTED -> "El proceso nativo arrancó, pero su API local no llegó a responder."
        RuntimeStartupStage.REST_READY -> "La API local respondió, pero falló el endurecimiento privado."
        RuntimeStartupStage.PRIVATE_MODE_VERIFIED -> "El motor estaba listo, pero la prueba no pudo completarse."
        RuntimeStartupStage.STOPPED -> "El motor se detuvo antes de completar la prueba."
        null -> "El motor no informó en qué etapa falló."
    }

    val details = buildList {
        snapshot.exitCode?.let { add("código de salida $it") }
        snapshot.failureMessage
            ?.takeIf { it.isNotBlank() }
            ?.let { add(it) }
        safeLogLine
            ?.takeIf { it.isNotBlank() }
            ?.let { add("último registro: $it") }
    }

    return if (details.isEmpty()) {
        stageText
    } else {
        "$stageText ${details.joinToString(" · ")}"
    }
}

internal fun sanitizeRuntimeDiagnosticText(raw: String?): String? {
    if (raw.isNullOrBlank()) return null

    return raw
        .replace(Regex("""/data/(?:user|data|app)/[^\s]+"""), "<ruta privada>")
        .replace(Regex("""\b[A-Z2-7]{7}(?:-[A-Z2-7]{7}){7}\b"""), "<device-id>")
        .replace(Regex("""\b(?:\d{1,3}\.){3}\d{1,3}\b"""), "<ip>")
        .replace(Regex("""[A-Za-z0-9_-]{32,}"""), "<valor privado>")
        .trim()
        .take(240)
}
