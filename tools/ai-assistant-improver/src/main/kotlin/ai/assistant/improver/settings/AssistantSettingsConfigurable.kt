package ai.assistant.improver.settings

import ai.assistant.improver.assistant.EnabledAssistants
import ai.assistant.improver.i18n.Labels
import ai.assistant.improver.i18n.LanguagePreference
import ai.assistant.improver.repository.SourceRepositories
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class AssistantSettingsConfigurable(
    private val assistantsStore: AssistantStore,
    private val languageStore: LanguageStore,
    private val repositoryStore: RepositoryStore,
) : Configurable {
    constructor(project: Project) : this(
        ProjectAssistantStore(project),
        ApplicationLanguageStore(),
        ProjectRepositoryStore(project),
    )

    private class Baseline(
        val assistants: EnabledAssistants,
        val language: LanguagePreference,
        val repositories: SourceRepositories,
    ) {
        fun sameAs(model: SettingsViewModel): Boolean =
            model.assistants() == assistants &&
                model.language() == language &&
                model.repositories() == repositories
    }

    private var model: SettingsViewModel? = null
    private var panel: SettingsPanel? = null
    private var baseline = Baseline(EnabledAssistants(), LanguagePreference(), SourceRepositories())

    override fun getDisplayName(): String = Labels.load(languageStore.read().language).text("settings.page.title")

    override fun createComponent(): JComponent {
        baseline = Baseline(assistantsStore.read(), languageStore.read(), repositoryStore.read())
        val created = SettingsViewModel(baseline.assistants, baseline.language, baseline.repositories)
        model = created
        return SettingsPanel(created).also { panel = it }
    }

    override fun isModified(): Boolean {
        val current = model ?: return false
        return !baseline.sameAs(current)
    }

    override fun apply() {
        val current = model ?: return
        assistantsStore.write(current.assistants())
        languageStore.write(current.language())
        repositoryStore.write(current.repositories())
        baseline = Baseline(current.assistants(), current.language(), current.repositories())
    }

    override fun reset() {
        val current = model ?: return
        baseline = Baseline(assistantsStore.read(), languageStore.read(), repositoryStore.read())
        current.reload(baseline.assistants, baseline.language, baseline.repositories)
        panel?.syncFromModel()
    }

    override fun disposeUIResources() {
        panel = null
        model = null
    }
}
