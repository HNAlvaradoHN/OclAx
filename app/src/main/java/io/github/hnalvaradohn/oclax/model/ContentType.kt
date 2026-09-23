package io.github.hnalvaradohn.oclax.model

enum class ContentType(val label: String, val glyph: String) {
    IMAGE("Imagen", "▣"),
    PDF("PDF", "PDF"),
    APP("Aplicación APK", "APK"),
    DOCUMENT("Documento", "DOC"),
    TEXT("Texto/Código", "TXT"),
    VIDEO("Video", "▶"),
    AUDIO("Audio", "♪"),
    OTHER("Archivo", "FILE"),
}

fun contentTypeFor(mimeType: String): ContentType {
    val mime = mimeType.lowercase()
    return when {
        mime.startsWith("image/") -> ContentType.IMAGE
        mime == "application/pdf" -> ContentType.PDF
        mime == "application/vnd.android.package-archive" -> ContentType.APP
        mime.startsWith("video/") -> ContentType.VIDEO
        mime.startsWith("audio/") -> ContentType.AUDIO
        isDocumentMime(mime) -> ContentType.DOCUMENT
        mime.startsWith("text/") || mime.contains("json") || mime.contains("xml") -> ContentType.TEXT
        else -> ContentType.OTHER
    }
}

fun isDocumentMime(mime: String): Boolean =
    mime.contains("msword") ||
        mime.contains("officedocument") ||
        mime.contains("openxmlformats-officedocument") ||
        mime.contains("opendocument") ||
        mime == "application/rtf"
