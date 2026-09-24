package io.github.hnalvaradohn.oclax.picker

import io.github.hnalvaradohn.oclax.model.ContentType
import io.github.hnalvaradohn.oclax.model.contentTypeFor

internal enum class PickerCategory(val label: String) {
    RECENT("Recientes"),
    PINNED("Fijados"),
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

internal fun defaultPickerCategory(requestedMimeTypes: Collection<String>): PickerCategory {
    if (requestedMimeTypes.size != 1) return PickerCategory.RECENT

    val requested = requestedMimeTypes.first()
        .substringBefore(';')
        .trim()
        .lowercase()

    return when {
        requested == "image/*" -> PickerCategory.IMAGES
        requested == "video/*" -> PickerCategory.VIDEO
        requested == "audio/*" -> PickerCategory.AUDIO
        requested == "text/*" -> PickerCategory.TEXT
        requested == "application/pdf" -> PickerCategory.PDF
        requested == "application/vnd.android.package-archive" -> PickerCategory.APK
        requested == "*/*" || requested.endsWith("/*") -> PickerCategory.RECENT
        requested.contains('/') -> when (contentTypeFor(requested)) {
            ContentType.IMAGE -> PickerCategory.IMAGES
            ContentType.PDF -> PickerCategory.PDF
            ContentType.APP -> PickerCategory.APK
            ContentType.DOCUMENT -> PickerCategory.DOCUMENTS
            ContentType.TEXT -> PickerCategory.TEXT
            ContentType.VIDEO -> PickerCategory.VIDEO
            ContentType.AUDIO -> PickerCategory.AUDIO
            ContentType.OTHER -> PickerCategory.RECENT
        }
        else -> PickerCategory.RECENT
    }
}
