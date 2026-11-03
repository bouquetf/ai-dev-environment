package ai.assistant.improver.settings

import ai.assistant.improver.i18n.Labels

object ImportMessages {
    fun confirm(labels: Labels, destinationPath: String): String =
        labels.text("settings.import.confirm", destinationPath)

    fun noTarget(labels: Labels, assistantLabel: String, componentPath: String): String =
        labels.text("settings.import.noTarget", assistantLabel, componentPath)

    fun kept(labels: Labels, destinationPath: String): String =
        labels.text("settings.import.kept", destinationPath)
}
