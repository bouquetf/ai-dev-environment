package ai.assistant.improver.install

import ai.assistant.improver.assistant.AssistantId
import ai.assistant.improver.component.ComponentId
import ai.assistant.improver.component.ComponentKind

sealed interface InstallTarget {
    val projectRelative: String

    data class Directory(override val projectRelative: String) : InstallTarget

    data class File(override val projectRelative: String) : InstallTarget
}

object InstallLayout {
    private val folderRule = Regex("""^(?:\.cursor|\.claude|\.opencode|\.junie)/rules/(.+)$""")

    fun target(component: ComponentId, assistant: AssistantId): InstallTarget? {
        val root = rootOf(assistant)
        return when (component.kind) {
            ComponentKind.SKILL -> InstallTarget.Directory("$root/skills/${leaf(component.relativePath)}")
            ComponentKind.AGENT -> InstallTarget.File("$root/agents/${leaf(component.relativePath)}")
            ComponentKind.RULE -> ruleTarget(component.relativePath, assistant, root)
        }
    }

    private fun ruleTarget(path: String, assistant: AssistantId, root: String): InstallTarget? = when (path) {
        "CLAUDE.md" -> if (assistant == AssistantId.CLAUDE) InstallTarget.File("CLAUDE.md") else null
        ".junie/guidelines.md" ->
            if (assistant == AssistantId.JUNIE) InstallTarget.File(".junie/guidelines.md") else null
        else -> {
            val relative = folderRule.matchEntire(path)?.groupValues?.get(1)
                ?: throw IllegalArgumentException("Unsupported rule path: $path")
            InstallTarget.File("$root/rules/$relative")
        }
    }

    private fun leaf(path: String): String = path.substringAfterLast('/')

    private fun rootOf(assistant: AssistantId): String = when (assistant) {
        AssistantId.CURSOR -> ".cursor"
        AssistantId.CLAUDE -> ".claude"
        AssistantId.OPENCODE -> ".opencode"
        AssistantId.JUNIE -> ".junie"
    }
}
