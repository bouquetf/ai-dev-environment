package ai.assistant.improver.settings

import ai.assistant.improver.assistant.EnabledAssistants
import ai.assistant.improver.component.ComponentSelection
import ai.assistant.improver.i18n.Labels
import ai.assistant.improver.i18n.LanguagePreference
import ai.assistant.improver.install.InstallationSnapshot
import ai.assistant.improver.install.OverwriteConfirmation
import ai.assistant.improver.publish.GitCommand
import ai.assistant.improver.publish.ProcessGit
import ai.assistant.improver.publish.PublishConfirmation
import ai.assistant.improver.publish.PublishFile
import ai.assistant.improver.repository.SourceRepositories
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import java.nio.file.Path
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTextField

class AssistantSettingsConfigurable(
    private val assistantsStore: AssistantStore,
    private val languageStore: LanguageStore,
    private val repositoryStore: RepositoryStore,
    private val selectionStore: ComponentSelectionStore,
    private val snapshotStore: InstallationSnapshotStore,
    private val confirmOverwrite: OverwriteConfirmation = OverwriteConfirmation { false },
    private val projectRoot: () -> Path = { error("No project directory") },
    private val dialogConfirmation: Boolean = false,
    private val confirmPublish: PublishConfirmation = PublishConfirmation { null },
    private val git: GitCommand = ProcessGit,
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
            git = git,
        )
        model = created
        val confirm = if (dialogConfirmation) dialogConfirm(created) else confirmOverwrite
        val publish = if (dialogConfirmation) dialogPublish(created) else confirmPublish
        return SettingsPanel(
            created,
            projectRoot,
            confirm,
            onImported = { snapshotStore.write(it) },
            publish,
            onPublished = { snapshotStore.write(it) },
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

    private fun dialogPublish(model: SettingsViewModel): PublishConfirmation = PublishConfirmation { files ->
        val labels = model.labels()
        val message = JTextField(labels.text("settings.publish.defaultMessage"))
        message.name = "publish.message"
        val content = JPanel()
        content.layout = BoxLayout(content, BoxLayout.Y_AXIS)
        content.add(JLabel(labels.text("settings.publish.intro")))
        fileLines(files).forEach { content.add(JLabel(it)) }
        content.add(JLabel(labels.text("settings.publish.message")))
        content.add(message)
        val choice = JOptionPane.showConfirmDialog(
            panel,
            content,
            labels.text("settings.publish.confirmTitle"),
            JOptionPane.YES_NO_OPTION,
        )
        if (choice == JOptionPane.YES_OPTION) message.text else null
    }

    private fun fileLines(files: List<PublishFile>): List<String> {
        val lines = mutableListOf<String>()
        var repository: String? = null
        files.forEach { file ->
            if (file.repositoryPath != repository) {
                lines += file.repositoryPath
                repository = file.repositoryPath
            }
            lines += "${file.destinationPath} -> ${file.sourcePath}"
        }
        return lines
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
