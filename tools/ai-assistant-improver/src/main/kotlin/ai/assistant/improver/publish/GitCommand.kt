package ai.assistant.improver.publish

import java.nio.charset.StandardCharsets
import java.nio.file.Path

fun interface GitCommand {
    fun run(repository: Path, args: List<String>): GitOutput
}

data class GitOutput(val exitCode: Int, val output: String)

object ProcessGit : GitCommand {
    override fun run(repository: Path, args: List<String>): GitOutput {
        val process = ProcessBuilder(listOf("git") + args)
            .directory(repository.toFile())
            .redirectErrorStream(true)
            .also { it.environment()["GIT_TERMINAL_PROMPT"] = "0" }
            .start()
        val output = process.inputStream.readBytes().toString(StandardCharsets.UTF_8)
        return GitOutput(process.waitFor(), output)
    }
}
