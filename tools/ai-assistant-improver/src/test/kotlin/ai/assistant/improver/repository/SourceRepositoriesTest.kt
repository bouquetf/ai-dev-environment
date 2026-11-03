package ai.assistant.improver.repository

import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `a url is refused and not added`() {
        val error = assertThrows<RepositoryRejectedException> {
            SourceRepositories().add("https://example.com/repo.git")
        }
        assertEquals(RepositoryRejectedException.Reason.NOT_A_DIRECTORY, error.reason)
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
    }

    private fun gitDirectory(root: Path, name: String): Path {
        val repo = Files.createDirectories(root.resolve(name))
        Files.createDirectories(repo.resolve(".git"))
        return repo
    }
}
