package ai.assistant.improver.publish

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.Locale

fun interface GitCommand {
    fun run(repository: Path, args: List<String>): GitOutput
}

data class GitOutput(val exitCode: Int, val output: String)

object ProcessGit : GitCommand {
    override fun run(repository: Path, args: List<String>): GitOutput =
        invoke(repository, args, emptyMap())

    internal fun invoke(
        repository: Path,
        args: List<String>,
        extraEnvironment: Map<String, String>,
    ): GitOutput {
        val process = ProcessBuilder(listOf("git") + args)
            .directory(repository.toFile())
            .redirectErrorStream(true)
            .also { builder ->
                extraEnvironment.forEach { (key, value) -> builder.environment()[key] = value }
                builder.environment()["GIT_TERMINAL_PROMPT"] = "0"
                allowLongPaths(builder)
            }
            .start()
        val output = process.inputStream.readBytes().toString(StandardCharsets.UTF_8)
        return GitOutput(process.waitFor(), output)
    }

    private fun allowLongPaths(builder: ProcessBuilder) {
        if (!WINDOWS) return
        val environment = builder.environment()
        val setting = "'core.longpaths=true'"
        val current = environment["GIT_CONFIG_PARAMETERS"]
        environment["GIT_CONFIG_PARAMETERS"] = when {
            current.isNullOrBlank() -> setting
            current.contains(setting) -> current
            else -> "$current $setting"
        }
    }

    private val WINDOWS = System.getProperty("os.name").lowercase(Locale.ROOT).contains("windows")
}
