package ai.assistant.improver.settings

import ai.assistant.improver.component.ComponentId
import ai.assistant.improver.component.ComponentKind
import ai.assistant.improver.install.InstallationSnapshot
import ai.assistant.improver.install.InstalledFile
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

class InstalledFileBean {
    var repository: String = ""
    var kind: String = ""
    var component: String = ""
    var source: String = ""
    var destination: String = ""
    var sha256: String = ""
}

class InstallationSnapshotBean {
    var files: MutableList<InstalledFileBean> = mutableListOf()
}

@State(name = "AiAssistantImproverSnapshot", storages = [Storage("ai-assistant-improver.xml")])
class InstallationSnapshotService : PersistentStateComponent<InstallationSnapshotBean> {
    private var snapshot = InstallationSnapshot()

    fun current(): InstallationSnapshot = snapshot

    fun replace(value: InstallationSnapshot) {
        snapshot = value
    }

    override fun getState(): InstallationSnapshotBean {
        val bean = InstallationSnapshotBean()
        bean.files = snapshot.files().map { file ->
            InstalledFileBean().apply {
                repository = file.repositoryPath
                kind = file.component.kind.wire
                component = file.component.relativePath
                source = file.sourcePath
                destination = file.destinationPath
                sha256 = file.sha256
            }
        }.toMutableList()
        return bean
    }

    override fun loadState(state: InstallationSnapshotBean) {
        snapshot = InstallationSnapshot.restore(
            state.files.map { bean ->
                InstalledFile(
                    bean.repository.trim(),
                    ComponentId(ComponentKind.fromWire(bean.kind.trim()), bean.component.trim()),
                    bean.source.trim(),
                    bean.destination.trim(),
                    bean.sha256.trim(),
                )
            },
        )
    }
}
