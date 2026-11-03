package ai.assistant.improver.settings

import ai.assistant.improver.repository.SourceRepositories
import com.intellij.openapi.project.Project

class ProjectRepositoryStore(private val project: Project) : RepositoryStore {
    private val service: SourceRepositoriesService
        get() = project.getService(SourceRepositoriesService::class.java)

    override fun read(): SourceRepositories = service.current()

    override fun write(value: SourceRepositories) {
        service.replace(value)
    }
}
