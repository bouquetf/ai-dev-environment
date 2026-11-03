package ai.assistant.improver.settings

import ai.assistant.improver.assistant.AssistantCatalog
import ai.assistant.improver.assistant.AssistantId
import ai.assistant.improver.assistant.EnabledAssistants
import ai.assistant.improver.i18n.Labels
import ai.assistant.improver.i18n.LanguagePreference
import ai.assistant.improver.i18n.UiLanguage

data class AssistantRow(val id: AssistantId, val label: String, val selected: Boolean)

data class LanguageChoice(val language: UiLanguage, val label: String, val selected: Boolean)

class SettingsViewModel(
    assistants: EnabledAssistants,
    language: LanguagePreference,
    private val labelsFor: (UiLanguage) -> Labels = { Labels.load(it) },
) {
    private var assistants: EnabledAssistants = assistants
    private var language: LanguagePreference = language

    private val labels: Labels
        get() = labelsFor(language.language)

    val pageTitle: String
        get() = labels.text("settings.page.title")

    val assistantsHeading: String
        get() = labels.text("settings.assistants.heading")

    val languageHeading: String
        get() = labels.text("settings.language.heading")

    val assistantRows: List<AssistantRow>
        get() {
            val current = labels
            val enabled = assistants.enabledIds()
            return AssistantCatalog.assistants.map {
                AssistantRow(it, current.text("settings.assistants.${it.wire}"), it in enabled)
            }
        }

    val languageChoices: List<LanguageChoice>
        get() {
            val current = labels
            return UiLanguage.entries.map {
                LanguageChoice(it, current.text("settings.language.${it.code}"), it == language.language)
            }
        }

    fun enableAssistant(id: AssistantId) {
        assistants = assistants.enable(id)
    }

    fun disableAssistant(id: AssistantId) {
        assistants = assistants.disable(id)
    }

    fun selectLanguage(language: UiLanguage) {
        this.language = this.language.withLanguage(language)
    }

    fun reload(assistants: EnabledAssistants, language: LanguagePreference) {
        this.assistants = assistants
        this.language = language
    }

    fun assistants(): EnabledAssistants = assistants

    fun language(): LanguagePreference = language
}
