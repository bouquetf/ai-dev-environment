package ai.assistant.improver.repository

import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

object RepositoryAddress {
    sealed interface Parsed {
        data class Local(val text: String) : Parsed
        data class Remote(val url: String) : Parsed
        data object Invalid : Parsed
    }

    fun parse(trimmed: String): Parsed {
        val schemeEnd = trimmed.indexOf(SCHEME_MARK)
        if (schemeEnd < 0) return parseScpOrLocal(trimmed)
        val scheme = trimmed.substring(0, schemeEnd).lowercase(Locale.ROOT)
        if (scheme !in ALLOWED_SCHEMES) return Parsed.Invalid
        val retained = scheme + trimmed.substring(schemeEnd)
        val hierarchical = trimmed.substring(schemeEnd + SCHEME_MARK.length)
        if (!authorityIsValid(hierarchical)) return Parsed.Invalid
        try {
            val uri = URI(retained)
            if (uri.userInfo != null) return Parsed.Invalid
            if (uri.host.isNullOrBlank() && retained.none { it.code > 127 }) return Parsed.Invalid
        } catch (_: URISyntaxException) {
            if (retained.none { it.code > 127 }) return Parsed.Invalid
        }
        return Parsed.Remote(retained)
    }

    private fun parseScpOrLocal(trimmed: String): Parsed {
        val at = trimmed.indexOf('@')
        if (at <= 0) return Parsed.Local(trimmed)
        val colon = trimmed.indexOf(':', startIndex = at + 1)
        if (colon < 0) return Parsed.Local(trimmed)
        val user = trimmed.substring(0, at)
        val host = trimmed.substring(at + 1, colon)
        val path = trimmed.substring(colon + 1)
        if (user.isEmpty() || host.isEmpty() || path.isEmpty()) return Parsed.Invalid
        if (user.any { it in SCP_REJECTED || it.isWhitespace() } ||
            host.any { it in SCP_REJECTED || it.isWhitespace() } ||
            path.any { it.isWhitespace() }
        ) {
            return Parsed.Invalid
        }
        return Parsed.Remote(trimmed)
    }

    private fun authorityIsValid(hierarchical: String): Boolean {
        if (hierarchical.isEmpty()) return false
        val end = hierarchical.indexOfAny(charArrayOf('/', '?', '#'))
        val authority = if (end < 0) hierarchical else hierarchical.substring(0, end)
        if (authority.isEmpty() || '@' in authority) return false
        val host = if (authority.startsWith("[")) {
            val close = authority.indexOf(']')
            if (close <= 1) return false
            authority.substring(1, close)
        } else {
            val colon = authority.lastIndexOf(':')
            if (colon >= 0) authority.substring(0, colon) else authority
        }
        return host.isNotBlank()
    }

    private const val SCHEME_MARK = "://"
    private val ALLOWED_SCHEMES = setOf("git", "http", "https")
    private const val SCP_REJECTED = ":/\\@"
}
