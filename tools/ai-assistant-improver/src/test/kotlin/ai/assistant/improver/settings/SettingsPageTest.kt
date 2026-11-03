package ai.assistant.improver.settings

import ai.assistant.improver.assistant.AssistantCatalog
import ai.assistant.improver.assistant.AssistantId
import ai.assistant.improver.assistant.EnabledAssistants
import ai.assistant.improver.i18n.Labels
import ai.assistant.improver.i18n.LanguagePreference
import ai.assistant.improver.i18n.UiLanguage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JCheckBox

class SettingsPageTest {
    private class MemoryAssistantStore(var value: EnabledAssistants = EnabledAssistants()) : AssistantStore {
        override fun read(): EnabledAssistants = value
        override fun write(value: EnabledAssistants) {
            this.value = value
        }
    }

    private class MemoryLanguageStore(var value: LanguagePreference = LanguagePreference()) : LanguageStore {
        override fun read(): LanguagePreference = value
        override fun write(value: LanguagePreference) {
            this.value = value
        }
    }

    private val assistants = MemoryAssistantStore()
    private val language = MemoryLanguageStore()
    private val page = AssistantSettingsConfigurable(assistants, language)
    private val panel = page.createComponent() as SettingsPanel

    private fun check(wire: String): JCheckBox = panel.assistantChecks().first { it.name == wire }

    @Test
    fun `initial page shows four unchecked English assistants`() {
        val checks = panel.assistantChecks()
        assertEquals(listOf("cursor", "claude", "opencode", "junie"), checks.map { it.name })
        assertEquals(listOf("Cursor", "Claude", "OpenCode", "Junie"), checks.map { it.text })
        assertTrue(checks.none { it.isSelected })
        assertEquals(UiLanguage.ENGLISH, panel.languageCombo().selectedItem)
        assertFalse(page.isModified)
        assertEquals(Labels.load(UiLanguage.ENGLISH).text("settings.page.title"), page.displayName)
    }

    @Test
    fun `checking an assistant enables it on apply`() {
        check("cursor").doClick()
        assertTrue(page.isModified)
        page.apply()
        assertEquals(listOf(AssistantId.CURSOR), assistants.value.enabledIds())
        assertFalse(page.isModified)
    }

    @Test
    fun `unchecking an assistant disables it on apply`() {
        check("cursor").doClick()
        page.apply()
        check("cursor").doClick()
        page.apply()
        assertTrue(assistants.value.enabledIds().isEmpty())
    }

    @Test
    fun `disabling never deletes installed files`(@TempDir projectDir: Path) {
        val installedSkill = projectDir.resolve(".cursor/skills/specification-fr/SKILL.md")
        Files.createDirectories(installedSkill.parent)
        Files.writeString(installedSkill, "installed")

        check("cursor").doClick()
        page.apply()
        check("cursor").doClick()
        page.apply()

        assertTrue(assistants.value.enabledIds().isEmpty())
        assertEquals("installed", Files.readString(installedSkill))
    }

    @Test
    fun `switching to French relabels the page and persists the language`() {
        panel.languageCombo().selectedItem = UiLanguage.FRENCH
        assertEquals("Langue", panel.languageHeading().text)
        assertEquals("Français", panel.languageLabel(UiLanguage.FRENCH))
        page.apply()
        assertEquals(LanguagePreference(UiLanguage.FRENCH), language.value)
        assertEquals(Labels.load(UiLanguage.FRENCH).text("settings.page.title"), page.displayName)
    }

    @Test
    fun `reset restores stored state`() {
        check("claude").doClick()
        page.reset()
        assertFalse(check("claude").isSelected)
        assertTrue(assistants.value.enabledIds().isEmpty())
        assertFalse(page.isModified)
    }

    @Test
    fun `only catalog identifiers are offered`() {
        assertEquals(AssistantCatalog.assistants.map { it.wire }, panel.assistantChecks().map { it.name })
    }
}
