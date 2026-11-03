package ai.assistant.improver.settings

import ai.assistant.improver.i18n.UiLanguage
import ai.assistant.improver.repository.RepositoryMode
import java.awt.Component
import javax.swing.BoxLayout
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JTextField

class SettingsPanel(private val model: SettingsViewModel) : JPanel() {
    private val assistantsHeading = JLabel()
    private val languageHeading = JLabel()
    private val checks: List<JCheckBox>
    private val combo = JComboBox(UiLanguage.entries.toTypedArray())
    private var syncing = false
    private val componentChecks = mutableListOf<JCheckBox>()
    private val repositorySection = RepositorySettingsSection(
        settings = model.repositorySettings,
        labelsOf = { model.labels() },
        isSyncing = { syncing },
        requestSync = { syncFromModel() },
    )

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        val settings = model
        add(assistantsHeading)
        checks = settings.assistantRows.map { row ->
            JCheckBox().apply {
                name = row.id.wire
                alignmentX = Component.LEFT_ALIGNMENT
                addActionListener {
                    if (!syncing) {
                        if (isSelected) settings.enableAssistant(row.id) else settings.disableAssistant(row.id)
                    }
                }
            }
        }
        checks.forEach { add(it) }
        add(languageHeading)
        combo.alignmentX = Component.LEFT_ALIGNMENT
        combo.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean,
            ): Component {
                val label = model.languageChoices.firstOrNull { it.language == value }?.label ?: value.toString()
                return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus)
            }
        }
        combo.addActionListener {
            val selected = combo.selectedItem as? UiLanguage
            if (!syncing && selected != null) {
                model.selectLanguage(selected)
                syncFromModel()
            }
        }
        add(combo)
        repositorySection.attach(this)
        syncFromModel()
    }

    fun assistantChecks(): List<JCheckBox> = checks

    fun assistantsHeading(): JLabel = assistantsHeading

    fun languageHeading(): JLabel = languageHeading

    fun languageCombo(): JComboBox<UiLanguage> = combo

    fun languageLabel(language: UiLanguage): String =
        model.languageChoices.first { it.language == language }.label

    fun repositoryHeading(): JLabel = repositorySection.heading()

    fun repositoryPathField(): JTextField = repositorySection.pathField()

    fun addButton(): JButton = repositorySection.addButton()

    fun browseButton(): JButton = repositorySection.browseButton()

    fun repositoryError(): JLabel = repositorySection.error()

    fun repositoryRows(): List<JPanel> = repositorySection.rows()

    fun modeCombo(path: String): JComboBox<RepositoryMode> = repositorySection.modeCombo(path)

    fun removeButton(path: String): JButton = repositorySection.removeButton(path)

    fun componentChecks(repositoryPath: String): List<JCheckBox> =
        componentChecks.filter { it.getClientProperty(COMPONENT_REPOSITORY) == repositoryPath }

    fun syncFromModel() {
        syncing = true
        try {
            assistantsHeading.text = model.assistantsHeading
            languageHeading.text = model.languageHeading
            val rows = model.assistantRows.associateBy { it.id.wire }
            checks.forEach { check ->
                val row = rows.getValue(check.name)
                check.text = row.label
                check.isSelected = row.selected
            }
            combo.selectedItem = model.language().language
            combo.repaint()
            repositorySection.sync()
            rebuildComponentChecks()
        } finally {
            syncing = false
        }
    }

    private fun rebuildComponentChecks() {
        componentChecks.forEach { check -> check.parent?.remove(check) }
        componentChecks.clear()
        val grouped = model.componentRows.groupBy { it.repositoryPath }
        model.repositories().entries().zip(repositorySection.rows()).forEach { (repository, row) ->
            val parent = row.parent as? JPanel ?: return@forEach
            val insertAt = parent.components.indexOf(row) + 1
            grouped[repository.path].orEmpty().forEachIndexed { index, line ->
                val check = JCheckBox(line.label)
                check.name = "component.check"
                check.alignmentX = Component.LEFT_ALIGNMENT
                check.putClientProperty(COMPONENT_REPOSITORY, line.repositoryPath)
                check.putClientProperty(COMPONENT_WIRE, "${line.id.kind.wire}:${line.id.relativePath}")
                check.isSelected = line.selected
                check.addActionListener {
                    if (!syncing) {
                        model.setComponentSelected(line.repositoryPath, line.id, check.isSelected)
                        syncFromModel()
                    }
                }
                parent.add(check, insertAt + index)
                componentChecks += check
            }
        }
        revalidate()
        repaint()
    }

    companion object {
        private const val COMPONENT_REPOSITORY = "componentRepository"
        private const val COMPONENT_WIRE = "componentWire"
    }
}
