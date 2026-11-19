package ai.assistant.improver.repository

import ai.assistant.improver.publish.GitCommand
import ai.assistant.improver.publish.GitOutput
import ai.assistant.improver.settings.SOURCE_CACHE_DIRECTORY
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class SourceRepositoriesTest {
    @Test
    fun `empty path is refused and not added`() {
        val initial = SourceRepositories()
        val empty = assertThrows<RepositoryRejectedException> { initial.add("") }
        val blank = assertThrows<RepositoryRejectedException> { initial.add("   ") }
        assertEquals(RepositoryRejectedException.Reason.EMPTY, empty.reason)
        assertEquals(RepositoryRejectedException.Reason.EMPTY, blank.reason)
        assertEquals("settings.error.repository.empty", empty.reason.labelKey)
        assertTrue(initial.entries().isEmpty())
    }

    @Test
    fun `a file is refused and not added`(@TempDir tempDir: Path) {
        val file = Files.writeString(tempDir.resolve("notes.txt"), "x")
        val initial = SourceRepositories()
        val raw = "  $file  "
        val error = assertThrows<RepositoryRejectedException> { initial.add(raw) }
        assertEquals(RepositoryRejectedException.Reason.NOT_A_DIRECTORY, error.reason)
        assertEquals(file.toString(), error.path)
        assertTrue(initial.entries().isEmpty())
    }

    @Test
    fun `git http and https urls are cloned in download only`(@TempDir tempDir: Path) {
        val cache = tempDir.resolve("sources")
        val git = acceptingClone()
        val initial = SourceRepositories()
        val https = initial.add("https://host/repo.git", cache, git)
        val http = https.add("http://host/repo.git", cache, git)
        val gitScheme = http.add("git://host/repo.git", cache, git)
        assertTrue(initial.entries().isEmpty())
        assertTrue(gitScheme.entries().all { it.mode == RepositoryMode.DOWNLOAD_ONLY })
        assertEquals(
            listOf("https://host/repo.git", "http://host/repo.git", "git://host/repo.git"),
            gitScheme.entries().map { it.path },
        )
        assertEquals(3, git.calls.size)
        gitScheme.entries().forEach { entry ->
            val call = git.calls.first { it.second[2] == entry.path }
            val destination = CacheDirectories.directory(cache, entry.path)
            assertEquals(cache.toAbsolutePath().normalize(), call.first.toAbsolutePath().normalize())
            assertEquals(listOf("clone", "--", entry.path, destination.toString()), call.second)
            assertTrue(call.second.none { arg -> arg == "--depth" || arg == "--config" || arg.contains("insteadOf") })
        }
    }

    @Test
    fun `scheme case is lowered and the rest of the url is kept`(@TempDir tempDir: Path) {
        val cache = tempDir.resolve("sources")
        val git = acceptingClone()
        val added = SourceRepositories().add("  HTTPS://Hôte/Repo.git  ", cache, git)
        assertEquals("https://Hôte/Repo.git", added.entries().single().path)
        assertEquals(RepositoryMode.DOWNLOAD_ONLY, added.entries().single().mode)
        assertEquals("https://Hôte/Repo.git", git.calls.single().second[2])
    }

    @Test
    fun `a rejected url is not stored and does not clone`(@TempDir tempDir: Path) {
        val cache = tempDir.resolve("sources")
        val git = acceptingClone()
        val rejected = listOf(
            "ssh://host/repo.git",
            "file:///tmp/repo.git",
            "https://",
            "https:///repo.git",
            "https://user:secret@host/repo.git",
            "https://user@host/repo.git",
            "git:secret@host:repo.git",
        )
        val initial = SourceRepositories()
        rejected.forEach { raw ->
            val error = assertThrows<RepositoryRejectedException> { initial.add(raw, cache, git) }
            assertEquals(RepositoryRejectedException.Reason.INVALID_URL, error.reason)
            assertEquals(raw, error.path)
        }
        assertTrue(initial.entries().isEmpty())
        assertTrue(git.calls.isEmpty())
        assertTrue(initial.entries().none { it.path.contains("secret") })
    }

    @Test
    fun `github scp and https urls are cloned in download only`(@TempDir tempDir: Path) {
        val cache = tempDir.resolve("sources")
        val git = acceptingClone()
        val scp = "git@github.com:bouquetf/ai-dev-environment.git"
        val https = "https://github.com/bouquetf/ai-dev-environment.git"
        val added = SourceRepositories().add("  $scp  ", cache, git).add(https, cache, git)
        assertEquals(listOf(scp, https), added.entries().map { it.path })
        assertTrue(added.entries().all { it.mode == RepositoryMode.DOWNLOAD_ONLY })
        listOf(scp, https).forEach { url ->
            val destination = CacheDirectories.directory(cache, url)
            val call = git.calls.first { it.second[2] == url }
            assertEquals(cache.toAbsolutePath().normalize(), call.first.toAbsolutePath().normalize())
            assertEquals(listOf("clone", "--", url, destination.toString()), call.second)
            assertEquals(destination, CacheDirectories.workTree(url, cache))
            assertTrue(call.second.none { arg -> arg == "--depth" || arg == "--config" || arg.contains("insteadOf") })
        }
        val duplicate = assertThrows<RepositoryRejectedException> { added.add(scp, cache, git) }
        assertEquals(RepositoryRejectedException.Reason.DUPLICATE, duplicate.reason)
        assertEquals(2, git.calls.size)
        val destination = CacheDirectories.directory(cache, scp)
        Files.createDirectories(destination)
        Files.writeString(destination.resolve("README.md"), "clone")
        val removed = added.remove(scp, cache)
        assertFalse(Files.exists(destination))
        assertEquals(listOf(https), removed.entries().map { it.path })
    }

    @Test
    fun `an scp address without a path and an invalid local path are not cloned`(@TempDir tempDir: Path) {
        val cache = tempDir.resolve("sources")
        val git = acceptingClone()
        val missingHost = assertThrows<RepositoryRejectedException> {
            SourceRepositories().add("git@:path", cache, git)
        }
        assertEquals(RepositoryRejectedException.Reason.INVALID_URL, missingHost.reason)
        val noColon = assertThrows<RepositoryRejectedException> { SourceRepositories().add("git@host", cache, git) }
        assertTrue(
            noColon.reason == RepositoryRejectedException.Reason.NOT_A_DIRECTORY ||
                noColon.reason == RepositoryRejectedException.Reason.NOT_A_GIT_REPOSITORY,
        )
        val missing = assertThrows<RepositoryRejectedException> {
            SourceRepositories().add(tempDir.resolve("missing").toString(), cache, git)
        }
        assertEquals(RepositoryRejectedException.Reason.NOT_A_DIRECTORY, missing.reason)
        assertTrue(git.calls.isEmpty())
    }

    @Test
    fun `the same retained url is a duplicate and a trailing slash is not`(@TempDir tempDir: Path) {
        val cache = tempDir.resolve("sources")
        val git = acceptingClone()
        val added = SourceRepositories().add("HTTPS://Host/a", cache, git)
        val duplicate = assertThrows<RepositoryRejectedException> { added.add("https://Host/a", cache, git) }
        assertEquals(RepositoryRejectedException.Reason.DUPLICATE, duplicate.reason)
        assertEquals(1, git.calls.size)
        assertEquals(1, added.entries().size)
        val slashed = added.add("https://Host/a/", cache, git)
        assertEquals(listOf("https://Host/a", "https://Host/a/"), slashed.entries().map { it.path })
        assertEquals(2, git.calls.size)
    }

    @Test
    fun `a local directory is not cloned into the cache`(@TempDir tempDir: Path) {
        val cache = tempDir.resolve("sources")
        val git = acceptingClone()
        val repo = gitDirectory(tempDir, "repo")
        val added = SourceRepositories().add(repo.toString(), cache, git)
        assertEquals(repo.toRealPath().toString(), added.entries().single().path)
        assertEquals(RepositoryMode.DOWNLOAD_ONLY, added.entries().single().mode)
        assertTrue(git.calls.isEmpty())
        assertFalse(Files.exists(cache))
    }

    @Test
    fun `a missing cache refuses the url without calling git`() {
        val git = acceptingClone()
        val error = assertThrows<RepositoryRejectedException> {
            SourceRepositories().add("https://host/repo.git", null, git)
        }
        assertEquals(RepositoryRejectedException.Reason.CLONE_FAILED, error.reason)
        assertEquals("https://host/repo.git", error.path)
        assertTrue(git.calls.isEmpty())
    }

    @Test
    fun `a failed clone stores nothing and deletes the partial directory`(@TempDir tempDir: Path) {
        val cache = tempDir.resolve("sources")
        val outside = Files.writeString(tempDir.resolve("outside.txt"), "keep")
        val extra = Files.createDirectories(tempDir.resolve("sources-extra"))
        Files.writeString(extra.resolve("file.txt"), "keep")
        val url = "https://host/repo.git"
        val destination = CacheDirectories.directory(cache, url)
        Files.createDirectories(destination.resolve("objects"))
        Files.writeString(destination.resolve("objects").resolve("pack"), "partial")
        val git = ScriptedGit { _, args ->
            assertEquals(listOf("clone", "--", url, destination.toString()), args)
            GitOutput(1, "failed")
        }
        val initial = SourceRepositories()
        val error = assertThrows<RepositoryRejectedException> { initial.add(url, cache, git) }
        assertEquals(RepositoryRejectedException.Reason.CLONE_FAILED, error.reason)
        assertEquals(url, error.path)
        assertTrue(initial.entries().isEmpty())
        assertFalse(Files.exists(destination))
        assertEquals("keep", Files.readString(outside))
        assertEquals("keep", Files.readString(extra.resolve("file.txt")))
    }

    @Test
    fun `removing a url deletes only its cache clone`(@TempDir tempDir: Path) {
        val cache = tempDir.resolve("sources")
        val url = "https://host/repo.git"
        val added = SourceRepositories().add(url, cache, acceptingClone())
        val destination = CacheDirectories.directory(cache, url)
        Files.createDirectories(destination.resolve("objects"))
        Files.writeString(destination.resolve("README.md"), "clone")
        val outside = Files.writeString(tempDir.resolve("outside.txt"), "keep")
        val extra = Files.createDirectories(tempDir.resolve("sources-extra"))
        Files.writeString(extra.resolve("file.txt"), "keep")
        val projectFile = tempDir.resolve("project").resolve(".cursor/skills/specification-fr/SKILL.md")
        Files.createDirectories(projectFile.parent)
        Files.writeString(projectFile, "installed")

        val removed = added.remove(url, cache)
        assertTrue(removed.entries().isEmpty())
        assertEquals(1, added.entries().size)
        assertFalse(Files.exists(destination))
        assertEquals("keep", Files.readString(outside))
        assertEquals("keep", Files.readString(extra.resolve("file.txt")))
        assertEquals("installed", Files.readString(projectFile))
    }

    @Test
    fun `removing a url whose clone is already gone still drops the entry`(@TempDir tempDir: Path) {
        val cache = tempDir.resolve("sources")
        val url = "https://host/repo.git"
        val added = SourceRepositories().add(url, cache, acceptingClone())
        val removed = added.remove(url, cache)
        assertTrue(removed.entries().isEmpty())
        assertFalse(Files.exists(CacheDirectories.directory(cache, url)))
    }

    @Test
    fun `removing a local path does not delete it or the cache`(@TempDir tempDir: Path) {
        val cache = tempDir.resolve("sources")
        val repo = gitDirectory(tempDir, "repo")
        val readme = Files.writeString(repo.resolve("README.md"), "local")
        val projectFile = tempDir.resolve("project").resolve(".cursor/skills/specification-fr/SKILL.md")
        Files.createDirectories(projectFile.parent)
        Files.writeString(projectFile, "installed")
        val added = SourceRepositories().add(repo.toString(), cache, acceptingClone())
        val path = added.entries().single().path
        val removed = added.remove(path, cache)
        assertTrue(removed.entries().isEmpty())
        assertEquals("local", Files.readString(readme))
        assertEquals("installed", Files.readString(projectFile))
        assertFalse(Files.exists(cache))
    }

    @Test
    fun `removing an unknown path keeps a url and its clone`(@TempDir tempDir: Path) {
        val cache = tempDir.resolve("sources")
        val url = "https://host/repo.git"
        val added = SourceRepositories().add(url, cache, acceptingClone())
        val destination = CacheDirectories.directory(cache, url)
        Files.createDirectories(destination)
        Files.writeString(destination.resolve("README.md"), "clone")
        val kept = added.remove("https://other.example/repo.git", cache)
        assertEquals(added, kept)
        assertEquals("clone", Files.readString(destination.resolve("README.md")))
    }

    @Test
    fun `a url with an ampersand round trips beside a local repository`(@TempDir tempDir: Path) {
        val cache = tempDir.resolve("sources")
        val repo = gitDirectory(tempDir, "one")
        val url = "https://host/repo.git?x=1&y=2"
        val state = SourceRepositories().add(repo.toString()).add(url, cache, acceptingClone())
        val xml = state.toXml()
        assertTrue(xml.contains("&amp;"))
        assertTrue(xml.indexOf(repo.toRealPath().toString().replace("&", "&amp;")) < xml.indexOf("https://host/repo.git"))
        assertEquals(state, SourceRepositories.fromXml(xml))
        assertEquals(
            listOf(repo.toRealPath().toString(), url),
            SourceRepositories.fromXml(xml).entries().map { it.path },
        )
    }

    @Test
    fun `the ide cache directory name is the project source cache`() {
        assertEquals("ai-assistant-improver/sources", SOURCE_CACHE_DIRECTORY)
    }

    @Test
    fun `a directory without git is refused`(@TempDir tempDir: Path) {
        val directory = Files.createDirectories(tempDir.resolve("plain"))
        val error = assertThrows<RepositoryRejectedException> {
            SourceRepositories().add(directory.toString())
        }
        assertEquals(RepositoryRejectedException.Reason.NOT_A_GIT_REPOSITORY, error.reason)
    }

    @Test
    fun `a directory that contains a git marker is added in download only`(@TempDir tempDir: Path) {
        val repo = gitDirectory(tempDir, "repo")
        val initial = SourceRepositories()
        val added = initial.add(repo.toString())
        assertTrue(initial.entries().isEmpty())
        assertEquals(RepositoryMode.DOWNLOAD_ONLY, added.entries().single().mode)
        assertEquals(repo.toRealPath().toString(), added.entries().single().path)
    }

    @Test
    fun `a git file marker is accepted`(@TempDir tempDir: Path) {
        val repo = Files.createDirectories(tempDir.resolve("worktree"))
        Files.createFile(repo.resolve(".git"))
        val added = SourceRepositories().add(repo.toString())
        assertEquals(RepositoryMode.DOWNLOAD_ONLY, added.entries().single().mode)
        assertEquals(repo.toRealPath().toString(), added.entries().single().path)
    }

    @Test
    fun `mode switches to synchronisation and back without mutating the previous state`(@TempDir tempDir: Path) {
        val repo = gitDirectory(tempDir, "repo")
        val added = SourceRepositories().add(repo.toString())
        val path = added.entries().single().path
        val synced = added.setMode(path, RepositoryMode.SYNCHRONISATION)
        val back = synced.setMode(path, RepositoryMode.DOWNLOAD_ONLY)
        assertEquals(RepositoryMode.DOWNLOAD_ONLY, added.entries().single().mode)
        assertEquals(RepositoryMode.SYNCHRONISATION, synced.entries().single().mode)
        assertEquals(RepositoryMode.DOWNLOAD_ONLY, back.entries().single().mode)
    }

    @Test
    fun `several repositories stay in insertion order and a duplicate is refused`(@TempDir tempDir: Path) {
        val first = gitDirectory(tempDir, "one")
        val second = gitDirectory(tempDir, "two")
        val listed = SourceRepositories().add(first.toString()).add(second.toString())
        assertEquals(
            listOf(first.toRealPath().toString(), second.toRealPath().toString()),
            listed.entries().map { it.path },
        )
        val equivalent = first.resolve("..").resolve(first.fileName).toString()
        val error = assertThrows<RepositoryRejectedException> { listed.add(equivalent) }
        assertEquals(RepositoryRejectedException.Reason.DUPLICATE, error.reason)
        assertEquals(2, listed.entries().size)
    }

    @Test
    fun `removing a repository drops it and leaves the clone and installed files`(@TempDir tempDir: Path) {
        val clone = gitDirectory(tempDir, "clone")
        val withRepo = SourceRepositories().add(clone.toString())
        val readme = clone.resolve("README.md")
        Files.writeString(readme, "clone")
        Files.writeString(clone.resolve(".git").resolve("HEAD"), "ref: refs/heads/main\n")
        val marker = tempDir.resolve("project").resolve(".cursor/skills/specification-fr/SKILL.md")
        Files.createDirectories(marker.parent)
        Files.writeString(marker, "installed")

        val removed = withRepo.remove(withRepo.entries().single().path)
        assertTrue(removed.entries().isEmpty())
        assertEquals(1, withRepo.entries().size)
        assertEquals("clone", Files.readString(readme))
        assertEquals("installed", Files.readString(marker))
        assertEquals("ref: refs/heads/main\n", Files.readString(clone.resolve(".git").resolve("HEAD")))
        assertEquals(withRepo, withRepo.remove("not-a-repository"))
    }

    @Test
    fun `unknown repository mode change is refused`() {
        assertThrows<IllegalArgumentException> {
            SourceRepositories().setMode("missing", RepositoryMode.SYNCHRONISATION)
        }
    }

    @Test
    fun `xml writes insertion order and round trips`(@TempDir tempDir: Path) {
        val first = gitDirectory(tempDir, "one")
        val second = gitDirectory(tempDir, "two")
        val added = SourceRepositories().add(first.toString()).add(second.toString())
        val state = added.setMode(added.entries()[1].path, RepositoryMode.SYNCHRONISATION)
        val xml = state.toXml()
        assertTrue(xml.indexOf("<mode>download-only</mode>") < xml.indexOf("<mode>synchronisation</mode>"))
        assertEquals(state, SourceRepositories.fromXml(xml))
        assertEquals(xml, SourceRepositories.fromXml(xml).toXml())
    }

    @Test
    fun `path with an ampersand round trips`(@TempDir tempDir: Path) {
        val repo = gitDirectory(tempDir, "a&b")
        val state = SourceRepositories().add(repo.toString())
        val xml = state.toXml()
        assertTrue(xml.contains("&amp;"))
        assertEquals(state, SourceRepositories.fromXml(xml))
        assertEquals(repo.toRealPath().toString(), SourceRepositories.fromXml(xml).entries().single().path)
    }

    @Test
    fun `empty document restores an empty list`() {
        assertEquals(
            SourceRepositories(),
            SourceRepositories.fromXml("<sourceRepositories>\n</sourceRepositories>"),
        )
    }

    @Test
    fun `xml with an unknown mode is refused`() {
        assertThrows<IllegalArgumentException> {
            SourceRepositories.fromXml(
                "<sourceRepositories><repository><path>C:\\r</path><mode>publish</mode></repository></sourceRepositories>",
            )
        }
    }

    @Test
    fun `xml with a duplicate path is refused`() {
        val error = assertThrows<IllegalArgumentException> {
            SourceRepositories.fromXml(
                """
                <sourceRepositories>
                  <repository><path>C:\r</path><mode>download-only</mode></repository>
                  <repository><path>C:\r</path><mode>synchronisation</mode></repository>
                </sourceRepositories>
                """.trimIndent(),
            )
        }
        assertTrue(error.message!!.contains("Duplicate repository:"))
    }

    @Test
    fun `label keys are stable`() {
        assertEquals("settings.error.repository.empty", RepositoryRejectedException.Reason.EMPTY.labelKey)
        assertEquals(
            "settings.error.repository.notDirectory",
            RepositoryRejectedException.Reason.NOT_A_DIRECTORY.labelKey,
        )
        assertEquals(
            "settings.error.repository.notGit",
            RepositoryRejectedException.Reason.NOT_A_GIT_REPOSITORY.labelKey,
        )
        assertEquals("settings.error.repository.duplicate", RepositoryRejectedException.Reason.DUPLICATE.labelKey)
        assertEquals("settings.error.repository.invalidUrl", RepositoryRejectedException.Reason.INVALID_URL.labelKey)
        assertEquals("settings.error.repository.cloneFailed", RepositoryRejectedException.Reason.CLONE_FAILED.labelKey)
    }

    private class ScriptedGit(private val responder: (Path, List<String>) -> GitOutput) : GitCommand {
        val calls = mutableListOf<Pair<Path, List<String>>>()

        override fun run(repository: Path, args: List<String>): GitOutput {
            calls += repository to args
            return responder(repository, args)
        }
    }

    private fun acceptingClone(): ScriptedGit = ScriptedGit { _, _ -> GitOutput(0, "") }

    private fun gitDirectory(root: Path, name: String): Path {
        val repo = Files.createDirectories(root.resolve(name))
        Files.createDirectories(repo.resolve(".git"))
        return repo
    }
}
