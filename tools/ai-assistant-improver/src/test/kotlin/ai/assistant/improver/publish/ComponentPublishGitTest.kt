package ai.assistant.improver.publish

import ai.assistant.improver.assistant.AssistantId
import ai.assistant.improver.assistant.EnabledAssistants
import ai.assistant.improver.component.ComponentSelection
import ai.assistant.improver.install.ComponentImport
import ai.assistant.improver.install.ContentDigest
import ai.assistant.improver.install.InstallationSnapshot
import ai.assistant.improver.install.OverwriteConfirmation
import ai.assistant.improver.repository.RepositoryMode
import ai.assistant.improver.repository.SourceRepositories
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ComponentPublishGitTest {
    @Test
    fun `a modified install is committed alone and the bare origin moves`(@TempDir root: Path) {
        val fixture = install(root)
        Files.writeString(fixture.destination, "local-edit\n")
        Files.writeString(fixture.clone.resolve("README.md"), "dirty\n")
        git(fixture.clone, "add", "README.md")

        val report = publish(fixture, "Publish skill")

        assertNull(report.failure)
        assertEquals("Publish skill", git(fixture.clone, "log", "-1", "--format=%s").trim())
        assertEquals(
            listOf("ia/skills/specification-fr/SKILL.md"),
            lines(git(fixture.clone, "show", "--name-only", "--pretty=format:", "HEAD")),
        )
        assertEquals(listOf("README.md"), lines(git(fixture.clone, "diff", "--cached", "--name-only")))
        val branch = git(fixture.clone, "rev-parse", "--abbrev-ref", "HEAD").trim()
        assertEquals(
            git(fixture.clone, "rev-parse", "HEAD").trim(),
            git(fixture.bare, "rev-parse", "refs/heads/$branch").trim(),
        )
        assertEquals(
            ContentDigest.sha256(Files.readAllBytes(fixture.destination)),
            report.snapshot.files().single().sha256,
        )
        val helper = ProcessGit.run(fixture.clone, listOf("config", "--local", "--get", "credential.helper"))
        assertTrue(helper.output.isBlank())
    }

    @Test
    fun `a missing origin creates no commit`(@TempDir root: Path) {
        val fixture = install(root)
        Files.writeString(fixture.destination, "local-edit\n")
        val before = commitCount(fixture.clone)
        val digest = fixture.snapshot.files().single().sha256
        git(fixture.clone, "remote", "remove", "origin")

        val report = publish(fixture, "Publish skill")

        assertEquals(PublishFailureStatus.NO_ORIGIN, report.failure?.status)
        assertEquals(before, commitCount(fixture.clone))
        assertEquals("specification\n", Files.readString(fixture.skill))
        assertEquals(digest, report.snapshot.files().single().sha256)
    }

    @Test
    fun `a failed push is not followed by a second commit`(@TempDir root: Path) {
        val fixture = install(root)
        Files.writeString(fixture.destination, "local-edit\n")
        val before = commitCount(fixture.clone)
        val digest = fixture.snapshot.files().single().sha256
        val missing = root.resolve("missing.git").toAbsolutePath().toString()
        git(fixture.clone, "remote", "set-url", "origin", missing)

        val failed = publish(fixture, "Publish skill")

        assertEquals(PublishFailureStatus.PUSH_FAILED, failed.failure?.status)
        assertEquals(before + 1, commitCount(fixture.clone))
        assertEquals(digest, failed.snapshot.files().single().sha256)
        git(fixture.clone, "remote", "set-url", "origin", fixture.bare.toString())

        val pushed = publish(fixture, failed.snapshot, "Publish skill")

        assertNull(pushed.failure)
        assertEquals(before + 1, commitCount(fixture.clone))
        val branch = git(fixture.clone, "rev-parse", "--abbrev-ref", "HEAD").trim()
        assertEquals(
            git(fixture.clone, "rev-parse", "HEAD").trim(),
            git(fixture.bare, "rev-parse", "refs/heads/$branch").trim(),
        )
        assertEquals(
            ContentDigest.sha256(Files.readAllBytes(fixture.destination)),
            pushed.snapshot.files().single().sha256,
        )
    }

    private data class Fixture(
        val bare: Path,
        val clone: Path,
        val project: Path,
        val repositories: SourceRepositories,
        val snapshot: InstallationSnapshot,
        val destination: Path,
        val skill: Path,
    )

    private fun install(root: Path): Fixture {
        val bare = Files.createDirectories(root.resolve("origin.git"))
        val clone = root.resolve("source")
        val project = root.resolve("project")
        git(bare, "init", "--bare")
        Files.createDirectories(clone)
        git(clone, "init")
        git(clone, "config", "user.name", "Test")
        git(clone, "config", "user.email", "test@example.com")
        git(clone, "config", "core.autocrlf", "false")
        git(clone, "config", "commit.gpgsign", "false")
        write(clone, "ia/skills/specification-fr/SKILL.md", "specification\n")
        write(clone, "README.md", "readme\n")
        git(clone, "add", "--", "ia/skills/specification-fr/SKILL.md", "README.md")
        git(clone, "commit", "-m", "initial")
        git(clone, "remote", "add", "origin", bare.toAbsolutePath().toString())
        val branch = git(clone, "rev-parse", "--abbrev-ref", "HEAD").trim()
        git(clone, "push", "-u", "origin", branch)
        val added = SourceRepositories().add(clone.toString())
        val repositories = added.setMode(added.entries().single().path, RepositoryMode.SYNCHRONISATION)
        val imported = ComponentImport.import(
            project,
            repositories,
            ComponentSelection(),
            EnabledAssistants().enable(AssistantId.CURSOR),
            InstallationSnapshot(),
            OverwriteConfirmation { false },
        )
        return Fixture(
            bare,
            Path.of(added.entries().single().path),
            project,
            repositories,
            imported.snapshot,
            project.resolve(".cursor").resolve("skills").resolve("specification-fr").resolve("SKILL.md"),
            Path.of(added.entries().single().path).resolve("ia").resolve("skills").resolve("specification-fr").resolve("SKILL.md"),
        )
    }

    private fun publish(fixture: Fixture, message: String): PublishReport =
        publish(fixture, fixture.snapshot, message)

    private fun publish(fixture: Fixture, snapshot: InstallationSnapshot, message: String): PublishReport =
        ComponentPublish.publish(
            fixture.project,
            fixture.repositories,
            snapshot,
            PublishConfirmation { message },
        )

    private fun commitCount(repository: Path): Int =
        git(repository, "rev-list", "--count", "HEAD").trim().toInt()

    private fun git(directory: Path, vararg args: String): String {
        val result = ProcessGit.run(directory, args.toList())
        if (result.exitCode != 0) {
            error("git ${args.toList()} -> ${result.exitCode}\n${result.output}")
        }
        return result.output
    }

    private fun lines(output: String): List<String> =
        output.lines().map { it.trim() }.filter { it.isNotEmpty() }

    private fun write(root: Path, relative: String, text: String) {
        val path = relative.split('/').fold(root) { parent, name -> parent.resolve(name) }
        path.parent?.let { Files.createDirectories(it) }
        Files.writeString(path, text)
    }
}
