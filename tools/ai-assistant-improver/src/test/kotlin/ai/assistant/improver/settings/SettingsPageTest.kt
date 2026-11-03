package ai.assistant.improver.settings

import ai.assistant.improver.assistant.AssistantCatalog
import ai.assistant.improver.assistant.AssistantId
import ai.assistant.improver.assistant.EnabledAssistants
import ai.assistant.improver.component.ComponentKind
import ai.assistant.improver.component.ComponentSelection
import ai.assistant.improver.i18n.Labels
import ai.assistant.improver.install.InstallationSnapshot
import ai.assistant.improver.install.OverwriteConfirmation
import ai.assistant.improver.install.OverwritePrompt
import ai.assistant.improver.i18n.LanguagePreference
import ai.assistant.improver.i18n.UiLanguage
import ai.assistant.improver.repository.RepositoryMode
import ai.assistant.improver.repository.SourceRepositories
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
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

    private class MemoryComponentSelectionStore(
        var value: ComponentSelection = ComponentSelection(),
    ) : ComponentSelectionStore {
        override fun read(): ComponentSelection = value
        override fun write(value: ComponentSelection) {
            this.value = value
        }
    }

    private class MemoryInstallationSnapshotStore(
        var value: InstallationSnapshot = InstallationSnapshot(),
    ) : InstallationSnapshotStore {
        override fun read(): InstallationSnapshot = value
        override fun write(value: InstallationSnapshot) {
            this.value = value
        }
    }

    private val assistants = MemoryAssistantStore()
    private val language = MemoryLanguageStore()
    private val repositories = MemoryRepositoryStore()
    private val selection = MemoryComponentSelectionStore()
    private val snapshot = MemoryInstallationSnapshotStore()
    private val page = AssistantSettingsConfigurable(assistants, language, repositories, selection, snapshot)
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

    @Test
    fun `a repository without components shows no checkbox and no error`(@TempDir tempDir: Path) {
        val repo = gitDirectory(tempDir, "repo")
        Files.writeString(repo.resolve("README.md"), "notes")
        addPath(repo)
        assertTrue(panel.componentChecks(repo.toRealPath().toString()).isEmpty())
        assertEquals("", panel.repositoryError().text)
    }

    @Test
    fun `discovered components start selected and apply keeps the selection store empty`(@TempDir tempDir: Path) {
        val repo = gitDirectory(tempDir, "repo")
        write(repo, "ia/skills/review/SKILL.md")
        write(repo, ".cursor/agents/reviewer.md")
        addPath(repo)
        val path = repo.toRealPath().toString()
        val checks = panel.componentChecks(path)
        assertEquals(listOf("Skill ia/skills/review", "Agent .cursor/agents/reviewer.md"), checks.map { it.text })
        assertTrue(checks.all { it.isSelected })
        page.apply()
        assertEquals(ComponentSelection(), selection.value)
        assertFalse(page.isModified)
    }

    @Test
    fun `unchecking a skill is persisted and leaves the file in place`(@TempDir tempDir: Path) {
        val repo = gitDirectory(tempDir, "repo")
        val skill = write(repo, "ia/skills/review/SKILL.md", "kept")
        addPath(repo)
        val path = repo.toRealPath().toString()
        panel.componentChecks(path).single().doClick()
        assertTrue(page.isModified)
        assertEquals("kept", Files.readString(skill))
        page.apply()
        val entry = selection.value.entries().single()
        assertEquals(path, entry.repositoryPath)
        assertEquals(ComponentKind.SKILL, entry.id.kind)
        assertEquals("ia/skills/review", entry.id.relativePath)
        assertFalse(page.isModified)
    }

    @Test
    fun `reset restores a checkbox that was not applied`(@TempDir tempDir: Path) {
        val repo = gitDirectory(tempDir, "repo")
        write(repo, "ia/skills/review/SKILL.md")
        addPath(repo)
        page.apply()
        val path = repo.toRealPath().toString()
        panel.componentChecks(path).single().doClick()
        page.reset()
        assertTrue(panel.componentChecks(path).single().isSelected)
        assertEquals(ComponentSelection(), selection.value)
    }

    @Test
    fun `a file added on disk is selected and the stored uncheck remains`(@TempDir tempDir: Path) {
        val repo = gitDirectory(tempDir, "repo")
        write(repo, "ia/skills/review/SKILL.md")
        addPath(repo)
        val path = repo.toRealPath().toString()
        panel.componentChecks(path).single().doClick()
        page.apply()
        write(repo, ".cursor/skills/other/SKILL.md")
        panel.syncFromModel()
        val checks = panel.componentChecks(path)
        assertEquals(listOf("Skill .cursor/skills/other", "Skill ia/skills/review"), checks.map { it.text })
        assertTrue(checks[0].isSelected)
        assertFalse(checks[1].isSelected)
        assertFalse(page.isModified)
        assertEquals("ia/skills/review", selection.value.entries().single().id.relativePath)
    }

    @Test
    fun `french labels a rule`(@TempDir tempDir: Path) {
        val repo = gitDirectory(tempDir, "repo")
        write(repo, "CLAUDE.md", "guide")
        addPath(repo)
        panel.languageCombo().selectedItem = UiLanguage.FRENCH
        assertEquals("Règle CLAUDE.md", panel.componentChecks(repo.toRealPath().toString()).single().text)
    }

    @Test
    fun `import copies the selected skill and saves the snapshot without apply`(@TempDir tempDir: Path) {
        val project = Files.createDirectories(tempDir.resolve("project"))
        val repo = gitDirectory(tempDir, "repo")
        write(repo, "ia/skills/specification-fr/SKILL.md", "specification")
        val (importPage, importPanel) = openImport(project)
        importPanel.assistantChecks().first { it.name == "cursor" }.doClick()
        addPath(importPanel, repo)
        importPage.apply()
        assertFalse(importPage.isModified)
        val assistantsBefore = assistants.value
        val repositoriesBefore = repositories.value

        importPanel.importButton().doClick()

        assertEquals("specification", read(project, ".cursor/skills/specification-fr/SKILL.md"))
        assertEquals(
            ".cursor/skills/specification-fr/SKILL.md",
            snapshot.value.files().single().destinationPath,
        )
        assertFalse(importPage.isModified)
        assertEquals(ComponentSelection(), selection.value)
        assertEquals(assistantsBefore, assistants.value)
        assertEquals(repositoriesBefore, repositories.value)
    }

    @Test
    fun `an unchecked skill is not imported and the uncheck is not persisted`(@TempDir tempDir: Path) {
        val project = Files.createDirectories(tempDir.resolve("project"))
        val repo = gitDirectory(tempDir, "repo")
        write(repo, "ia/skills/specification-fr/SKILL.md", "specification")
        val (_, importPanel) = openImport(project)
        importPanel.assistantChecks().first { it.name == "cursor" }.doClick()
        addPath(importPanel, repo)
        importPanel.componentChecks(repo.toRealPath().toString()).single().doClick()

        importPanel.importButton().doClick()

        assertEquals(emptyList<Path>(), Files.list(project).use { it.toList() })
        assertEquals(ComponentSelection(), selection.value)
        assertEquals(InstallationSnapshot(), snapshot.value)
    }

    @Test
    fun `download only confirmation is supplied by the test`(@TempDir tempDir: Path) {
        val project = Files.createDirectories(tempDir.resolve("project"))
        val repo = gitDirectory(tempDir, "repo")
        val source = write(repo, "ia/skills/specification-fr/SKILL.md", "specification")
        val prompts = mutableListOf<OverwritePrompt>()
        var allow = false
        val (_, importPanel) = openImport(project, prompts) { allow }
        importPanel.assistantChecks().first { it.name == "cursor" }.doClick()
        addPath(importPanel, repo)

        importPanel.importButton().doClick()

        assertTrue(prompts.isEmpty())
        val installed = resolve(project, ".cursor/skills/specification-fr/SKILL.md")
        Files.writeString(installed, "local-edit")
        val digestBefore = snapshot.value.files().single().sha256
        importPanel.importButton().doClick()
        assertEquals(".cursor/skills/specification-fr", prompts.single().destinationPath)
        assertEquals("local-edit", Files.readString(installed))
        assertEquals(digestBefore, snapshot.value.files().single().sha256)
        Files.writeString(source, "revised")
        allow = true
        importPanel.importButton().doClick()
        assertEquals("revised", Files.readString(installed))
        assertNotEquals(digestBefore, snapshot.value.files().single().sha256)
        assertEquals("", importPanel.importNotices().text)
    }

    @Test
    fun `a missing target is reported and the other copy still happens`(@TempDir tempDir: Path) {
        val project = Files.createDirectories(tempDir.resolve("project"))
        val repo = gitDirectory(tempDir, "repo")
        write(repo, "ia/skills/specification-fr/SKILL.md", "specification")
        write(repo, "CLAUDE.md", "guide")
        val (_, importPanel) = openImport(project)
        importPanel.assistantChecks().first { it.name == "cursor" }.doClick()
        addPath(importPanel, repo)

        importPanel.importButton().doClick()

        assertEquals("specification", read(project, ".cursor/skills/specification-fr/SKILL.md"))
        assertFalse(exists(project, "CLAUDE.md"))
        assertEquals("Cursor has no folder for CLAUDE.md.", importPanel.importNotices().text)
    }

    @Test
    fun `french notice uses the french bundle`(@TempDir tempDir: Path) {
        val project = Files.createDirectories(tempDir.resolve("project"))
        val repo = gitDirectory(tempDir, "repo")
        write(repo, "ia/skills/specification-fr/SKILL.md", "specification")
        write(repo, "CLAUDE.md", "guide")
        val (_, importPanel) = openImport(project)
        importPanel.assistantChecks().first { it.name == "cursor" }.doClick()
        addPath(importPanel, repo)
        importPanel.languageCombo().selectedItem = UiLanguage.FRENCH

        importPanel.importButton().doClick()

        assertEquals("Cursor : aucun dossier pour CLAUDE.md.", importPanel.importNotices().text)
    }

    @Test
    fun `synchronisation keeps the local file and shows the notice`(@TempDir tempDir: Path) {
        val project = Files.createDirectories(tempDir.resolve("project"))
        val repo = gitDirectory(tempDir, "repo")
        write(repo, "ia/skills/specification-fr/SKILL.md", "specification")
        val prompts = mutableListOf<OverwritePrompt>()
        val (importPage, importPanel) = openImport(project, prompts) { false }
        importPanel.assistantChecks().first { it.name == "cursor" }.doClick()
        addPath(importPanel, repo)
        importPage.apply()
        importPanel.importButton().doClick()
        val installed = resolve(project, ".cursor/skills/specification-fr/SKILL.md")
        Files.writeString(installed, "local-edit")
        val digestBefore = snapshot.value.files().single().sha256
        val path = repo.toRealPath().toString()
        importPanel.modeCombo(path).selectedItem = RepositoryMode.SYNCHRONISATION
        importPage.apply()

        importPanel.importButton().doClick()

        assertTrue(prompts.isEmpty())
        assertEquals("local-edit", Files.readString(installed))
        assertEquals("Local changes kept for .cursor/skills/specification-fr.", importPanel.importNotices().text)
        assertEquals(digestBefore, snapshot.value.files().single().sha256)
    }

    private fun openImport(
        project: Path,
        prompts: MutableList<OverwritePrompt> = mutableListOf(),
        allow: () -> Boolean = { false },
    ): Pair<AssistantSettingsConfigurable, SettingsPanel> {
        val configurable = AssistantSettingsConfigurable(
            assistants,
            language,
            repositories,
            selection,
            snapshot,
            confirmOverwrite = OverwriteConfirmation { prompt ->
                prompts += prompt
                allow()
            },
            projectRoot = { project },
        )
        return configurable to configurable.createComponent() as SettingsPanel
    }

    private fun gitDirectory(root: Path, name: String): Path {
        val repo = Files.createDirectories(root.resolve(name))
        Files.createDirectories(repo.resolve(".git"))
        return repo
    }

    private fun addPath(path: Path) {
        addPath(panel, path)
    }

    private fun addPath(target: SettingsPanel, path: Path) {
        target.repositoryPathField().text = path.toString()
        target.addButton().doClick()
    }

    private fun write(root: Path, relative: String, content: String = "x"): Path {
        val path = resolve(root, relative)
        Files.createDirectories(path.parent)
        Files.writeString(path, content)
        return path
    }

    private fun read(root: Path, relative: String): String = Files.readString(resolve(root, relative))

    private fun exists(root: Path, relative: String): Boolean = Files.exists(resolve(root, relative))

    private fun resolve(root: Path, relative: String): Path =
        relative.split('/').fold(root) { parent, name -> parent.resolve(name) }

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
