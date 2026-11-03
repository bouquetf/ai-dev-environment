package ai.assistant.improver.settings

import ai.assistant.improver.assistant.EnabledAssistants
import ai.assistant.improver.component.ComponentSelection
import ai.assistant.improver.i18n.Labels
import ai.assistant.improver.i18n.LanguagePreference
import ai.assistant.improver.install.InstallationSnapshot
import ai.assistant.improver.install.OverwriteConfirmation
import ai.assistant.improver.repository.SourceRepositories
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import java.nio.file.Path
import javax.swing.JComponent
import javax.swing.JOptionPane

class AssistantSettingsConfigurable(
    private val assistantsStore: AssistantStore,
    private val languageStore: LanguageStore,
    private val repositoryStore: RepositoryStore,
    private val selectionStore: ComponentSelectionStore,
    private val snapshotStore: InstallationSnapshotStore,
    private val confirmOverwrite: OverwriteConfirmation = OverwriteConfirmation { false },
    private val projectRoot: () -> Path = { error("No project directory") },
    private val dialogConfirmation: Boolean = false,
) : Configurable {
    constructor(project: Project) : this(
        ProjectAssistantStore(project),
        ApplicationLanguageStore(),
        ProjectRepositoryStore(project),
        ProjectComponentSelectionStore(project),
        ProjectInstallationSnapshotStore(project),
        projectRoot = { Path.of(requireNotNull(project.basePath) { "Project has no base path" }) },
        dialogConfirmation = true,
    )

    private class Baseline(
        val assistants: EnabledAssistants,
        val language: LanguagePreference,
        val repositories: SourceRepositories,
        val selection: ComponentSelection,
    ) {
        fun sameAs(model: SettingsViewModel): Boolean =
            model.assistants() == assistants &&
                model.language() == language &&
                model.repositories() == repositories &&
                model.selection() == selection
    }

    private var model: SettingsViewModel? = null
    private var panel: SettingsPanel? = null
    private var baseline = Baseline(
        EnabledAssistants(),
        LanguagePreference(),
        SourceRepositories(),
        ComponentSelection(),
    )

    override fun getDisplayName(): String = Labels.load(languageStore.read().language).text("settings.page.title")

    override fun createComponent(): JComponent {
        baseline = storedBaseline()
        val created = SettingsViewModel(
            baseline.assistants,
            baseline.language,
            baseline.repositories,
            baseline.selection,
            snapshotStore.read(),
        )
        model = created
        val confirm = if (dialogConfirmation) dialogConfirm(created) else confirmOverwrite
        return SettingsPanel(
            created,
            projectRoot,
            confirm,
            onImported = { snapshotStore.write(it) },
        ).also { panel = it }
    }

    private fun dialogConfirm(model: SettingsViewModel): OverwriteConfirmation = OverwriteConfirmation { prompt ->
        val labels = model.labels()
        JOptionPane.showConfirmDialog(
            panel,
            ImportMessages.confirm(labels, prompt.destinationPath),
            labels.text("settings.import.confirmTitle"),
            JOptionPane.YES_NO_OPTION,
        ) == JOptionPane.YES_OPTION
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
        selectionStore.write(current.selection())
        baseline = Baseline(
            current.assistants(),
            current.language(),
            current.repositories(),
            current.selection(),
        )
    }

    override fun reset() {
        val current = model ?: return
        baseline = storedBaseline()
        current.reload(
            baseline.assistants,
            baseline.language,
            baseline.repositories,
            baseline.selection,
            snapshotStore.read(),
        )
        panel?.syncFromModel()
    }

    private fun storedBaseline(): Baseline = Baseline(
        assistantsStore.read(),
        languageStore.read(),
        repositoryStore.read(),
        selectionStore.read(),
    )

    override fun disposeUIResources() {
        panel = null
        model = null
    }
}
