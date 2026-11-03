package ai.assistant.improver.install

import ai.assistant.improver.component.ComponentId
import ai.assistant.improver.component.ComponentKind
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

data class InstalledFile(
    val repositoryPath: String,
    val component: ComponentId,
    val sourcePath: String,
    val destinationPath: String,
    val sha256: String,
)

class InstallationSnapshot private constructor(private val files: List<InstalledFile>) {
    constructor() : this(emptyList())

    fun files(): List<InstalledFile> = files.toList()

    fun toXml(): String = buildString {
        append("<installationSnapshot>\n")
        files.forEach { file ->
            append("  <file>\n")
            append("    <repository>").append(xmlText(file.repositoryPath)).append("</repository>\n")
            append("    <kind>").append(file.component.kind.wire).append("</kind>\n")
            append("    <component>").append(xmlText(file.component.relativePath)).append("</component>\n")
            append("    <source>").append(xmlText(file.sourcePath)).append("</source>\n")
            append("    <destination>").append(xmlText(file.destinationPath)).append("</destination>\n")
            append("    <sha256>").append(file.sha256).append("</sha256>\n")
            append("  </file>\n")
        }
        append("</installationSnapshot>")
    }

    override fun equals(other: Any?): Boolean = other is InstallationSnapshot && other.files == files

    override fun hashCode(): Int = files.hashCode()

    companion object {
        private val digest = Regex("[0-9a-f]{64}")

        fun restore(files: List<InstalledFile>): InstallationSnapshot {
            val destinations = mutableSetOf<String>()
            files.forEach { file ->
                require(file.repositoryPath.isNotBlank()) { "Blank repository path" }
                require(file.sourcePath.isNotBlank()) { "Blank source path" }
                require(file.destinationPath.isNotBlank()) { "Blank destination path" }
                require('\\' !in file.sourcePath) { "Snapshot path contains a backslash: ${file.sourcePath}" }
                require('\\' !in file.destinationPath) {
                    "Snapshot path contains a backslash: ${file.destinationPath}"
                }
                require(!file.sourcePath.startsWith('/')) { "Snapshot path is absolute: ${file.sourcePath}" }
                require(!file.destinationPath.startsWith('/')) {
                    "Snapshot path is absolute: ${file.destinationPath}"
                }
                require(digest.matches(file.sha256)) { "Invalid content digest: ${file.sha256}" }
                require(destinations.add(file.destinationPath)) {
                    "Duplicate destination: ${file.destinationPath}"
                }
            }
            return InstallationSnapshot(files.toList())
        }

        fun fromXml(xml: String): InstallationSnapshot {
            val root = parse(xml)
            require(root.tagName == "installationSnapshot") { "Unexpected root element: ${root.tagName}" }
            val children = root.childNodes
            val files = mutableListOf<InstalledFile>()
            for (index in 0 until children.length) {
                val node = children.item(index)
                if (node !is Element) continue
                require(node.tagName == "file") { "Unexpected element: ${node.tagName}" }
                files += InstalledFile(
                    childText(node, "repository"),
                    ComponentId(ComponentKind.fromWire(childText(node, "kind")), childText(node, "component")),
                    childText(node, "source"),
                    childText(node, "destination"),
                    childText(node, "sha256"),
                )
            }
            return restore(files)
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
    }
}
