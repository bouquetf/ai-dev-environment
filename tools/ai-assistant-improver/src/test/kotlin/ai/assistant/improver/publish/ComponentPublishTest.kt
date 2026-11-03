package ai.assistant.improver.publish

import ai.assistant.improver.component.ComponentId
import ai.assistant.improver.component.ComponentKind
import ai.assistant.improver.install.ContentDigest
import ai.assistant.improver.install.InstallationSnapshot
import ai.assistant.improver.install.InstalledFile
import ai.assistant.improver.repository.RepositoryMode
import ai.assistant.improver.repository.SourceRepositories
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ComponentPublishTest {
    @Test
    fun `download only is excluded and an unchanged synchronised file is not proposed`(@TempDir root: Path) {
        val download = repository(root, "download")
        val synchronised = repository(root, "sync", RepositoryMode.SYNCHRONISATION)
        val project = Files.createDirectories(root.resolve("project"))
        write(project, "download.txt", "changed")
        write(synchronised.root, "ia/skills/demo/SKILL.md", "same")
        write(project, "sync.txt", "same")
        val snapshot = InstallationSnapshot.restore(
            listOf(
                installed(download.path, "ia/skills/demo/SKILL.md", "download.txt", "original"),
                installed(synchronised.path, "ia/skills/demo/SKILL.md", "sync.txt", "same"),
            ),
        )
        val repositories = SourceRepositories.restore(
            listOf(download.repositories.entries().single(), synchronised.repositories.entries().single()),
        )
        var confirmed = false

        val report = ComponentPublish.publish(
            project,
            repositories,
            snapshot,
            PublishConfirmation {
                confirmed = true
                null
            },
            ForbiddenGit,
        )

        assertFalse(confirmed)
        assertFalse(report.cancelled)
        assertNull(report.failure)
        assertTrue(report.files.isEmpty())
        assertEquals(snapshot, report.snapshot)
    }

    @Test
    fun `an unchecked component stays publishable`(@TempDir root: Path) {
        val repository = repository(root, "sync", RepositoryMode.SYNCHRONISATION)
        val project = Files.createDirectories(root.resolve("project"))
        write(repository.root, "ia/skills/demo/SKILL.md", "original")
        write(project, ".cursor/skills/demo/SKILL.md", "local")
        val snapshot = InstallationSnapshot.restore(
            listOf(installed(repository.path, "ia/skills/demo/SKILL.md", ".cursor/skills/demo/SKILL.md", "original")),
        )
        var seen = emptyList<PublishFile>()

        val report = ComponentPublish.publish(
            project,
            repository.repositories,
            snapshot,
            PublishConfirmation {
                seen = it
                null
            },
            ForbiddenGit,
        )

        assertEquals("ia/skills/demo/SKILL.md", seen.single().sourcePath)
        assertEquals(".cursor/skills/demo/SKILL.md", seen.single().destinationPath)
        assertEquals(seen, report.files)
        assertTrue(report.cancelled)
    }

    @Test
    fun `cancel writes nothing and does not call git`(@TempDir root: Path) {
        val repository = repository(root, "sync", RepositoryMode.SYNCHRONISATION)
        val project = Files.createDirectories(root.resolve("project"))
        write(repository.root, "ia/skills/demo/SKILL.md", "original")
        write(project, ".cursor/skills/demo/SKILL.md", "local")
        val snapshot = InstallationSnapshot.restore(
            listOf(installed(repository.path, "ia/skills/demo/SKILL.md", ".cursor/skills/demo/SKILL.md", "original")),
        )
        var seen = emptyList<PublishFile>()

        val report = ComponentPublish.publish(
            project,
            repository.repositories,
            snapshot,
            PublishConfirmation {
                seen = it
                null
            },
            ForbiddenGit,
        )

        assertEquals("ia/skills/demo/SKILL.md", seen.single().sourcePath)
        assertEquals(".cursor/skills/demo/SKILL.md", seen.single().destinationPath)
        assertEquals("original", read(repository.root, "ia/skills/demo/SKILL.md"))
        assertTrue(report.cancelled)
        assertNull(report.failure)
    }

    @Test
    fun `a blank message does not call git`(@TempDir root: Path) {
        val repository = repository(root, "sync", RepositoryMode.SYNCHRONISATION)
        val project = Files.createDirectories(root.resolve("project"))
        write(repository.root, "ia/skills/demo/SKILL.md", "original")
        write(project, ".cursor/skills/demo/SKILL.md", "local")
        val snapshot = InstallationSnapshot.restore(
            listOf(installed(repository.path, "ia/skills/demo/SKILL.md", ".cursor/skills/demo/SKILL.md", "original")),
        )

        val report = ComponentPublish.publish(
            project,
            repository.repositories,
            snapshot,
            PublishConfirmation { "  " },
            ForbiddenGit,
        )

        assertEquals(PublishFailureStatus.EMPTY_MESSAGE, report.failure?.status)
        assertEquals("", report.failure?.repositoryPath)
        assertEquals("original", read(repository.root, "ia/skills/demo/SKILL.md"))
        assertFalse(report.cancelled)
    }

    @Test
    fun `divergence copies nothing and does not commit`(@TempDir root: Path) {
        val repository = repository(root, "sync", RepositoryMode.SYNCHRONISATION)
        val project = Files.createDirectories(root.resolve("project"))
        write(repository.root, "ia/skills/demo/SKILL.md", "origin-changed")
        write(project, ".cursor/skills/demo/SKILL.md", "dest-changed")
        val snapshot = InstallationSnapshot.restore(
            listOf(installed(repository.path, "ia/skills/demo/SKILL.md", ".cursor/skills/demo/SKILL.md", "snapshot")),
        )
        val git = scripted { args ->
            when (args.first()) {
                "remote" -> GitOutput(0, "url\n")
                "rev-parse" -> GitOutput(0, "main\n")
                else -> error("unexpected git: $args")
            }
        }

        val report = ComponentPublish.publish(
            project,
            repository.repositories,
            snapshot,
            PublishConfirmation { "Publish skill" },
            git,
        )

        assertEquals(PublishFailureStatus.DIVERGENCE, report.failure?.status)
        assertEquals("origin-changed", read(repository.root, "ia/skills/demo/SKILL.md"))
        assertTrue(git.calls.none { it.first() == "add" || it.first() == "commit" || it.first() == "push" })
    }

    @Test
    fun `two local copies of the same source that disagree are divergence`(@TempDir root: Path) {
        val repository = repository(root, "sync", RepositoryMode.SYNCHRONISATION)
        val project = Files.createDirectories(root.resolve("project"))
        write(repository.root, "ia/skills/demo/SKILL.md", "snapshot")
        write(project, ".cursor/skills/demo/SKILL.md", "alpha")
        write(project, ".claude/skills/demo/SKILL.md", "beta")
        val snapshot = InstallationSnapshot.restore(
            listOf(
                installed(repository.path, "ia/skills/demo/SKILL.md", ".cursor/skills/demo/SKILL.md", "snapshot"),
                installed(repository.path, "ia/skills/demo/SKILL.md", ".claude/skills/demo/SKILL.md", "snapshot"),
            ),
        )
        val git = scripted { args ->
            when (args.first()) {
                "remote" -> GitOutput(0, "url\n")
                "rev-parse" -> GitOutput(0, "main\n")
                else -> error("unexpected git: $args")
            }
        }

        val report = ComponentPublish.publish(
            project,
            repository.repositories,
            snapshot,
            PublishConfirmation { "Publish skill" },
            git,
        )

        assertEquals(PublishFailureStatus.DIVERGENCE, report.failure?.status)
        assertEquals("snapshot", read(repository.root, "ia/skills/demo/SKILL.md"))
        assertTrue(git.calls.none { it.first() == "add" || it.first() == "commit" || it.first() == "push" })
    }

    @Test
    fun `only the changed source path is indexed`(@TempDir root: Path) {
        val repository = repository(root, "sync", RepositoryMode.SYNCHRONISATION)
        val project = Files.createDirectories(root.resolve("project"))
        write(repository.root, "ia/skills/demo/SKILL.md", "original")
        write(repository.root, "ia/skills/demo/NOTES.md", "note")
        write(project, ".cursor/skills/demo/SKILL.md", "local")
        write(project, ".cursor/skills/demo/NOTES.md", "note")
        val snapshot = InstallationSnapshot.restore(
            listOf(
                installed(repository.path, "ia/skills/demo/SKILL.md", ".cursor/skills/demo/SKILL.md", "original"),
                installed(repository.path, "ia/skills/demo/NOTES.md", ".cursor/skills/demo/NOTES.md", "note"),
            ),
        )
        val git = accepting(status = " M ia/skills/demo/SKILL.md\n")

        val report = ComponentPublish.publish(
            project,
            repository.repositories,
            snapshot,
            PublishConfirmation { "Publish the skill" },
            git,
        )

        val added = git.calls.first { it.first() == "add" }
        val committed = git.calls.first { it.first() == "commit" }
        assertTrue(added.contains("ia/skills/demo/SKILL.md"))
        assertFalse(added.contains("ia/skills/demo/NOTES.md"))
        assertTrue(committed.contains("ia/skills/demo/SKILL.md"))
        assertFalse(committed.contains("ia/skills/demo/NOTES.md"))
        assertEquals("Publish the skill", committed[committed.indexOf("-m") + 1])
        assertEquals(
            ContentDigest.sha256("local".toByteArray(Charsets.UTF_8)),
            report.snapshot.files().single { it.sourcePath.endsWith("SKILL.md") }.sha256,
        )
        assertEquals(
            ContentDigest.sha256("note".toByteArray(Charsets.UTF_8)),
            report.snapshot.files().single { it.sourcePath.endsWith("NOTES.md") }.sha256,
        )
        assertEquals(listOf(repository.path), report.publishedRepositories)
        assertNull(report.failure)
    }

    @Test
    fun `a push failure keeps the snapshot and a second call does not commit again`(@TempDir root: Path) {
        val repository = repository(root, "sync", RepositoryMode.SYNCHRONISATION)
        val project = Files.createDirectories(root.resolve("project"))
        write(repository.root, "ia/skills/demo/SKILL.md", "original")
        write(project, ".cursor/skills/demo/SKILL.md", "local")
        val snapshot = InstallationSnapshot.restore(
            listOf(installed(repository.path, "ia/skills/demo/SKILL.md", ".cursor/skills/demo/SKILL.md", "original")),
        )
        val git = PhaseGit()

        val first = ComponentPublish.publish(
            project,
            repository.repositories,
            snapshot,
            PublishConfirmation { "Publish skill" },
            git,
        )

        assertEquals(PublishFailureStatus.PUSH_FAILED, first.failure?.status)
        assertEquals(snapshot.files().single().sha256, first.snapshot.files().single().sha256)
        assertEquals("local", read(repository.root, "ia/skills/demo/SKILL.md"))
        val afterPush = git.calls.size

        val second = ComponentPublish.publish(
            project,
            repository.repositories,
            first.snapshot,
            PublishConfirmation { "Publish skill" },
            git,
        )

        assertNull(second.failure)
        assertTrue(git.calls.drop(afterPush).none { it.first() == "add" || it.first() == "commit" })
        assertEquals(
            ContentDigest.sha256("local".toByteArray(Charsets.UTF_8)),
            second.snapshot.files().single().sha256,
        )
    }

    @Test
    fun `a published repository is kept when the next repository has no origin`(@TempDir root: Path) {
        val first = repository(root, "first", RepositoryMode.SYNCHRONISATION)
        val second = repository(root, "second", RepositoryMode.SYNCHRONISATION)
        val project = Files.createDirectories(root.resolve("project"))
        write(first.root, "ia/skills/one/SKILL.md", "one")
        write(second.root, "ia/skills/two/SKILL.md", "two")
        write(project, "one.txt", "one-local")
        write(project, "two.txt", "two-local")
        val snapshot = InstallationSnapshot.restore(
            listOf(
                installed(first.path, "ia/skills/one/SKILL.md", "one.txt", "one"),
                installed(second.path, "ia/skills/two/SKILL.md", "two.txt", "two"),
            ),
        )
        val repositories = SourceRepositories.restore(
            listOf(
                first.repositories.entries().single(),
                second.repositories.entries().single(),
            ),
        )
        val git = scripted { args ->
            val repository = args.repository
            if (repository == second.root && args.args.first() == "remote") {
                GitOutput(1, "No such remote\n")
            } else {
                when (args.args.first()) {
                    "remote" -> GitOutput(0, "url\n")
                    "rev-parse" -> GitOutput(0, "main\n")
                    "status" -> GitOutput(0, " M file\n")
                    "add", "commit", "push" -> GitOutput(0, "")
                    else -> error("unexpected git: ${args.args}")
                }
            }
        }

        val report = ComponentPublish.publish(
            project,
            repositories,
            snapshot,
            PublishConfirmation { "Publish skill" },
            git,
        )

        assertEquals(PublishFailureStatus.NO_ORIGIN, report.failure?.status)
        assertEquals(second.path, report.failure?.repositoryPath)
        assertEquals(listOf(first.path), report.publishedRepositories)
        assertEquals(
            ContentDigest.sha256("one-local".toByteArray(Charsets.UTF_8)),
            report.snapshot.files().single { it.destinationPath == "one.txt" }.sha256,
        )
        assertEquals(
            ContentDigest.sha256("two".toByteArray(Charsets.UTF_8)),
            report.snapshot.files().single { it.destinationPath == "two.txt" }.sha256,
        )
        assertEquals("two", read(second.root, "ia/skills/two/SKILL.md"))
    }

    private data class PreparedRepository(val root: Path, val path: String, val repositories: SourceRepositories)

    private fun repository(
        root: Path,
        name: String,
        mode: RepositoryMode = RepositoryMode.DOWNLOAD_ONLY,
    ): PreparedRepository {
        val directory = Files.createDirectories(root.resolve(name))
        Files.createDirectories(directory.resolve(".git"))
        val added = SourceRepositories().add(directory.toString())
        val path = added.entries().single().path
        return PreparedRepository(Path.of(path), path, added.setMode(path, mode))
    }

    private fun installed(
        repositoryPath: String,
        sourcePath: String,
        destinationPath: String,
        text: String,
    ): InstalledFile = InstalledFile(
        repositoryPath,
        ComponentId(ComponentKind.SKILL, "ia/skills/demo"),
        sourcePath,
        destinationPath,
        ContentDigest.sha256(text.toByteArray(Charsets.UTF_8)),
    )

    private fun write(root: Path, relative: String, text: String) {
        val path = relative.split('/').fold(root) { parent, name -> parent.resolve(name) }
        path.parent?.let { Files.createDirectories(it) }
        Files.writeString(path, text)
    }

    private fun read(root: Path, relative: String): String {
        val path = relative.split('/').fold(root) { parent, name -> parent.resolve(name) }
        return Files.readString(path)
    }

    private class Call(val repository: Path, val args: List<String>) {
        fun first(): String = args.first()
    }

    private class RecordingGit(private val responder: (Call) -> GitOutput) : GitCommand {
        val calls = mutableListOf<List<String>>()

        override fun run(repository: Path, args: List<String>): GitOutput {
            calls += args
            return responder(Call(repository, args))
        }
    }

    private fun scripted(responder: (Call) -> GitOutput): RecordingGit = RecordingGit(responder)

    private fun accepting(status: String): RecordingGit = scripted { call ->
        when (call.args.first()) {
            "remote" -> GitOutput(0, "url\n")
            "rev-parse" -> GitOutput(0, "main\n")
            "status" -> GitOutput(0, status)
            "add", "commit", "push" -> GitOutput(0, "")
            else -> error("unexpected git: ${call.args}")
        }
    }

    private class PhaseGit : GitCommand {
        val calls = mutableListOf<List<String>>()
        private var statusCalls = 0
        private var pushCalls = 0

        override fun run(repository: Path, args: List<String>): GitOutput {
            calls += args
            return when (args.first()) {
                "remote" -> GitOutput(0, "url\n")
                "rev-parse" -> GitOutput(0, "main\n")
                "status" -> {
                    statusCalls += 1
                    if (statusCalls == 1) GitOutput(0, " M ia/skills/demo/SKILL.md\n") else GitOutput(0, "")
                }
                "add", "commit" -> GitOutput(0, "")
                "push" -> {
                    pushCalls += 1
                    if (pushCalls == 1) GitOutput(1, "rejected\n") else GitOutput(0, "")
                }
                else -> error("unexpected git: $args")
            }
        }
    }

    private object ForbiddenGit : GitCommand {
        override fun run(repository: Path, args: List<String>): GitOutput = error("git was called: $args")
    }
}
