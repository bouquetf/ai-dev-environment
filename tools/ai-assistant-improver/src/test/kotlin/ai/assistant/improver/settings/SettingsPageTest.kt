package ai.assistant.improver.settings

import ai.assistant.improver.assistant.AssistantCatalog
import ai.assistant.improver.assistant.AssistantId
import ai.assistant.improver.assistant.EnabledAssistants
import ai.assistant.improver.component.ComponentDiscovery
import ai.assistant.improver.component.ComponentKind
import ai.assistant.improver.component.ComponentSelection
import ai.assistant.improver.i18n.Labels
import ai.assistant.improver.install.ContentDigest
import ai.assistant.improver.install.InstallationSnapshot
import ai.assistant.improver.install.OverwriteConfirmation
import ai.assistant.improver.install.OverwritePrompt
import ai.assistant.improver.i18n.LanguagePreference
import ai.assistant.improver.i18n.UiLanguage
import ai.assistant.improver.publish.GitCommand
import ai.assistant.improver.publish.GitOutput
import ai.assistant.improver.publish.PublishConfirmation
import ai.assistant.improver.publish.PublishFile
import ai.assistant.improver.repository.CacheDirectories
import ai.assistant.improver.repository.RepositoryMode
import ai.assistant.improver.repository.SourceRepositories
import ai.assistant.improver.repository.SourceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.Component
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.BoxLayout
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JFileChooser
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
    fun `language label and combo share a compact left aligned row`() {
        val heading = panel.languageHeading()
        val combo = panel.languageCombo()
        val languageRow = heading.parent as JPanel
        assertSame(languageRow, combo.parent)
        assertEquals("language.row", languageRow.name)
        assertNotSame(panel, languageRow)
        assertSame(panel, languageRow.parent)
        assertTrue(languageRow.layout is BoxLayout)
        assertEquals(Component.LEFT_ALIGNMENT, languageRow.alignmentX)

        assertEquals(UiLanguage.ENGLISH, combo.prototypeDisplayValue)
        val englishWidth = languageProbeWidth(combo, UiLanguage.ENGLISH)
        assertEquals(englishWidth, combo.preferredSize.width)
        assertEquals(combo.preferredSize.width, combo.maximumSize.width)
        assertEquals(heading.preferredSize.height, combo.preferredSize.height)
        val languageWord = fixedTextProbeWidth(combo, "Language")
        if (languageWord != englishWidth) {
            assertNotEquals(languageWord, combo.preferredSize.width)
        }

        layoutWide(languageRow)
        assertEquals(heading.y, combo.y)
        assertEquals(0, heading.x)
        assertEquals(heading.x + heading.width, combo.x)
        assertEquals(combo.preferredSize.width, combo.width)
        assertTrue(combo.x + combo.width < languageRow.width)

        val widthBeforeFrench = combo.preferredSize.width
        combo.selectedItem = UiLanguage.FRENCH
        assertEquals("Langue", heading.text)
        assertSame(languageRow, heading.parent)
        assertSame(languageRow, combo.parent)
        assertEquals(UiLanguage.FRENCH, combo.prototypeDisplayValue)
        val frenchWidth = languageProbeWidth(combo, UiLanguage.FRENCH)
        assertEquals(frenchWidth, combo.preferredSize.width)
        assertEquals(combo.preferredSize.width, combo.maximumSize.width)
        assertEquals(heading.preferredSize.height, combo.preferredSize.height)
        if (englishWidth != frenchWidth) {
            assertNotEquals(widthBeforeFrench, combo.preferredSize.width)
        }
        val langueWord = fixedTextProbeWidth(combo, "Langue")
        if (langueWord != frenchWidth) {
            assertNotEquals(langueWord, combo.preferredSize.width)
        }

        layoutWide(languageRow)
        assertEquals(combo.preferredSize.width, combo.width)
    }

    @Test
    fun `repository mode lists stay unconstrained`(@TempDir tempDir: Path) {
        val repo = gitDirectory(tempDir, "repo")
        addPath(repo)
        val path = repo.toRealPath().toString()
        val modeCombo = panel.modeCombo(path)
        assertFalse(modeCombo.isMaximumSizeSet)
        val modeParent = modeCombo.parent
        assertNotEquals("language.row", modeParent.name)
        assertTrue(modeParent.components.any { it === modeCombo })
        assertTrue(modeParent.components.filterIsInstance<JLabel>().isNotEmpty())
        assertSame(panel.removeButton(path), modeParent.components.first { it is JButton && it.name == "repository.remove" })
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
    fun `browse chooses a directory`() {
        assertEquals(JFileChooser.DIRECTORIES_ONLY, RepositorySettingsSection.directoryChooser().fileSelectionMode)
        assertEquals("repository.browse", panel.browseButton().name)
    }

    @Test
    fun `an https url is listed in download only and apply persists it`(@TempDir tempDir: Path) {
        val cache = tempDir.resolve("cache")
        val git = RecordingCloneGit()
        val configurable = AssistantSettingsConfigurable(
            assistants,
            language,
            repositories,
            selection,
            snapshot,
            git = git,
            cacheRoot = { cache },
        )
        val settings = configurable.createComponent() as SettingsPanel
        val retained = "https://Plugins.Test/Repo.git"
        settings.repositoryPathField().text = "  HTTPS://Plugins.Test/Repo.git  "
        settings.addButton().doClick()
        assertEquals("", settings.repositoryPathField().text)
        assertEquals("", settings.repositoryError().text)
        assertEquals(retained, rowPath(settings.repositoryRows().single()))
        assertEquals("Download only", renderedMode(settings.modeCombo(retained)))
        configurable.apply()
        assertEquals(retained, repositories.value.entries().single().path)
        assertEquals(RepositoryMode.DOWNLOAD_ONLY, repositories.value.entries().single().mode)
        val destination = CacheDirectories.directory(cache, retained)
        assertEquals(listOf("clone", "--", retained, destination.toString()), git.calls.single().second)
        assertFalse(repositories.value.entries().single().path.contains(destination.fileName.toString()))
    }

    @Test
    fun `removing a url deletes the cache clone and leaves installed files`(@TempDir tempDir: Path) {
        val cache = tempDir.resolve("cache")
        val git = RecordingCloneGit()
        val configurable = AssistantSettingsConfigurable(
            assistants,
            language,
            repositories,
            selection,
            snapshot,
            git = git,
            cacheRoot = { cache },
        )
        val settings = configurable.createComponent() as SettingsPanel
        val retained = "https://Plugins.Test/Repo.git"
        settings.repositoryPathField().text = retained
        settings.addButton().doClick()
        val destination = CacheDirectories.directory(cache, retained)
        Files.createDirectories(destination)
        Files.writeString(destination.resolve("README.md"), "clone")
        val marker = tempDir.resolve("project").resolve(".cursor/skills/specification-fr/SKILL.md")
        Files.createDirectories(marker.parent)
        Files.writeString(marker, "installed")
        settings.removeButton(retained).doClick()
        configurable.apply()
        assertTrue(repositories.value.entries().isEmpty())
        assertFalse(Files.exists(destination))
        assertEquals("installed", Files.readString(marker))
    }

    @Test
    fun `discovery of a url reads the cache and selection stores the url`(@TempDir tempDir: Path) {
        val cache = tempDir.resolve("cache")
        val url = "https://plugins.test/repo.git"
        val clone = CacheDirectories.directory(cache, url)
        write(clone, "ia/skills/demo/SKILL.md", "skill")
        val local = gitDirectory(tempDir, "local")
        write(local, "ia/skills/local-skill/SKILL.md", "local")
        val localPath = local.toRealPath().toString()
        val seen = mutableListOf<Path>()
        val model = SettingsViewModel(
            EnabledAssistants(),
            LanguagePreference(),
            SourceRepositories.restore(
                listOf(
                    SourceRepository(url, RepositoryMode.DOWNLOAD_ONLY),
                    SourceRepository(localPath, RepositoryMode.DOWNLOAD_ONLY),
                ),
            ),
            discover = { path ->
                seen.add(path)
                ComponentDiscovery.discover(path)
            },
            cacheRoot = { cache },
        )
        val rows = model.componentRows
        assertEquals(clone.normalize(), seen[0].normalize())
        assertEquals(Path.of(localPath).normalize(), seen[1].normalize())
        assertTrue(seen.none { it.normalize() == cache.normalize() })
        val remoteRow = rows.single { it.repositoryPath == url }
        assertTrue(remoteRow.label.contains("ia/skills/demo"))
        model.setComponentSelected(url, remoteRow.id, false)
        val xml = model.selection().toXml()
        assertEquals(url, model.selection().entries().single().repositoryPath)
        assertTrue(xml.contains(url))
        assertFalse(xml.contains(clone.fileName.toString()))
    }

    @Test
    fun `a failed fetch is reported and the current files are imported`(@TempDir tempDir: Path) {
        val cache = tempDir.resolve("cache")
        val project = tempDir.resolve("project")
        val url = "https://plugins.test/repo.git"
        val clone = CacheDirectories.directory(cache, url)
        write(clone, "ia/skills/specification-fr/SKILL.md", "specification")
        repositories.value = SourceRepositories.restore(listOf(SourceRepository(url, RepositoryMode.DOWNLOAD_ONLY)))
        val git = object : GitCommand {
            override fun run(repository: Path, args: List<String>): GitOutput =
                if (args == listOf("fetch", "origin")) GitOutput(1, "failed") else error("unexpected git: $args")
        }
        val configurable = AssistantSettingsConfigurable(
            assistants,
            language,
            repositories,
            selection,
            snapshot,
            projectRoot = { project },
            git = git,
            cacheRoot = { cache },
        )
        val settings = configurable.createComponent() as SettingsPanel
        settings.assistantChecks().first { it.name == "cursor" }.doClick()
        settings.importButton().doClick()
        assertEquals(
            "The update failed for $url. Import continues with the current files.",
            settings.importNotices().text,
        )
        assertEquals("specification", Files.readString(project.resolve(".cursor/skills/specification-fr/SKILL.md")))
        assertTrue(Files.isDirectory(clone))
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
        assertEquals(
            "Enter a directory that contains .git, or a git, http, or https URL.",
            panel.repositoryError().text,
        )
        assertFalse(page.isModified)
        panel.repositoryPathField().text = "   "
        panel.addButton().doClick()
        assertEquals(
            "Enter a directory that contains .git, or a git, http, or https URL.",
            panel.repositoryError().text,
        )
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

    @Test
    fun `publish button uses the injected confirmation and saves the snapshot`(@TempDir tempDir: Path) {
        val project = Files.createDirectories(tempDir.resolve("project"))
        val seen = mutableListOf<PublishFile>()
        val git = PageGit()
        val (publishPage, publishPanel, repo) = importedSkill(tempDir, project, git, PublishConfirmation {
            seen += it
            "Publish skill"
        })
        val path = repo.toRealPath().toString()
        publishPanel.modeCombo(path).selectedItem = RepositoryMode.SYNCHRONISATION
        publishPage.apply()
        val installed = resolve(project, ".cursor/skills/specification-fr/SKILL.md")
        Files.writeString(installed, "local-edit")
        val assistantsBefore = assistants.value
        val repositoriesBefore = repositories.value
        val selectionBefore = selection.value

        publishPanel.publishButton().doClick()

        assertEquals("ia/skills/specification-fr/SKILL.md", seen.single().sourcePath)
        assertEquals(".cursor/skills/specification-fr/SKILL.md", seen.single().destinationPath)
        val committed = git.calls.first { it.first() == "commit" }
        assertTrue(committed.containsAll(listOf("commit", "-m", "Publish skill", "ia/skills/specification-fr/SKILL.md")))
        assertEquals(ContentDigest.sha256(Files.readAllBytes(installed)), snapshot.value.files().single().sha256)
        assertFalse(publishPage.isModified)
        assertEquals(assistantsBefore, assistants.value)
        assertEquals(repositoriesBefore, repositories.value)
        assertEquals(selectionBefore, selection.value)
        assertEquals("Published $path.", publishPanel.publishNotices().text)
    }

    @Test
    fun `cancel publishes nothing`(@TempDir tempDir: Path) {
        val project = Files.createDirectories(tempDir.resolve("project"))
        val git = PageGit()
        val (_, publishPanel, repo) = installedForPublish(tempDir, project, git) { null }
        val skill = resolve(repo, "ia/skills/specification-fr/SKILL.md")
        val digestBefore = snapshot.value.files().single().sha256

        publishPanel.publishButton().doClick()

        assertTrue(git.calls.isEmpty())
        assertEquals("specification", Files.readString(skill))
        assertEquals(digestBefore, snapshot.value.files().single().sha256)
        assertEquals("Publication cancelled.", publishPanel.publishNotices().text)
    }

    @Test
    fun `download only offers nothing to publish`(@TempDir tempDir: Path) {
        val project = Files.createDirectories(tempDir.resolve("project"))
        var confirmed = false
        val (_, publishPanel) = importedSkill(tempDir, project, PageGit(), PublishConfirmation {
            confirmed = true
            null
        })
        Files.writeString(resolve(project, ".cursor/skills/specification-fr/SKILL.md"), "local-edit")

        publishPanel.publishButton().doClick()

        assertFalse(confirmed)
        assertEquals("No local changes to publish.", publishPanel.publishNotices().text)
    }

    @Test
    fun `divergence is shown from the page`(@TempDir tempDir: Path) {
        val project = Files.createDirectories(tempDir.resolve("project"))
        val git = PageGit()
        val (_, publishPanel, repo) = installedForPublish(tempDir, project, git) { "Publish skill" }
        val skill = resolve(repo, "ia/skills/specification-fr/SKILL.md")
        Files.writeString(skill, "upstream")
        Files.writeString(resolve(project, ".cursor/skills/specification-fr/SKILL.md"), "local-edit")

        publishPanel.publishButton().doClick()

        val path = repo.toRealPath().toString()
        assertEquals(
            "The source repository and the local files both changed. Nothing was published for $path.",
            publishPanel.publishNotices().text,
        )
        assertEquals("upstream", Files.readString(skill))
    }

    @Test
    fun `french push failure uses the french bundle`(@TempDir tempDir: Path) {
        val project = Files.createDirectories(tempDir.resolve("project"))
        val git = PageGit()
        val (_, publishPanel, repo) = installedForPublish(tempDir, project, git) { "Publish skill" }
        val digestBefore = snapshot.value.files().single().sha256
        publishPanel.languageCombo().selectedItem = UiLanguage.FRENCH
        git.pushCode = 1

        publishPanel.publishButton().doClick()

        val path = repo.toRealPath().toString()
        assertEquals("Le commit reste en local, sans push, pour $path.", publishPanel.publishNotices().text)
        assertEquals(digestBefore, snapshot.value.files().single().sha256)
    }

    @Test
    fun `publish label follows the language`() {
        assertEquals("Publish", panel.publishButton().text)
        panel.languageCombo().selectedItem = UiLanguage.FRENCH
        assertEquals("Publier", panel.publishButton().text)
    }

    private fun installedForPublish(
        tempDir: Path,
        project: Path,
        git: PageGit,
        confirm: (List<PublishFile>) -> String?,
    ): Triple<AssistantSettingsConfigurable, SettingsPanel, Path> {
        val opened = importedSkill(tempDir, project, git, PublishConfirmation(confirm))
        val path = opened.third.toRealPath().toString()
        opened.second.modeCombo(path).selectedItem = RepositoryMode.SYNCHRONISATION
        opened.first.apply()
        Files.writeString(resolve(project, ".cursor/skills/specification-fr/SKILL.md"), "local-edit")
        return opened
    }

    private fun importedSkill(
        tempDir: Path,
        project: Path,
        git: GitCommand,
        confirm: PublishConfirmation,
    ): Triple<AssistantSettingsConfigurable, SettingsPanel, Path> {
        val repo = gitDirectory(tempDir, "repo")
        write(repo, "ia/skills/specification-fr/SKILL.md", "specification")
        val configurable = AssistantSettingsConfigurable(
            assistants,
            language,
            repositories,
            selection,
            snapshot,
            projectRoot = { project },
            confirmPublish = confirm,
            git = git,
        )
        val settings = configurable.createComponent() as SettingsPanel
        settings.assistantChecks().first { it.name == "cursor" }.doClick()
        addPath(settings, repo)
        configurable.apply()
        settings.importButton().doClick()
        return Triple(configurable, settings, repo)
    }

    private class RecordingCloneGit : GitCommand {
        val calls = mutableListOf<Pair<Path, List<String>>>()

        override fun run(repository: Path, args: List<String>): GitOutput {
            calls += repository to args
            return GitOutput(0, "")
        }
    }

    private class PageGit : GitCommand {
        val calls = mutableListOf<List<String>>()
        var pushCode: Int = 0

        override fun run(repository: Path, args: List<String>): GitOutput {
            calls += args
            return when (args.first()) {
                "remote" -> GitOutput(0, "url\n")
                "rev-parse" -> GitOutput(0, "main\n")
                "status" -> GitOutput(0, " M ia/skills/specification-fr/SKILL.md\n")
                "add", "commit" -> GitOutput(0, "")
                "push" -> GitOutput(pushCode, if (pushCode == 0) "" else "rejected\n")
                else -> error("unexpected git: $args")
            }
        }
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

    private fun layoutWide(row: JPanel) {
        val preferred = row.preferredSize
        row.setSize(preferred.width + 240, preferred.height)
        row.doLayout()
    }

    private fun languageProbeWidth(combo: JComboBox<UiLanguage>, language: UiLanguage): Int {
        val probe = JComboBox(arrayOf(language))
        probe.font = combo.font
        probe.renderer = combo.renderer
        probe.prototypeDisplayValue = language
        return probe.preferredSize.width
    }

    private fun fixedTextProbeWidth(combo: JComboBox<*>, text: String): Int {
        val probe = JComboBox(arrayOf(text))
        probe.font = combo.font
        probe.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean,
            ): Component {
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus)
            }
        }
        probe.prototypeDisplayValue = text
        return probe.preferredSize.width
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
