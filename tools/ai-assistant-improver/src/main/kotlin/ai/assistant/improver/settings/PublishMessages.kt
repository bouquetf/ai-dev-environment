package ai.assistant.improver.settings

import ai.assistant.improver.i18n.Labels
import ai.assistant.improver.publish.PublishFailure
import ai.assistant.improver.publish.PublishFailureStatus

object PublishMessages {
    fun done(labels: Labels, repositoryPath: String): String =
        labels.text("settings.publish.done", repositoryPath)

    fun cancelled(labels: Labels): String = labels.text("settings.publish.cancelled")

    fun nothing(labels: Labels): String = labels.text("settings.publish.nothing")

    fun failure(labels: Labels, failure: PublishFailure): String = when (failure.status) {
        PublishFailureStatus.DIVERGENCE -> labels.text("settings.publish.divergence", failure.repositoryPath)
        PublishFailureStatus.NO_ORIGIN -> labels.text("settings.publish.noOrigin", failure.repositoryPath)
        PublishFailureStatus.PUSH_FAILED -> labels.text("settings.publish.pushFailed", failure.repositoryPath)
        PublishFailureStatus.COMMIT_FAILED -> labels.text("settings.publish.commitFailed", failure.repositoryPath)
        PublishFailureStatus.DETACHED -> labels.text("settings.publish.detached", failure.repositoryPath)
        PublishFailureStatus.EMPTY_MESSAGE -> labels.text("settings.publish.emptyMessage")
    }
}
