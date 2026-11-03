package ai.assistant.improver.settings

import ai.assistant.improver.component.ComponentId
import ai.assistant.improver.component.ComponentKind
import ai.assistant.improver.component.ComponentSelection
import ai.assistant.improver.component.DeselectedComponent
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

class DeselectedComponentBean {
    var repository: String = ""
    var kind: String = ""
    var path: String = ""
}

class ComponentSelectionBean {
    var deselected: MutableList<DeselectedComponentBean> = mutableListOf()
}

@State(name = "AiAssistantImproverComponents", storages = [Storage("ai-assistant-improver.xml")])
class ComponentSelectionService : PersistentStateComponent<ComponentSelectionBean> {
    private var selection = ComponentSelection()

    fun current(): ComponentSelection = selection

    fun replace(value: ComponentSelection) {
        selection = value
    }

    override fun getState(): ComponentSelectionBean {
        val bean = ComponentSelectionBean()
        bean.deselected = selection.entries().map { entry ->
            DeselectedComponentBean().apply {
                repository = entry.repositoryPath
                kind = entry.id.kind.wire
                path = entry.id.relativePath
            }
        }.toMutableList()
        return bean
    }

    override fun loadState(state: ComponentSelectionBean) {
        selection = ComponentSelection.restore(
            state.deselected.map { bean ->
                DeselectedComponent(
                    bean.repository.trim(),
                    ComponentId(ComponentKind.fromWire(bean.kind.trim()), bean.path.trim()),
                )
            },
        )
    }
}
