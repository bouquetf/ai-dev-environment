package ai.assistant.improver.settings

import ai.assistant.improver.i18n.UiLanguage
import java.awt.Component
import javax.swing.BoxLayout
import javax.swing.DefaultListCellRenderer
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel

class SettingsPanel(private val model: SettingsViewModel) : JPanel() {
    private val assistantsHeading = JLabel()
    private val languageHeading = JLabel()
    private val checks: List<JCheckBox>
    private val combo = JComboBox(UiLanguage.entries.toTypedArray())
    private var syncing = false

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
        syncFromModel()
    }

    fun assistantChecks(): List<JCheckBox> = checks

    fun assistantsHeading(): JLabel = assistantsHeading

    fun languageHeading(): JLabel = languageHeading

    fun languageCombo(): JComboBox<UiLanguage> = combo

    fun languageLabel(language: UiLanguage): String =
        model.languageChoices.first { it.language == language }.label

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
        } finally {
            syncing = false
        }
    }
}
