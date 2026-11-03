package ai.assistant.improver.install

import ai.assistant.improver.assistant.AssistantCatalog
import ai.assistant.improver.assistant.AssistantId
import ai.assistant.improver.assistant.EnabledAssistants
import ai.assistant.improver.component.ComponentId
import ai.assistant.improver.component.ComponentKind
import ai.assistant.improver.component.ComponentSelection
import ai.assistant.improver.repository.RepositoryMode
import ai.assistant.improver.repository.SourceRepositories
import ai.assistant.improver.repository.SourceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ComponentImportIntegrationTest {
    @Test
    fun `selected skill is copied to every enabled assistant and CLAUDE md stays at the project root`(
        @TempDir source: Path,
        @TempDir project: Path,
    ) {
        write(source, "ia/skills/specification-fr/SKILL.md", "specification")
        write(source, "ia/skills/specification-fr/extra/note.md", "note")
        write(source, "ia/skills/other/SKILL.md", "other")
        write(source, "CLAUDE.md", "guide")
        val assistants = AssistantCatalog.assistants.fold(EnabledAssistants()) { current, id -> current.enable(id) }
        val selection = ComponentSelection().setSelected(
            source.toString(),
            ComponentId(ComponentKind.SKILL, "ia/skills/other"),
            false,
        )
        val repositories = SourceRepositories.restore(
            listOf(SourceRepository(source.toString(), RepositoryMode.DOWNLOAD_ONLY)),
        )

        val report = ComponentImport.import(
            project,
            repositories,
            selection,
            assistants,
            InstallationSnapshot(),
            OverwriteConfirmation { prompt -> error("Unexpected confirmation for ${prompt.destinationPath}") },
        )

        listOf(".cursor", ".claude", ".opencode", ".junie").forEach { root ->
            assertEquals("specification", read(project, "$root/skills/specification-fr/SKILL.md"))
            assertEquals("note", read(project, "$root/skills/specification-fr/extra/note.md"))
            assertFalse(exists(project, "$root/skills/other"))
            assertFalse(exists(project, "$root/CLAUDE.md"))
        }
        assertEquals("guide", read(project, "CLAUDE.md"))
        assertEquals("specification", read(source, "ia/skills/specification-fr/SKILL.md"))
        assertEquals("note", read(source, "ia/skills/specification-fr/extra/note.md"))
        assertEquals("other", read(source, "ia/skills/other/SKILL.md"))
        assertEquals("guide", read(source, "CLAUDE.md"))
        val files = report.snapshot.files()
        assertEquals(8, files.count { it.component.relativePath == "ia/skills/specification-fr" })
        assertEquals(1, files.count { it.sourcePath == "CLAUDE.md" && it.destinationPath == "CLAUDE.md" })
        assertEquals(9, files.size)
        assertEquals(
            listOf(ImportAction.NO_TARGET, ImportAction.COPIED, ImportAction.NO_TARGET, ImportAction.NO_TARGET),
            report.destinations.filter { it.component.relativePath == "CLAUDE.md" }.map { it.action },
        )
        assertEquals(
            listOf(AssistantId.CURSOR, AssistantId.CLAUDE, AssistantId.OPENCODE, AssistantId.JUNIE),
            report.destinations.filter { it.component.relativePath == "CLAUDE.md" }.map { it.assistant },
        )
        assertTrue(report.destinations.none { it.component.relativePath == "ia/skills/other" })
    }

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
