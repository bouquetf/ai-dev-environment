package ai.assistant.improver.repository

import ai.assistant.improver.assistant.AssistantId
import ai.assistant.improver.assistant.EnabledAssistants
import ai.assistant.improver.component.ComponentDiscovery
import ai.assistant.improver.component.ComponentSelection
import ai.assistant.improver.install.ComponentImport
import ai.assistant.improver.install.ContentDigest
import ai.assistant.improver.install.InstallationSnapshot
import ai.assistant.improver.install.OverwriteConfirmation
import ai.assistant.improver.publish.ComponentPublish
import ai.assistant.improver.publish.GitCommand
import ai.assistant.improver.publish.GitOutput
import ai.assistant.improver.publish.ProcessGit
import ai.assistant.improver.publish.PublishConfirmation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

class RemoteRepositoryGitTest {
    @Test
    fun `an https url is cloned from a bare repository and published back`(@TempDir root: Path) {
        val bare = Files.createDirectories(root.resolve("origin.git"))
        val seed = root.resolve("seed")
        git(bare, "init", "--bare")
        Files.createDirectories(seed)
        git(seed, "init")
        write(seed, "ia/skills/specification-fr/SKILL.md", "specification\n")
        git(seed, "add", "--", "ia/skills/specification-fr/SKILL.md")
        git(seed, "commit", "-m", "initial")
        val bareUrl = bare.toAbsolutePath().normalize().toString().replace('\\', '/')
        git(seed, "remote", "add", "origin", bareUrl)
        val branch = git(seed, "rev-parse", "--abbrev-ref", "HEAD").trim()
        git(seed, "push", "-u", "origin", branch)

        val cache = root.resolve("cache")
        val project = root.resolve("project")
        val url = "https://plugins.test/repo.git"
        val gitCommand = RecordingEnvironmentGit(pluginEnvironment(bare))
        val added = SourceRepositories().add(url, cache, gitCommand)
        val clone = CacheDirectories.directory(cache, url)
        val cloneCall = gitCommand.calls.first { it.second.first() == "clone" }

        assertEquals(listOf("clone", "--", url, clone.toString()), cloneCall.second)
        assertTrue(gitCommand.calls.none { call ->
            call.second.any { arg -> arg == "--depth" || arg.contains("insteadOf") || arg.contains("http.extraHeader") }
        })
        assertEquals(
            listOf("ia/skills/specification-fr"),
            ComponentDiscovery.discover(clone).map { it.relativePath },
        )
        assertEquals("specification\n", Files.readString(clone.resolve("ia/skills/specification-fr/SKILL.md")))
        assertTrue(clone.normalize() != seed.normalize())

        val repositories = added.setMode(added.entries().single().path, RepositoryMode.SYNCHRONISATION)
        val imported = ComponentImport.import(
            project,
            repositories,
            ComponentSelection(),
            EnabledAssistants().enable(AssistantId.CURSOR),
            InstallationSnapshot(),
            OverwriteConfirmation { false },
            cacheRoot = cache,
            git = gitCommand,
        )
        assertEquals(url, imported.snapshot.files().single().repositoryPath)
        val destination = project.resolve(".cursor").resolve("skills").resolve("specification-fr").resolve("SKILL.md")
        Files.writeString(destination, "local-edit\n")

        val published = ComponentPublish.publish(
            project,
            repositories,
            imported.snapshot,
            PublishConfirmation { "Publish skill" },
            gitCommand,
            cache,
        )

        assertNull(published.failure)
        assertEquals(listOf(url), published.publishedRepositories)
        assertEquals(
            ContentDigest.sha256(Files.readAllBytes(destination)),
            published.snapshot.files().single().sha256,
        )
        assertEquals(git(clone, "rev-parse", "HEAD").trim(), git(bare, "rev-parse", "refs/heads/$branch").trim())
        assertEquals("Publish skill", git(clone, "log", "-1", "--format=%s").trim())
    }

    @Test
    fun `windows git commands allow long paths`(@TempDir root: Path) {
        assumeTrue(System.getProperty("os.name").lowercase(Locale.ROOT).contains("windows"))
        val result = ProcessGit.run(root, listOf("config", "--get", "core.longpaths"))
        assertEquals(0, result.exitCode)
        assertEquals("true", result.output.trim())
    }

    private class RecordingEnvironmentGit(private val extraEnvironment: Map<String, String>) : GitCommand {
        val calls = mutableListOf<Pair<Path, List<String>>>()

        override fun run(repository: Path, args: List<String>): GitOutput {
            calls += repository to args
            return ProcessGit.invoke(repository, args, extraEnvironment)
        }
    }

    private fun pluginEnvironment(bare: Path): Map<String, String> {
        val target = bare.toAbsolutePath().normalize().toString().replace('\\', '/')
        return baseEnvironment() + mapOf(
            "GIT_CONFIG_COUNT" to "3",
            "GIT_CONFIG_KEY_0" to "url.$target.insteadOf",
            "GIT_CONFIG_VALUE_0" to "https://plugins.test/repo.git",
            "GIT_CONFIG_KEY_1" to "commit.gpgsign",
            "GIT_CONFIG_VALUE_1" to "false",
            "GIT_CONFIG_KEY_2" to "core.autocrlf",
            "GIT_CONFIG_VALUE_2" to "false",
        )
    }

    private fun baseEnvironment(): Map<String, String> = mapOf(
        "GIT_CONFIG_COUNT" to "2",
        "GIT_CONFIG_KEY_0" to "commit.gpgsign",
        "GIT_CONFIG_VALUE_0" to "false",
        "GIT_CONFIG_KEY_1" to "core.autocrlf",
        "GIT_CONFIG_VALUE_1" to "false",
        "GIT_AUTHOR_NAME" to "Test",
        "GIT_AUTHOR_EMAIL" to "test@example.com",
        "GIT_COMMITTER_NAME" to "Test",
        "GIT_COMMITTER_EMAIL" to "test@example.com",
    )

    private fun git(directory: Path, vararg args: String): String {
        val result = ProcessGit.invoke(directory, args.toList(), baseEnvironment())
        if (result.exitCode != 0) {
            error("git ${args.toList()} -> ${result.exitCode}\n${result.output}")
        }
        return result.output
    }

    private fun write(root: Path, relative: String, text: String) {
        val path = relative.split('/').fold(root) { parent, name -> parent.resolve(name) }
        path.parent?.let { Files.createDirectories(it) }
        Files.writeString(path, text)
    }
}
