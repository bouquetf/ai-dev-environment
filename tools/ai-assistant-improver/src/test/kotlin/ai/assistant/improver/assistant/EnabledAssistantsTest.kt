package ai.assistant.improver.assistant

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class EnabledAssistantsTest {
    @Test
    fun `initial state enables nothing`() {
        assertTrue(EnabledAssistants().enabledIds().isEmpty())
    }

    @Test
    fun `enable adds assistants without mutating the original`() {
        val initial = EnabledAssistants()
        val result = initial.enable(AssistantId.CURSOR).enable(AssistantId.JUNIE)
        assertEquals(listOf(AssistantId.CURSOR, AssistantId.JUNIE), result.enabledIds())
        assertTrue(initial.enabledIds().isEmpty())
    }

    @Test
    fun `enable is idempotent`() {
        val once = EnabledAssistants().enable(AssistantId.CLAUDE)
        assertEquals(once, once.enable(AssistantId.CLAUDE))
    }

    @Test
    fun `disable removes an assistant without mutating the original`() {
        val enabled = EnabledAssistants().enable(AssistantId.CURSOR)
        assertTrue(enabled.disable(AssistantId.CURSOR).enabledIds().isEmpty())
        assertEquals(listOf(AssistantId.CURSOR), enabled.enabledIds())
    }

    @Test
    fun `disable on empty state stays empty`() {
        assertTrue(EnabledAssistants().disable(AssistantId.CURSOR).enabledIds().isEmpty())
    }

    @Test
    fun `unknown identifier is refused`() {
        val enable = assertThrows<UnknownAssistantException> { EnabledAssistants().enableWire("copilot") }
        assertEquals("copilot", enable.wire)
        val disable = assertThrows<UnknownAssistantException> { EnabledAssistants().disableWire("copilot") }
        assertEquals("copilot", disable.wire)
    }

    @Test
    fun `disabling never deletes installed files`(@TempDir projectDir: Path) {
        val installedSkill = projectDir.resolve(".cursor/skills/specification-fr/SKILL.md")
        Files.createDirectories(installedSkill.parent)
        Files.writeString(installedSkill, "installed")

        EnabledAssistants().enable(AssistantId.CURSOR).disable(AssistantId.CURSOR)
        val xml = EnabledAssistants().enable(AssistantId.CURSOR).toXml()
        EnabledAssistants.fromXml(xml).disable(AssistantId.CURSOR)

        assertEquals("installed", Files.readString(installedSkill))
    }

    @Test
    fun `xml writes catalog order and round trips`() {
        val state = EnabledAssistants().enable(AssistantId.JUNIE).enable(AssistantId.CURSOR)
        val xml = state.toXml()
        assertTrue(xml.indexOf("<id>cursor</id>") < xml.indexOf("<id>junie</id>"))
        assertEquals(xml, EnabledAssistants.fromXml(xml).toXml())
        assertEquals(state, EnabledAssistants.fromXml(xml))
    }

    @Test
    fun `empty document restores empty state`() {
        assertEquals(EnabledAssistants(), EnabledAssistants.fromXml("<enabledAssistants>\n</enabledAssistants>"))
    }

    @Test
    fun `xml with unknown id is refused`() {
        assertThrows<UnknownAssistantException> {
            EnabledAssistants.fromXml("<enabledAssistants><id>copilot</id></enabledAssistants>")
        }
    }

    @Test
    fun `xml with duplicate id is refused`() {
        assertThrows<IllegalArgumentException> {
            EnabledAssistants.fromXml("<enabledAssistants><id>cursor</id><id>cursor</id></enabledAssistants>")
        }
    }
}
