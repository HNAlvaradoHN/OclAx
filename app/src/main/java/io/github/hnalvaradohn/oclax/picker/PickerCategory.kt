package io.github.hnalvaradohn.oclax.picker

import androidx.annotation.StringRes
import io.github.hnalvaradohn.oclax.R
import io.github.hnalvaradohn.oclax.model.ContentType
import io.github.hnalvaradohn.oclax.model.contentTypeFor

internal const val INSTALLED_APP_MIME_TYPE = "application/vnd.android.package-archive"

internal enum class PickerCategory(@StringRes val labelRes: Int) {
    RECENT(R.string.filter_recent),
    PINNED(R.string.filter_pinned),
    APPLICATIONS(R.string.filter_applications),
    IMAGES(R.string.filter_images),
    DOCUMENTS(R.string.filter_documents),
    PDF(R.string.filter_pdf),
    APK(R.string.filter_apk),
    TEXT(R.string.filter_text),
    VIDEO(R.string.filter_video),
    AUDIO(R.string.filter_audio),
    OTHER(R.string.filter_other),
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
