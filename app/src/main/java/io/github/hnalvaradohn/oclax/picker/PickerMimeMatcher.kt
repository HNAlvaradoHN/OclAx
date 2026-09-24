package io.github.hnalvaradohn.oclax.picker

object PickerMimeMatcher {
    fun matches(
        candidateMimeType: String,
        requestedMimeTypes: Collection<String>,
    ): Boolean {
        val candidate = normalize(candidateMimeType) ?: return false
        if (requestedMimeTypes.isEmpty()) return true

        val requested = requestedMimeTypes.mapNotNull(::normalize)
        if (requested.isEmpty()) return false

        return requested.any { request ->
            when {
                request == "*/*" -> true
                request == candidate -> true
                request.endsWith("/*") ->
                    candidate.substringBefore('/') == request.substringBefore('/')
                else -> false
            }
        }
    }

    private fun normalize(value: String): String? {
        val normalized = value
            .substringBefore(';')
            .trim()
            .lowercase()

        if (normalized == "*/*") return normalized

        val parts = normalized.split('/')
        if (parts.size != 2) return null
        if (parts[0].isBlank() || parts[1].isBlank()) return null
        if (parts[0].contains('*')) return null
        if (parts[1].contains('*') && parts[1] != "*") return null

        return normalized
    }
}
