package ai.assistant.improver.settings

import ai.assistant.improver.i18n.Labels
import ai.assistant.improver.repository.RepositoryMode
import java.awt.Component
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JTextField

class RepositorySettingsSection(
    private val settings: RepositorySettings,
    private val labelsOf: () -> Labels,
    private val isSyncing: () -> Boolean,
    private val requestSync: () -> Unit,
) {
    private val heading = JLabel()
    private val pathField = JTextField()
    private val addButton = JButton()
    private val browseButton = JButton()
    private val errorLabel = JLabel()
    private val rowPanels = mutableListOf<JPanel>()
    private val modeCombos = mutableListOf<JComboBox<RepositoryMode>>()
    private val removeButtons = mutableListOf<JButton>()

    init {
        heading.alignmentX = Component.LEFT_ALIGNMENT
        pathField.name = "repository.path"
        pathField.alignmentX = Component.LEFT_ALIGNMENT
        pathField.maximumSize = Dimension(Int.MAX_VALUE, pathField.preferredSize.height)
        addButton.name = "repository.add"
        addButton.alignmentX = Component.LEFT_ALIGNMENT
        addButton.addActionListener {
            if (!isSyncing()) {
                settings.add(pathField.text)
                if (settings.error(labelsOf()) == null) {
                    pathField.text = ""
                }
                requestSync()
            }
        }
        browseButton.name = "repository.browse"
        browseButton.alignmentX = Component.LEFT_ALIGNMENT
        errorLabel.name = "repository.error"
        errorLabel.alignmentX = Component.LEFT_ALIGNMENT
    }

    fun attach(parent: JPanel) {
        browseButton.addActionListener {
            if (!isSyncing()) {
                val chooser = JFileChooser()
                chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                    pathField.text = chooser.selectedFile.absolutePath
                }
            }
        }
        parent.add(heading)
        parent.add(pathField)
        parent.add(addButton)
        parent.add(browseButton)
        parent.add(errorLabel)
    }

    fun sync() {
        val labels = labelsOf()
        heading.text = settings.heading(labels)
        addButton.text = settings.addLabel(labels)
        browseButton.text = settings.browseLabel(labels)
        errorLabel.text = settings.error(labels) ?: ""
        rebuildRows(labels)
    }

    fun heading(): JLabel = heading

    fun pathField(): JTextField = pathField

    fun addButton(): JButton = addButton

    fun browseButton(): JButton = browseButton

    fun error(): JLabel = errorLabel

    fun rows(): List<JPanel> = rowPanels.toList()

    fun modeCombo(path: String): JComboBox<RepositoryMode> =
        modeCombos.first { it.getClientProperty(REPOSITORY_PATH) == path }

    fun removeButton(path: String): JButton =
        removeButtons.first { it.getClientProperty(REPOSITORY_PATH) == path }

    private fun rebuildRows(labels: Labels) {
        rowPanels.forEach { row ->
            row.parent?.remove(row)
        }
        rowPanels.clear()
        modeCombos.clear()
        removeButtons.clear()
        val parent = heading.parent as? JPanel ?: return
        settings.rows(labels).forEach { row ->
            val line = JPanel()
            line.layout = BoxLayout(line, BoxLayout.X_AXIS)
            line.alignmentX = Component.LEFT_ALIGNMENT
            val pathLabel = JLabel(row.path)
            val modeCombo = JComboBox(RepositoryMode.entries.toTypedArray())
            modeCombo.name = "repository.mode"
            modeCombo.putClientProperty(REPOSITORY_PATH, row.path)
            modeCombo.renderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>?,
                    value: Any?,
                    index: Int,
                    isSelected: Boolean,
                    cellHasFocus: Boolean,
                ): Component {
                    val label = (value as? RepositoryMode)?.let { settings.modeLabel(labels, it) } ?: value.toString()
                    return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus)
                }
            }
            modeCombo.addActionListener {
                val selected = modeCombo.selectedItem as? RepositoryMode
                if (!isSyncing() && selected != null) {
                    settings.setMode(row.path, selected)
                    requestSync()
                }
            }
            modeCombo.selectedItem = row.mode
            val removeButton = JButton(settings.removeLabel(labels))
            removeButton.name = "repository.remove"
            removeButton.putClientProperty(REPOSITORY_PATH, row.path)
            removeButton.addActionListener {
                if (!isSyncing()) {
                    settings.remove(row.path)
                    requestSync()
                }
            }
            line.add(pathLabel)
            line.add(modeCombo)
            line.add(removeButton)
            rowPanels += line
            modeCombos += modeCombo
            removeButtons += removeButton
            parent.add(line)
        }
        parent.revalidate()
        parent.repaint()
    }

    companion object {
        private const val REPOSITORY_PATH = "repositoryPath"
    }
}
