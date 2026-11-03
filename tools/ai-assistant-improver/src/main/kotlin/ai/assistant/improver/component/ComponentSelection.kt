package ai.assistant.improver.component

import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

data class DeselectedComponent(val repositoryPath: String, val id: ComponentId)

class ComponentSelection private constructor(private val deselected: List<DeselectedComponent>) {
    constructor() : this(emptyList())

    fun entries(): List<DeselectedComponent> = deselected.toList()

    fun components(repositoryPath: String, discovered: List<ComponentId>): List<DiscoveredComponent> {
        val rejected = deselected.filter { it.repositoryPath == repositoryPath }.map { it.id }.toSet()
        return discovered.map { DiscoveredComponent(it, it !in rejected) }
    }

    fun setSelected(repositoryPath: String, id: ComponentId, selected: Boolean): ComponentSelection {
        require(repositoryPath.isNotBlank()) { "Blank repository path" }
        val existing = deselected.indexOfFirst { it.repositoryPath == repositoryPath && it.id == id }
        if (selected) {
            if (existing < 0) return ComponentSelection(deselected.toList())
            return ComponentSelection(deselected.filterIndexed { index, _ -> index != existing })
        }
        if (existing >= 0) return ComponentSelection(deselected.toList())
        return ComponentSelection(deselected + DeselectedComponent(repositoryPath, id))
    }

    fun toXml(): String = buildString {
        append("<componentSelection>\n")
        deselected.forEach { entry ->
            append("  <deselected>\n")
            append("    <repository>").append(xmlText(entry.repositoryPath)).append("</repository>\n")
            append("    <kind>").append(entry.id.kind.wire).append("</kind>\n")
            append("    <path>").append(xmlText(entry.id.relativePath)).append("</path>\n")
            append("  </deselected>\n")
        }
        append("</componentSelection>")
    }

    override fun equals(other: Any?): Boolean = other is ComponentSelection && other.deselected == deselected

    override fun hashCode(): Int = deselected.hashCode()

    companion object {
        fun restore(entries: List<DeselectedComponent>): ComponentSelection {
            val seen = mutableSetOf<Pair<String, ComponentId>>()
            entries.forEach { entry ->
                require(entry.repositoryPath.isNotBlank()) { "Blank repository path" }
                require(seen.add(entry.repositoryPath to entry.id)) {
                    "Duplicate component: ${entry.id.kind.wire}:${entry.id.relativePath} in ${entry.repositoryPath}"
                }
            }
            return ComponentSelection(entries.toList())
        }

        fun fromXml(xml: String): ComponentSelection {
            val root = parse(xml)
            require(root.tagName == "componentSelection") { "Unexpected root element: ${root.tagName}" }
            val children = root.childNodes
            val entries = mutableListOf<DeselectedComponent>()
            for (index in 0 until children.length) {
                val node = children.item(index)
                if (node !is Element) continue
                require(node.tagName == "deselected") { "Unexpected element: ${node.tagName}" }
                entries += DeselectedComponent(
                    childText(node, "repository"),
                    ComponentId(ComponentKind.fromWire(childText(node, "kind")), childText(node, "path")),
                )
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
    }
}
