package ai.assistant.improver.settings

import ai.assistant.improver.assistant.EnabledAssistants
import ai.assistant.improver.i18n.Labels
import ai.assistant.improver.i18n.LanguagePreference
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class AssistantSettingsConfigurable(
    private val assistantsStore: AssistantStore,
    private val languageStore: LanguageStore,
) : Configurable {
    constructor(project: Project) : this(ProjectAssistantStore(project), ApplicationLanguageStore())

    private var model: SettingsViewModel? = null
    private var panel: SettingsPanel? = null
    private var baseAssistants: EnabledAssistants = EnabledAssistants()
    private var baseLanguage: LanguagePreference = LanguagePreference()

    override fun getDisplayName(): String = Labels.load(languageStore.read().language).text("settings.page.title")

    override fun createComponent(): JComponent {
        baseAssistants = assistantsStore.read()
        baseLanguage = languageStore.read()
        val created = SettingsViewModel(baseAssistants, baseLanguage)
        model = created
        return SettingsPanel(created).also { panel = it }
    }

    override fun isModified(): Boolean {
        val current = model ?: return false
        return current.assistants() != baseAssistants || current.language() != baseLanguage
    }

    override fun apply() {
        val current = model ?: return
        assistantsStore.write(current.assistants())
        languageStore.write(current.language())
        baseAssistants = current.assistants()
        baseLanguage = current.language()
    }

    override fun reset() {
        val current = model ?: return
        baseAssistants = assistantsStore.read()
        baseLanguage = languageStore.read()
        current.reload(baseAssistants, baseLanguage)
        panel?.syncFromModel()
    }

    override fun disposeUIResources() {
        panel = null
        model = null
    }
}
