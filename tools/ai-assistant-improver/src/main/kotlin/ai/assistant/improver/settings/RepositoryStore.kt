package ai.assistant.improver.settings

import ai.assistant.improver.repository.SourceRepositories

interface RepositoryStore {
    fun read(): SourceRepositories
    fun write(value: SourceRepositories)
}
