package ai.assistant.improver.settings

import ai.assistant.improver.assistant.AssistantCatalog
import ai.assistant.improver.assistant.AssistantId
import ai.assistant.improver.assistant.EnabledAssistants
import ai.assistant.improver.i18n.Labels
import ai.assistant.improver.i18n.LanguagePreference
import ai.assistant.improver.i18n.UiLanguage
import ai.assistant.improver.repository.RepositoryMode
import ai.assistant.improver.repository.SourceRepositories
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.Component
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer

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

    private class MemoryRepositoryStore(var value: SourceRepositories = SourceRepositories()) : RepositoryStore {
        override fun read(): SourceRepositories = value
        override fun write(value: SourceRepositories) {
            this.value = value
        }
    }

    private val assistants = MemoryAssistantStore()
    private val language = MemoryLanguageStore()
    private val repositories = MemoryRepositoryStore()
    private val page = AssistantSettingsConfigurable(assistants, language, repositories)
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

    @Test
    fun `adding a git directory lists it in download only and apply persists it`(@TempDir tempDir: Path) {
        val repo = gitDirectory(tempDir, "repo")
        addPath(repo)
        val canonical = repo.toRealPath().toString()
        assertEquals("", panel.repositoryPathField().text)
        assertEquals("", panel.repositoryError().text)
        assertTrue(page.isModified)
        assertEquals(canonical, rowPath(panel.repositoryRows().single()))
        assertEquals("Download only", renderedMode(panel.modeCombo(canonical)))
        page.apply()
        assertEquals(canonical, repositories.value.entries().single().path)
        assertEquals(RepositoryMode.DOWNLOAD_ONLY, repositories.value.entries().single().mode)
        assertFalse(page.isModified)
    }

    @Test
    fun `a file is refused on the page and is not persisted`(@TempDir tempDir: Path) {
        val file = Files.writeString(tempDir.resolve("notes.txt"), "x")
        panel.repositoryPathField().text = "  $file  "
        panel.addButton().doClick()
        assertEquals("Not a directory: $file", panel.repositoryError().text)
        assertTrue(panel.repositoryRows().isEmpty())
        page.apply()
        assertTrue(repositories.value.entries().isEmpty())
    }

    @Test
    fun `a directory without git is refused on the page`(@TempDir tempDir: Path) {
        val directory = Files.createDirectories(tempDir.resolve("plain"))
        panel.repositoryPathField().text = directory.toString()
        panel.addButton().doClick()
        assertEquals("This directory does not contain .git: $directory", panel.repositoryError().text)
    }

    @Test
    fun `an empty path is refused on the page`() {
        panel.repositoryPathField().text = ""
        panel.addButton().doClick()
        assertEquals("Enter a directory that contains .git.", panel.repositoryError().text)
        assertFalse(page.isModified)
        panel.repositoryPathField().text = "   "
        panel.addButton().doClick()
        assertEquals("Enter a directory that contains .git.", panel.repositoryError().text)
        assertFalse(page.isModified)
    }

    @Test
    fun `mode switches to synchronisation and back on apply`(@TempDir tempDir: Path) {
        val repo = gitDirectory(tempDir, "repo")
        addPath(repo)
        val path = repo.toRealPath().toString()
        panel.modeCombo(path).selectedItem = RepositoryMode.SYNCHRONISATION
        page.apply()
        assertEquals(RepositoryMode.SYNCHRONISATION, repositories.value.entries().single().mode)
        panel.modeCombo(path).selectedItem = RepositoryMode.DOWNLOAD_ONLY
        page.apply()
        assertEquals(RepositoryMode.DOWNLOAD_ONLY, repositories.value.entries().single().mode)
    }

    @Test
    fun `a duplicate path is refused and the list stays`(@TempDir tempDir: Path) {
        val repo = gitDirectory(tempDir, "repo")
        addPath(repo)
        page.apply()
        val equivalent = repo.resolve("..").resolve(repo.fileName).toString()
        panel.repositoryPathField().text = equivalent
        panel.addButton().doClick()
        assertEquals("This repository is already listed: $equivalent", panel.repositoryError().text)
        assertEquals(1, panel.repositoryRows().size)
    }

    @Test
    fun `two repositories are listed in insertion order`(@TempDir tempDir: Path) {
        val first = gitDirectory(tempDir, "one")
        val second = gitDirectory(tempDir, "two")
        addPath(first)
        addPath(second)
        assertEquals(
            listOf(first.toRealPath().toString(), second.toRealPath().toString()),
            panel.repositoryRows().map { rowPath(it) },
        )
    }

    @Test
    fun `removing a repository leaves the clone and installed files`(@TempDir tempDir: Path) {
        val clone = gitDirectory(tempDir, "clone")
        val readme = clone.resolve("README.md")
        Files.writeString(readme, "clone")
        val marker = tempDir.resolve("project").resolve(".cursor/skills/specification-fr/SKILL.md")
        Files.createDirectories(marker.parent)
        Files.writeString(marker, "installed")
        addPath(clone)
        panel.removeButton(clone.toRealPath().toString()).doClick()
        page.apply()
        assertTrue(repositories.value.entries().isEmpty())
        assertEquals("clone", Files.readString(readme))
        assertEquals("installed", Files.readString(marker))
    }

    @Test
    fun `reset drops an addition that was not applied`(@TempDir tempDir: Path) {
        addPath(gitDirectory(tempDir, "repo"))
        assertTrue(page.isModified)
        page.reset()
        assertTrue(panel.repositoryRows().isEmpty())
        assertEquals("", panel.repositoryError().text)
        assertTrue(repositories.value.entries().isEmpty())
    }

    @Test
    fun `changing a repository mode clears a refused path`(@TempDir tempDir: Path) {
        val repo = gitDirectory(tempDir, "repo")
        addPath(repo)
        val file = Files.writeString(tempDir.resolve("notes.txt"), "x")
        panel.repositoryPathField().text = file.toString()
        panel.addButton().doClick()
        assertEquals("Not a directory: $file", panel.repositoryError().text)
        panel.modeCombo(repo.toRealPath().toString()).selectedItem = RepositoryMode.SYNCHRONISATION
        assertEquals("", panel.repositoryError().text)
    }

    @Test
    fun `french labels the repository section`(@TempDir tempDir: Path) {
        panel.languageCombo().selectedItem = UiLanguage.FRENCH
        assertEquals("Dépôts", panel.repositoryHeading().text)
        assertEquals("Ajouter", panel.addButton().text)
        val repo = gitDirectory(tempDir, "repo")
        addPath(repo)
        assertEquals("Téléchargement seul", renderedMode(panel.modeCombo(repo.toRealPath().toString())))
        val file = Files.writeString(tempDir.resolve("notes.txt"), "x")
        panel.repositoryPathField().text = file.toString()
        panel.addButton().doClick()
        assertEquals("Pas un répertoire : $file", panel.repositoryError().text)
    }

    private fun gitDirectory(root: Path, name: String): Path {
        val repo = Files.createDirectories(root.resolve(name))
        Files.createDirectories(repo.resolve(".git"))
        return repo
    }

    private fun addPath(path: Path) {
        panel.repositoryPathField().text = path.toString()
        panel.addButton().doClick()
    }

    private fun rowPath(row: Component): String =
        (row as JPanel).components.filterIsInstance<JLabel>().single().text

    @Suppress("UNCHECKED_CAST")
    private fun renderedMode(combo: JComboBox<RepositoryMode>): String {
        val renderer = combo.renderer as ListCellRenderer<Any>
        val component = renderer.getListCellRendererComponent(
            JList<Any>(),
            combo.selectedItem,
            -1,
            false,
            false,
        )
        return (component as JLabel).text
    }
}
