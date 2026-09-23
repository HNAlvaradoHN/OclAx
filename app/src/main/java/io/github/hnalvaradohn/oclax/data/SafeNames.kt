package io.github.hnalvaradohn.oclax.data

object SafeNames {
    private const val MAX_DISPLAY_NAME = 120
    private val invalidCharacters = Regex("[\\u0000-\\u001F\\\\/:*?\"<>|]")
    private val safeExtension = Regex("[A-Za-z0-9]{1,16}")

    fun sanitize(raw: String?, fallback: String = "archivo"): String {
        val leaf = raw
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.replace(invalidCharacters, "_")
            ?.trim()
            ?.trim('.')
            .orEmpty()

        val candidate = if (leaf.isBlank()) fallback else leaf
        return candidate.take(MAX_DISPLAY_NAME)
    }

    fun extension(displayName: String): String? {
        val value = displayName.substringAfterLast('.', missingDelimiterValue = "")
        return value.takeIf { it.isNotBlank() && safeExtension.matches(it) }?.lowercase()
    }

    fun storageFileName(displayName: String, mimeType: String): String {
        val ext = extension(displayName) ?: extensionForMime(mimeType)
        return if (ext == null) "content" else "content.$ext"
    }

    private fun extensionForMime(mimeType: String): String? = when (mimeType.lowercase()) {
        "text/plain" -> "txt"
        "application/pdf" -> "pdf"
        "application/vnd.android.package-archive" -> "apk"
        "application/msword" -> "doc"
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
        "application/zip" -> "zip"
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        "video/mp4" -> "mp4"
        "audio/mpeg" -> "mp3"
        else -> null
    }
}
