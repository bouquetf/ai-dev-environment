package ai.assistant.improver.install

import ai.assistant.improver.assistant.AssistantCatalog
import ai.assistant.improver.assistant.AssistantId
import ai.assistant.improver.assistant.EnabledAssistants
import ai.assistant.improver.component.ComponentDiscovery
import ai.assistant.improver.component.ComponentId
import ai.assistant.improver.component.ComponentKind
import ai.assistant.improver.component.ComponentSelection
import ai.assistant.improver.repository.RepositoryMode
import ai.assistant.improver.repository.SourceRepositories
import ai.assistant.improver.repository.SourceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ComponentImportTest {
    @Test
    fun `sha256 of abc is the known vector`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            ContentDigest.sha256("abc".toByteArray(Charsets.US_ASCII)),
        )
    }

    @Test
    fun `snapshot xml round trips and rejects a duplicate destination`() {
        val file = InstalledFile(
            repositoryPath = "C:/repo",
            component = ComponentId(ComponentKind.SKILL, "ia/skills/review"),
            sourcePath = "ia/skills/review/SKILL.md",
            destinationPath = ".cursor/skills/review/SKILL.md",
            sha256 = ContentDigest.sha256("abc".toByteArray(Charsets.US_ASCII)),
        )
        val snapshot = InstallationSnapshot.restore(listOf(file))
        assertEquals(snapshot, InstallationSnapshot.fromXml(snapshot.toXml()))
        assertThrows<IllegalArgumentException> {
            InstallationSnapshot.restore(listOf(file, file.copy(sourcePath = "ia/skills/review/OTHER.md")))
        }
        assertThrows<IllegalArgumentException> {
            InstallationSnapshot.restore(listOf(file.copy(sha256 = "ABC")))
        }
    }

    @Test
    fun `inactive assistant receives nothing`(@TempDir source: Path, @TempDir project: Path) {
        write(source, "ia/skills/specification-fr/SKILL.md", "specification")

        val report = import(source, project, assistants(AssistantId.CURSOR))

        assertEquals("specification", read(project, ".cursor/skills/specification-fr/SKILL.md"))
        assertFalse(exists(project, ".claude"))
        assertFalse(exists(project, ".opencode"))
        assertFalse(exists(project, ".junie"))
        assertEquals(AssistantId.CURSOR, report.destinations.single().assistant)
        assertEquals(ImportAction.COPIED, report.destinations.single().action)
    }

    @Test
    fun `deselected component is skipped`(@TempDir source: Path, @TempDir project: Path) {
        write(source, "ia/skills/specification-fr/SKILL.md", "specification")
        val selection = ComponentSelection().setSelected(
            source.toString(),
            ComponentId(ComponentKind.SKILL, "ia/skills/specification-fr"),
            false,
        )

        val report = import(source, project, assistants(AssistantId.CURSOR), selection)

        assertEquals(emptyList<Path>(), Files.list(project).use { it.toList() })
        assertTrue(report.destinations.isEmpty())
        assertEquals(InstallationSnapshot(), report.snapshot)
    }

    @Test
    fun `missing target produces a notice and the other targets continue`(
        @TempDir source: Path,
        @TempDir project: Path,
    ) {
        write(source, "ia/skills/specification-fr/SKILL.md", "specification")
        write(source, "CLAUDE.md", "guide")

        val report = import(source, project, assistants(AssistantId.CURSOR, AssistantId.CLAUDE))

        assertEquals("specification", read(project, ".cursor/skills/specification-fr/SKILL.md"))
        assertEquals("specification", read(project, ".claude/skills/specification-fr/SKILL.md"))
        assertEquals("guide", read(project, "CLAUDE.md"))
        assertFalse(exists(project, ".cursor/CLAUDE.md"))
        assertFalse(exists(project, ".claude/CLAUDE.md"))
        assertEquals(
            listOf(
                AssistantId.CURSOR to ImportAction.NO_TARGET,
                AssistantId.CLAUDE to ImportAction.COPIED,
            ),
            report.destinations
                .filter { it.component.relativePath == "CLAUDE.md" }
                .map { it.assistant to it.action },
        )
    }

    @Test
    fun `absent destination is copied without confirmation`(@TempDir source: Path, @TempDir project: Path) {
        write(source, "ia/skills/specification-fr/SKILL.md", "specification")

        val report = import(source, project, assistants(AssistantId.CURSOR))

        val destination = ".cursor/skills/specification-fr/SKILL.md"
        assertEquals(ImportAction.COPIED, report.destinations.single().action)
        assertEquals(destination, report.snapshot.files().single().destinationPath)
        assertEquals(
            ContentDigest.sha256(Files.readAllBytes(resolve(project, destination))),
            report.snapshot.files().single().sha256,
        )
    }

    @Test
    fun `synchronisation copies when the destination is absent`(@TempDir source: Path, @TempDir project: Path) {
        write(source, "ia/skills/specification-fr/SKILL.md", "specification")

        val report = import(
            source,
            project,
            assistants(AssistantId.CURSOR),
            mode = RepositoryMode.SYNCHRONISATION,
        )

        assertEquals("specification", read(project, ".cursor/skills/specification-fr/SKILL.md"))
        assertEquals(ImportAction.COPIED, report.destinations.single().action)
        assertEquals(1, report.snapshot.files().size)
    }

    @Test
    fun `matching snapshot is recopied and updated`(@TempDir source: Path, @TempDir project: Path) {
        val relative = "ia/skills/specification-fr/SKILL.md"
        write(source, relative, "version-one")
        val first = import(source, project, assistants(AssistantId.CURSOR))
        write(source, relative, "version-two")

        val second = import(source, project, assistants(AssistantId.CURSOR), snapshot = first.snapshot)

        assertEquals("version-two", read(project, ".cursor/skills/specification-fr/SKILL.md"))
        assertNotEquals(first.snapshot.files().single().sha256, second.snapshot.files().single().sha256)
        assertEquals(ImportAction.COPIED, second.destinations.single().action)
    }

    @Test
    fun `synchronisation recopies when the destination matches the snapshot`(
        @TempDir source: Path,
        @TempDir project: Path,
    ) {
        val relative = "ia/skills/specification-fr/SKILL.md"
        write(source, relative, "version-one")
        val first = import(
            source,
            project,
            assistants(AssistantId.CURSOR),
            mode = RepositoryMode.SYNCHRONISATION,
        )
        write(source, relative, "version-two")

        val second = import(
            source,
            project,
            assistants(AssistantId.CURSOR),
            mode = RepositoryMode.SYNCHRONISATION,
            snapshot = first.snapshot,
        )

        assertEquals("version-two", read(project, ".cursor/skills/specification-fr/SKILL.md"))
        assertNotEquals(first.snapshot.files().single().sha256, second.snapshot.files().single().sha256)
    }

    @Test
    fun `download only refusal leaves the destination and the snapshot`(
        @TempDir source: Path,
        @TempDir project: Path,
    ) {
        write(source, "ia/skills/specification-fr/SKILL.md", "source-v1")
        val first = import(source, project, assistants(AssistantId.CURSOR))
        val destination = resolve(project, ".cursor/skills/specification-fr/SKILL.md")
        Files.writeString(destination, "local-edit")
        val prompts = mutableListOf<OverwritePrompt>()

        val second = import(
            source,
            project,
            assistants(AssistantId.CURSOR),
            snapshot = first.snapshot,
            confirm = OverwriteConfirmation { prompt ->
                prompts += prompt
                false
            },
        )

        assertEquals("local-edit", Files.readString(destination))
        assertEquals(first.snapshot, second.snapshot)
        assertEquals(ImportAction.REFUSED, second.destinations.single().action)
        assertEquals(".cursor/skills/specification-fr", prompts.single().destinationPath)
    }

    @Test
    fun `download only acceptance overwrites and updates the snapshot`(
        @TempDir source: Path,
        @TempDir project: Path,
    ) {
        val relative = "ia/skills/specification-fr/SKILL.md"
        write(source, relative, "source-v1")
        val first = import(source, project, assistants(AssistantId.CURSOR))
        val destination = resolve(project, ".cursor/skills/specification-fr/SKILL.md")
        Files.writeString(destination, "local-edit")
        write(source, relative, "source-v2")

        val second = import(
            source,
            project,
            assistants(AssistantId.CURSOR),
            snapshot = first.snapshot,
            confirm = OverwriteConfirmation { true },
        )

        assertEquals("source-v2", Files.readString(destination))
        assertEquals(
            ContentDigest.sha256("source-v2".toByteArray(Charsets.UTF_8)),
            second.snapshot.files().single().sha256,
        )
        assertNotEquals(first.snapshot.files().single().sha256, second.snapshot.files().single().sha256)
        assertEquals(ImportAction.COPIED, second.destinations.single().action)
    }

    @Test
    fun `synchronisation keeps a diverged destination`(@TempDir source: Path, @TempDir project: Path) {
        write(source, "ia/skills/specification-fr/SKILL.md", "source-v1")
        val first = import(source, project, assistants(AssistantId.CURSOR))
        val destination = resolve(project, ".cursor/skills/specification-fr/SKILL.md")
        Files.writeString(destination, "local-edit")

        val second = import(
            source,
            project,
            assistants(AssistantId.CURSOR),
            mode = RepositoryMode.SYNCHRONISATION,
            snapshot = first.snapshot,
        )

        assertEquals("local-edit", Files.readString(destination))
        assertEquals(first.snapshot, second.snapshot)
        assertEquals(ImportAction.KEPT, second.destinations.single().action)
    }

    @Test
    fun `a deleted destination is copied again without confirmation`(@TempDir source: Path, @TempDir project: Path) {
        write(source, "ia/skills/specification-fr/SKILL.md", "specification")
        val first = import(source, project, assistants(AssistantId.CURSOR))
        Files.delete(resolve(project, ".cursor/skills/specification-fr/SKILL.md"))

        val second = import(source, project, assistants(AssistantId.CURSOR), snapshot = first.snapshot)

        assertEquals("specification", read(project, ".cursor/skills/specification-fr/SKILL.md"))
        assertEquals(ImportAction.COPIED, second.destinations.single().action)
        assertEquals(
            ContentDigest.sha256("specification".toByteArray(Charsets.UTF_8)),
            second.snapshot.files().single().sha256,
        )
    }

    @Test
    fun `a nested agent keeps only its file name`(@TempDir source: Path, @TempDir project: Path) {
        write(source, ".claude/agents/team/reviewer.md", "review")

        val report = import(source, project, assistants(AssistantId.CURSOR))

        assertEquals("review", read(project, ".cursor/agents/reviewer.md"))
        assertFalse(exists(project, ".cursor/agents/team"))
        assertEquals(".cursor/agents/reviewer.md", report.snapshot.files().single().destinationPath)
    }

    @Test
    fun `a folder rule keeps its path under rules`(@TempDir source: Path, @TempDir project: Path) {
        write(source, ".cursor/rules/kotlin/style.mdc", "style")

        val report = import(source, project, assistants(AssistantId.JUNIE))

        assertEquals("style", read(project, ".junie/rules/kotlin/style.mdc"))
        assertEquals(".junie/rules/kotlin/style.mdc", report.destinations.single().destinationPath)
    }

    @Test
    fun `guidelines are installed only for junie`(@TempDir source: Path, @TempDir project: Path) {
        write(source, ".junie/guidelines.md", "guide")

        val report = import(source, project, allAssistants())

        assertEquals("guide", read(project, ".junie/guidelines.md"))
        assertFalse(exists(project, "guidelines.md"))
        assertFalse(exists(project, ".cursor/guidelines.md"))
        assertFalse(exists(project, ".claude/guidelines.md"))
        assertFalse(exists(project, ".opencode/guidelines.md"))
        assertEquals(
            listOf(
                AssistantId.CURSOR to ImportAction.NO_TARGET,
                AssistantId.CLAUDE to ImportAction.NO_TARGET,
                AssistantId.OPENCODE to ImportAction.NO_TARGET,
                AssistantId.JUNIE to ImportAction.COPIED,
            ),
            report.destinations.map { it.assistant to it.action },
        )
    }

    @Test
    fun `an extra file in a skill folder is a difference and is removed on overwrite`(
        @TempDir source: Path,
        @TempDir project: Path,
    ) {
        write(source, "ia/skills/specification-fr/SKILL.md", "specification")
        val first = import(source, project, assistants(AssistantId.CURSOR))
        write(project, ".cursor/skills/specification-fr/local.md", "local")

        val second = import(
            source,
            project,
            assistants(AssistantId.CURSOR),
            snapshot = first.snapshot,
            confirm = OverwriteConfirmation { true },
        )

        assertFalse(exists(project, ".cursor/skills/specification-fr/local.md"))
        assertEquals("specification", read(project, ".cursor/skills/specification-fr/SKILL.md"))
        assertTrue(second.snapshot.files().none { it.destinationPath.endsWith("local.md") })
    }

    @Test
    fun `a later skill of the same name replaces the snapshot destination`(
        @TempDir source: Path,
        @TempDir project: Path,
    ) {
        val first = ComponentId(ComponentKind.SKILL, "ia/skills/specification-fr")
        val second = ComponentId(ComponentKind.SKILL, ".cursor/skills/specification-fr")
        write(source, "ia/skills/specification-fr/SKILL.md", "from-ia")
        write(source, ".cursor/skills/specification-fr/SKILL.md", "from-cursor")

        val report = import(
            source,
            project,
            assistants(AssistantId.CURSOR),
            discover = { listOf(first, second) },
        )

        assertEquals("from-cursor", read(project, ".cursor/skills/specification-fr/SKILL.md"))
        val entry = report.snapshot.files().single()
        assertEquals(second, entry.component)
        assertEquals(ContentDigest.sha256("from-cursor".toByteArray(Charsets.UTF_8)), entry.sha256)
    }

    private fun import(
        source: Path,
        project: Path,
        enabled: EnabledAssistants,
        selection: ComponentSelection = ComponentSelection(),
        mode: RepositoryMode = RepositoryMode.DOWNLOAD_ONLY,
        snapshot: InstallationSnapshot = InstallationSnapshot(),
        confirm: OverwriteConfirmation = OverwriteConfirmation { prompt ->
            error("Unexpected confirmation for ${prompt.destinationPath}")
        },
        discover: (Path) -> List<ComponentId> = ComponentDiscovery::discover,
    ): ImportReport {
        val repositories = SourceRepositories.restore(listOf(SourceRepository(source.toString(), mode)))
        return ComponentImport.import(project, repositories, selection, enabled, snapshot, confirm, discover)
    }

    private fun assistants(vararg ids: AssistantId): EnabledAssistants =
        ids.fold(EnabledAssistants()) { current, id -> current.enable(id) }

    private fun allAssistants(): EnabledAssistants =
        AssistantCatalog.assistants.fold(EnabledAssistants()) { current, id -> current.enable(id) }

    private fun write(root: Path, relative: String, text: String) {
        val path = resolve(root, relative)
        path.parent?.let { Files.createDirectories(it) }
        Files.writeString(path, text)
    }

    private fun read(root: Path, relative: String): String = Files.readString(resolve(root, relative))

    private fun exists(root: Path, relative: String): Boolean = Files.exists(resolve(root, relative))

    private fun resolve(root: Path, relative: String): Path =
        relative.split('/').fold(root) { parent, name -> parent.resolve(name) }
}
