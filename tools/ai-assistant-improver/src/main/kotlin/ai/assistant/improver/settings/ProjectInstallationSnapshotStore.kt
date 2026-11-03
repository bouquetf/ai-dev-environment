package ai.assistant.improver.settings

import ai.assistant.improver.install.InstallationSnapshot
import com.intellij.openapi.project.Project

class ProjectInstallationSnapshotStore(private val project: Project) : InstallationSnapshotStore {
    private val service: InstallationSnapshotService
        get() = project.getService(InstallationSnapshotService::class.java)

    override fun read(): InstallationSnapshot = service.current()

    override fun write(value: InstallationSnapshot) {
        service.replace(value)
    }
}
