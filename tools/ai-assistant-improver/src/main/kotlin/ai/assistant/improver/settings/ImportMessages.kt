package ai.assistant.improver.settings

import ai.assistant.improver.i18n.Labels
import ai.assistant.improver.install.ImportNotice
import ai.assistant.improver.install.ImportNoticeKind

object ImportMessages {
    fun confirm(labels: Labels, destinationPath: String): String =
        labels.text("settings.import.confirm", destinationPath)

    fun noTarget(labels: Labels, assistantLabel: String, componentPath: String): String =
        labels.text("settings.import.noTarget", assistantLabel, componentPath)

    fun kept(labels: Labels, destinationPath: String): String =
        labels.text("settings.import.kept", destinationPath)

    fun notice(labels: Labels, notice: ImportNotice): String = when (notice.kind) {
        ImportNoticeKind.CLONE_FAILED -> labels.text("settings.import.cloneFailed", notice.repositoryPath)
        ImportNoticeKind.FETCH_FAILED -> labels.text("settings.import.fetchFailed", notice.repositoryPath)
        ImportNoticeKind.KEPT_TREE -> labels.text("settings.import.keptTree", notice.repositoryPath)
    }
}
