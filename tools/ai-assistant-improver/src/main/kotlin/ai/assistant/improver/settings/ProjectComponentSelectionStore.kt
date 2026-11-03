package ai.assistant.improver.settings

import ai.assistant.improver.component.ComponentSelection
import com.intellij.openapi.project.Project

class ProjectComponentSelectionStore(private val project: Project) : ComponentSelectionStore {
    private val service: ComponentSelectionService
        get() = project.getService(ComponentSelectionService::class.java)

    override fun read(): ComponentSelection = service.current()

    override fun write(value: ComponentSelection) {
        service.replace(value)
    }
}
