package io.github.hnalvaradohn.oclax.data

internal const val RECENT_SCREENSHOT_WINDOW_MILLIS = 60_000L

internal fun looksLikeScreenshot(
    displayName: String?,
    relativePath: String?,
): Boolean {
    val name = displayName.orEmpty().lowercase()
    val path = relativePath.orEmpty().lowercase()
    return name.contains("screenshot") ||
        name.contains("screen_shot") ||
        name.contains("captura de pantalla") ||
        path.contains("/screenshots/") ||
        path.endsWith("/screenshots") ||
        path.contains("screenshots/")
}

internal fun shouldDeleteScreenshotSourceOnExpiry(
    mimeType: String,
    sourceIsMediaStore: Boolean,
    displayName: String?,
    relativePath: String?,
    sourceAddedAt: Long?,
    importedAt: Long,
): Boolean {
    if (!mimeType.startsWith("image/")) return false
    if (!sourceIsMediaStore) return false
    if (!looksLikeScreenshot(displayName, relativePath)) return false

    val addedAt = sourceAddedAt ?: return false
    val age = importedAt - addedAt
    return age in 0..RECENT_SCREENSHOT_WINDOW_MILLIS
}
