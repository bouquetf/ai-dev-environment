package ai.assistant.improver.settings

import ai.assistant.improver.assistant.AssistantCatalog
import ai.assistant.improver.assistant.AssistantId
import ai.assistant.improver.assistant.EnabledAssistants
import ai.assistant.improver.component.ComponentDiscovery
import ai.assistant.improver.component.ComponentId
import ai.assistant.improver.component.ComponentSelection
import ai.assistant.improver.i18n.Labels
import ai.assistant.improver.i18n.LanguagePreference
import ai.assistant.improver.i18n.UiLanguage
import ai.assistant.improver.install.ComponentImport
import ai.assistant.improver.install.ImportReport
import ai.assistant.improver.install.InstallationSnapshot
import ai.assistant.improver.install.OverwriteConfirmation
import ai.assistant.improver.repository.SourceRepositories
import java.nio.file.Path

data class AssistantRow(val id: AssistantId, val label: String, val selected: Boolean)

data class LanguageChoice(val language: UiLanguage, val label: String, val selected: Boolean)

data class ComponentRow(
    val repositoryPath: String,
    val id: ComponentId,
    val label: String,
    val selected: Boolean,
)

class SettingsViewModel(
    assistants: EnabledAssistants,
    language: LanguagePreference,
    repositories: SourceRepositories = SourceRepositories(),
    selection: ComponentSelection = ComponentSelection(),
    snapshot: InstallationSnapshot = InstallationSnapshot(),
    private val discover: (Path) -> List<ComponentId> = { ComponentDiscovery.discover(it) },
    private val labelsFor: (UiLanguage) -> Labels = { Labels.load(it) },
) {
    private var assistants: EnabledAssistants = assistants
    private var language: LanguagePreference = language
    private var selection: ComponentSelection = selection
    private var snapshot: InstallationSnapshot = snapshot
    val repositorySettings: RepositorySettings = RepositorySettings(repositories)

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

    val componentRows: List<ComponentRow>
        get() {
            val current = labels
            return repositorySettings.current().entries().flatMap { repository ->
                val discovered = discover(Path.of(repository.path))
                selection.components(repository.path, discovered).map { component ->
                    val kindLabel = current.text("settings.components.${component.id.kind.wire}")
                    ComponentRow(
                        repository.path,
                        component.id,
                        "$kindLabel ${component.id.relativePath}",
                        component.selected,
                    )
                }
            }
        }

    fun setComponentSelected(repositoryPath: String, id: ComponentId, selected: Boolean) {
        selection = selection.setSelected(repositoryPath, id, selected)
    }

    fun labels(): Labels = labels

    fun importSelected(projectRoot: Path, confirm: OverwriteConfirmation): ImportReport {
        val report = ComponentImport.import(
            projectRoot,
            repositories(),
            selection,
            assistants,
            snapshot,
            confirm,
            discover,
        )
        snapshot = report.snapshot
        return report
    }

    fun reload(
        assistants: EnabledAssistants,
        language: LanguagePreference,
        repositories: SourceRepositories,
        selection: ComponentSelection,
        snapshot: InstallationSnapshot,
    ) {
        this.assistants = assistants
        this.language = language
        this.selection = selection
        this.snapshot = snapshot
        repositorySettings.replace(repositories)
    }

    fun assistants(): EnabledAssistants = assistants

    fun language(): LanguagePreference = language

    fun repositories(): SourceRepositories = repositorySettings.current()

    fun selection(): ComponentSelection = selection

    fun snapshot(): InstallationSnapshot = snapshot
}
