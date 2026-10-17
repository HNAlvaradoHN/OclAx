package io.github.hnalvaradohn.oclax.provider

internal const val INSTALLED_APK_MIME_TYPE = "application/vnd.android.package-archive"

internal sealed interface InstalledAppDocumentRef {
    val packageName: String

    data class Single(
        override val packageName: String,
    ) : InstalledAppDocumentRef

    data class Directory(
        override val packageName: String,
    ) : InstalledAppDocumentRef

    data class Part(
        override val packageName: String,
        val index: Int,
    ) : InstalledAppDocumentRef
}

internal object InstalledAppDocumentIds {
    private const val SINGLE_PREFIX = "installed-app-single:"
    private const val DIRECTORY_PREFIX = "installed-app-dir:"
    private const val PART_PREFIX = "installed-app-part:"
    private const val MAX_PACKAGE_NAME_LENGTH = 255

    fun single(packageName: String): String = SINGLE_PREFIX + requirePackageName(packageName)

    fun directory(packageName: String): String = DIRECTORY_PREFIX + requirePackageName(packageName)

    fun part(packageName: String, index: Int): String {
        require(index >= 0) { "El índice del APK no puede ser negativo." }
        return PART_PREFIX + requirePackageName(packageName) + ":" + index
    }

    fun parse(documentId: String): InstalledAppDocumentRef? =
        when {
            documentId.startsWith(SINGLE_PREFIX) ->
                parsePackage(documentId.removePrefix(SINGLE_PREFIX))
                    ?.let(InstalledAppDocumentRef::Single)

            documentId.startsWith(DIRECTORY_PREFIX) ->
                parsePackage(documentId.removePrefix(DIRECTORY_PREFIX))
                    ?.let(InstalledAppDocumentRef::Directory)

            documentId.startsWith(PART_PREFIX) -> {
                val remainder = documentId.removePrefix(PART_PREFIX)
                val separator = remainder.lastIndexOf(':')
                if (separator <= 0 || separator == remainder.lastIndex) {
                    null
                } else {
                    val packageName = parsePackage(remainder.substring(0, separator))
                    val index = remainder.substring(separator + 1).toIntOrNull()
                    if (packageName == null || index == null || index < 0) {
                        null
                    } else {
                        InstalledAppDocumentRef.Part(packageName, index)
                    }
                }
            }

            else -> null
        }

    private fun requirePackageName(packageName: String): String =
        requireNotNull(parsePackage(packageName)) { "Nombre de paquete inválido." }

    private fun parsePackage(packageName: String): String? =
        packageName
            .takeIf { it.isNotBlank() && it.length <= MAX_PACKAGE_NAME_LENGTH }
            ?.takeIf { value ->
                value.all { char ->
                    char.isLetterOrDigit() || char == '.' || char == '_'
                }
            }
}
