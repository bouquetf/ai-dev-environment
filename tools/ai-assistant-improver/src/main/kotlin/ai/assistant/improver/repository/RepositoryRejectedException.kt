package ai.assistant.improver.repository

class RepositoryRejectedException(
    val reason: Reason,
    val path: String,
) : IllegalArgumentException("$reason: $path") {
    enum class Reason {
        EMPTY,
        NOT_A_DIRECTORY,
        NOT_A_GIT_REPOSITORY,
        DUPLICATE,
        INVALID_URL,
        CLONE_FAILED;

        val labelKey: String
            get() = when (this) {
                EMPTY -> "settings.error.repository.empty"
                NOT_A_DIRECTORY -> "settings.error.repository.notDirectory"
                NOT_A_GIT_REPOSITORY -> "settings.error.repository.notGit"
                DUPLICATE -> "settings.error.repository.duplicate"
                INVALID_URL -> "settings.error.repository.invalidUrl"
                CLONE_FAILED -> "settings.error.repository.cloneFailed"
            }
    }
}
