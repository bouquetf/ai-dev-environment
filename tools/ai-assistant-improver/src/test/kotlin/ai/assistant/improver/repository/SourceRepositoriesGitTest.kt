package ai.assistant.improver.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class SourceRepositoriesGitTest {
    @Test
    fun `a directory without git is refused after a real directory check`(@TempDir tempDir: Path) {
        val directory = Files.createDirectories(tempDir.resolve("plain"))
        val error = assertThrows<RepositoryRejectedException> { SourceRepositories().add(directory.toString()) }
        assertEquals(RepositoryRejectedException.Reason.NOT_A_GIT_REPOSITORY, error.reason)
    }

    @Test
    fun `a directory after git init is accepted in download only`(@TempDir tempDir: Path) {
        val directory = Files.createDirectories(tempDir.resolve("repo"))
        val process = ProcessBuilder(listOf("git", "init"))
            .directory(directory.toFile())
            .redirectErrorStream(true)
            .also { it.environment()["GIT_TERMINAL_PROMPT"] = "0" }
            .start()
        val output = process.inputStream.readBytes().toString(StandardCharsets.UTF_8)
        assertEquals(0, process.waitFor(), output)
        val added = SourceRepositories().add(directory.toString())
        assertEquals(RepositoryMode.DOWNLOAD_ONLY, added.entries().single().mode)
        assertEquals(directory.toRealPath().toString(), added.entries().single().path)
    }
}
