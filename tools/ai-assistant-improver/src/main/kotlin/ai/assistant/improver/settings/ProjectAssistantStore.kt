package ai.assistant.improver.settings

import ai.assistant.improver.assistant.EnabledAssistants
import com.intellij.openapi.project.Project

class ProjectAssistantStore(private val project: Project) : AssistantStore {
    private val service: EnabledAssistantsService
        get() = project.getService(EnabledAssistantsService::class.java)

    override fun read(): EnabledAssistants = service.current()

    override fun write(value: EnabledAssistants) {
        service.replace(value)
    }
}
