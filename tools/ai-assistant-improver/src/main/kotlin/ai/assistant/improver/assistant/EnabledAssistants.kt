package ai.assistant.improver.assistant

import org.w3c.dom.Element
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

class EnabledAssistants private constructor(private val enabled: Set<AssistantId>) {
    constructor() : this(emptySet())

    fun enabledIds(): List<AssistantId> = AssistantCatalog.assistants.filter { it in enabled }

    fun enable(id: AssistantId): EnabledAssistants = EnabledAssistants(enabled + id)

    fun disable(id: AssistantId): EnabledAssistants = EnabledAssistants(enabled - id)

    fun enableWire(wire: String): EnabledAssistants = enable(AssistantId.fromWire(wire))

    fun disableWire(wire: String): EnabledAssistants = disable(AssistantId.fromWire(wire))

    fun toXml(): String = buildString {
        append("<enabledAssistants>\n")
        enabledIds().forEach { append("  <id>").append(it.wire).append("</id>\n") }
        append("</enabledAssistants>")
    }

    override fun equals(other: Any?): Boolean = other is EnabledAssistants && other.enabled == enabled

    override fun hashCode(): Int = enabled.hashCode()

    override fun toString(): String = "EnabledAssistants(${enabledIds().joinToString { it.wire }})"

    companion object {
        fun fromWires(wires: List<String>): EnabledAssistants {
            val ids = mutableSetOf<AssistantId>()
            wires.forEach { wire ->
                val id = AssistantId.fromWire(wire)
                require(ids.add(id)) { "Duplicate assistant: ${id.wire}" }
            }
            return EnabledAssistants(ids)
        }

        fun fromXml(xml: String): EnabledAssistants {
            val root = parse(xml)
            require(root.tagName == "enabledAssistants") { "Unexpected root element: ${root.tagName}" }
            val nodes = root.getElementsByTagName("id")
            return fromWires((0 until nodes.length).map { nodes.item(it).textContent.trim() })
        }

        private fun parse(xml: String): Element =
            DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(InputSource(StringReader(xml.trim())))
                .documentElement
    }
}
