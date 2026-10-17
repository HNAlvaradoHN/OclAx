package io.github.hnalvaradohn.oclax.picker

import io.github.hnalvaradohn.oclax.model.ContentType
import io.github.hnalvaradohn.oclax.model.contentTypeFor

internal const val INSTALLED_APP_MIME_TYPE = "application/vnd.android.package-archive"

internal enum class PickerCategory(val label: String) {
    RECENT("Recientes"),
    PINNED("Fijados"),
    APPLICATIONS("Aplicaciones"),
    IMAGES("Imágenes"),
    DOCUMENTS("Documentos"),
    PDF("PDF"),
    APK("APK"),
    TEXT("Texto/Código"),
    VIDEO("Video"),
    AUDIO("Audio"),
    OTHER("Otros"),
}

internal fun PickerCategory.matches(
    mimeType: String,
    pinned: Boolean = false,
): Boolean {
    if (this == PickerCategory.RECENT) return true
    if (this == PickerCategory.PINNED) return pinned
    if (this == PickerCategory.APPLICATIONS) return false

    return when (contentTypeFor(mimeType)) {
        ContentType.IMAGE -> this == PickerCategory.IMAGES
        ContentType.PDF -> this == PickerCategory.PDF
        ContentType.APP -> this == PickerCategory.APK
        ContentType.DOCUMENT -> this == PickerCategory.DOCUMENTS
        ContentType.TEXT -> this == PickerCategory.TEXT
        ContentType.VIDEO -> this == PickerCategory.VIDEO
        ContentType.AUDIO -> this == PickerCategory.AUDIO
        ContentType.OTHER -> this == PickerCategory.OTHER
    }
}

internal fun installedAppsCompatible(requestedMimeTypes: List<String>): Boolean =
    PickerMimeMatcher.matches(INSTALLED_APP_MIME_TYPE, requestedMimeTypes)
