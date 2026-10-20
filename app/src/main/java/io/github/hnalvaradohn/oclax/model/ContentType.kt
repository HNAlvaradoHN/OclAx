package io.github.hnalvaradohn.oclax.model

import androidx.annotation.StringRes
import io.github.hnalvaradohn.oclax.R

enum class ContentType(@StringRes val labelRes: Int, val symbol: String) {
    IMAGE(R.string.filter_images, "▣"),
    PDF(R.string.filter_pdf, "PDF"),
    APP(R.string.filter_apk, "APK"),
    DOCUMENT(R.string.filter_documents, "DOC"),
    TEXT(R.string.filter_text, "TXT"),
    VIDEO(R.string.filter_video, "▶"),
    AUDIO(R.string.filter_audio, "♪"),
    OTHER(R.string.filter_other, "FILE"),
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

fun ContentType.supportsClipboardCopy(): Boolean =
    this == ContentType.TEXT || this == ContentType.IMAGE
