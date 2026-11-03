package ai.assistant.improver.settings

import ai.assistant.improver.install.InstallationSnapshot

interface InstallationSnapshotStore {
    fun read(): InstallationSnapshot
    fun write(value: InstallationSnapshot)
}
