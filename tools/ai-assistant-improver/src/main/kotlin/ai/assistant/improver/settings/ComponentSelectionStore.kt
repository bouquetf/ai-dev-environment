package ai.assistant.improver.settings

import ai.assistant.improver.component.ComponentSelection

interface ComponentSelectionStore {
    fun read(): ComponentSelection
    fun write(value: ComponentSelection)
}
