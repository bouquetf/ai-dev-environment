package ai.assistant.improver.repository

import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.IOException
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory

class SourceRepositories private constructor(private val stored: List<SourceRepository>) {
    constructor() : this(emptyList())

    fun entries(): List<SourceRepository> = stored.toList()

    fun add(rawPath: String): SourceRepositories {
        val trimmed = rawPath.trim()
        if (trimmed.isEmpty()) {
            throw RepositoryRejectedException(RepositoryRejectedException.Reason.EMPTY, trimmed)
        }
        val candidate = try {
            Path.of(trimmed)
        } catch (_: InvalidPathException) {
            throw RepositoryRejectedException(RepositoryRejectedException.Reason.NOT_A_DIRECTORY, trimmed)
        }
        if (!Files.exists(candidate) || !Files.isDirectory(candidate)) {
            throw RepositoryRejectedException(RepositoryRejectedException.Reason.NOT_A_DIRECTORY, trimmed)
        }
        if (!Files.exists(candidate.resolve(".git"))) {
            throw RepositoryRejectedException(RepositoryRejectedException.Reason.NOT_A_GIT_REPOSITORY, trimmed)
        }
        val canonical = candidate.toRealPath().toString()
        if (stored.any { it.path == canonical }) {
            throw RepositoryRejectedException(RepositoryRejectedException.Reason.DUPLICATE, trimmed)
        }
        return SourceRepositories(stored + SourceRepository(canonical, RepositoryMode.DOWNLOAD_ONLY))
    }

    fun setMode(path: String, mode: RepositoryMode): SourceRepositories {
        val index = indexOf(path)
        if (index < 0) throw IllegalArgumentException("Unknown repository: $path")
        val updated = stored.toMutableList()
        updated[index] = stored[index].copy(mode = mode)
        return SourceRepositories(updated)
    }

    fun remove(path: String): SourceRepositories {
        val index = indexOf(path)
        if (index < 0) return SourceRepositories(stored.toList())
        return SourceRepositories(stored.filterIndexed { i, _ -> i != index })
    }

    fun toXml(): String = buildString {
        append("<sourceRepositories>\n")
        stored.forEach { repository ->
            append("  <repository>\n")
            append("    <path>").append(xmlText(repository.path)).append("</path>\n")
            append("    <mode>").append(repository.mode.wire).append("</mode>\n")
            append("  </repository>\n")
        }
        append("</sourceRepositories>")
    }

    override fun equals(other: Any?): Boolean = other is SourceRepositories && other.stored == stored

    override fun hashCode(): Int = stored.hashCode()

    private fun indexOf(path: String): Int {
        val direct = stored.indexOfFirst { it.path == path }
        if (direct >= 0) return direct
        val canonical = canonicalIfExists(path) ?: return -1
        return stored.indexOfFirst { it.path == canonical }
    }

    companion object {
        fun restore(entries: List<SourceRepository>): SourceRepositories {
            val paths = mutableSetOf<String>()
            entries.forEach { entry ->
                require(entry.path.isNotBlank()) { "Blank repository path" }
                require(paths.add(entry.path)) { "Duplicate repository: ${entry.path}" }
            }
            return SourceRepositories(entries.toList())
        }

        fun fromXml(xml: String): SourceRepositories {
            val root = parse(xml)
            require(root.tagName == "sourceRepositories") { "Unexpected root element: ${root.tagName}" }
            val children = root.childNodes
            val entries = mutableListOf<SourceRepository>()
            for (index in 0 until children.length) {
                val node = children.item(index)
                if (node !is Element) continue
                require(node.tagName == "repository") { "Unexpected element: ${node.tagName}" }
                entries += SourceRepository(childText(node, "path"), RepositoryMode.fromWire(childText(node, "mode")))
            }
            return restore(entries)
        }

        private fun childText(parent: Element, tag: String): String {
            val children = parent.childNodes
            val matches = (0 until children.length)
                .map { children.item(it) }
                .filterIsInstance<Element>()
                .filter { it.tagName == tag }
            require(matches.size == 1) { "Missing element: $tag" }
            return matches.single().textContent.trim()
        }

        private fun parse(xml: String): Element =
            DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(InputSource(StringReader(xml.trim())))
                .documentElement

        private fun xmlText(value: String): String = buildString {
            value.forEach { character ->
                when (character) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    else -> append(character)
                }
            }
        }

        private fun canonicalIfExists(path: String): String? =
            try {
                val candidate = Path.of(path)
                if (!Files.exists(candidate)) null else candidate.toRealPath().toString()
            } catch (_: InvalidPathException) {
                null
            } catch (_: IOException) {
                null
            }
    }
}
