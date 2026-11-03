package ai.assistant.improver.settings

import ai.assistant.improver.i18n.Labels
import ai.assistant.improver.repository.RepositoryMode
import ai.assistant.improver.repository.RepositoryRejectedException
import ai.assistant.improver.repository.SourceRepositories

data class RepositoryRow(val path: String, val mode: RepositoryMode, val modeLabel: String)

class RepositorySettings(repositories: SourceRepositories = SourceRepositories()) {
    private var repositories: SourceRepositories = repositories
    private var rejection: RepositoryRejectedException? = null

    fun current(): SourceRepositories = repositories

    fun replace(value: SourceRepositories) {
        repositories = value
        rejection = null
    }

    fun heading(labels: Labels): String = labels.text("settings.repositories.heading")

    fun addLabel(labels: Labels): String = labels.text("settings.repositories.add")

    fun browseLabel(labels: Labels): String = labels.text("settings.repositories.browse")

    fun removeLabel(labels: Labels): String = labels.text("settings.repositories.remove")

    fun modeLabel(labels: Labels, mode: RepositoryMode): String =
        labels.text("settings.repositories.mode.${mode.wire}")

    fun rows(labels: Labels): List<RepositoryRow> =
        repositories.entries().map { RepositoryRow(it.path, it.mode, modeLabel(labels, it.mode)) }

    fun error(labels: Labels): String? {
        val current = rejection ?: return null
        return if (current.reason == RepositoryRejectedException.Reason.EMPTY) {
            labels.text(current.reason.labelKey)
        } else {
            labels.text(current.reason.labelKey, current.path)
        }
    }

    fun add(rawPath: String) {
        try {
            repositories = repositories.add(rawPath)
            rejection = null
        } catch (error: RepositoryRejectedException) {
            rejection = error
        }
    }

    fun setMode(path: String, mode: RepositoryMode) {
        repositories = repositories.setMode(path, mode)
        rejection = null
    }

    fun remove(path: String) {
        repositories = repositories.remove(path)
        rejection = null
    }
}
