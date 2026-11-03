package ai.assistant.improver.settings

import ai.assistant.improver.repository.RepositoryMode
import ai.assistant.improver.repository.SourceRepositories
import ai.assistant.improver.repository.SourceRepository
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

class SourceRepositoryBean {
    var path: String = ""
    var mode: String = RepositoryMode.DOWNLOAD_ONLY.wire
}

class SourceRepositoriesBean {
    var repositories: MutableList<SourceRepositoryBean> = mutableListOf()
}

@State(name = "AiAssistantImproverRepositories", storages = [Storage("ai-assistant-improver.xml")])
class SourceRepositoriesService : PersistentStateComponent<SourceRepositoriesBean> {
    private var repositories = SourceRepositories()

    fun current(): SourceRepositories = repositories

    fun replace(value: SourceRepositories) {
        repositories = value
    }

    override fun getState(): SourceRepositoriesBean {
        val bean = SourceRepositoriesBean()
        bean.repositories = repositories.entries().map { entry ->
            SourceRepositoryBean().apply {
                path = entry.path
                mode = entry.mode.wire
            }
        }.toMutableList()
        return bean
    }

    override fun loadState(state: SourceRepositoriesBean) {
        repositories = SourceRepositories.restore(
            state.repositories.map { bean ->
                SourceRepository(bean.path.trim(), RepositoryMode.fromWire(bean.mode.trim()))
            },
        )
    }
}
