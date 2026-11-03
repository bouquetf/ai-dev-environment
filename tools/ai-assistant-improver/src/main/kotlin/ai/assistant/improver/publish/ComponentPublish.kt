package ai.assistant.improver.publish

import ai.assistant.improver.component.ComponentId
import ai.assistant.improver.install.ContentDigest
import ai.assistant.improver.install.InstallationSnapshot
import ai.assistant.improver.install.InstalledFile
import ai.assistant.improver.repository.RepositoryMode
import ai.assistant.improver.repository.SourceRepositories
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

data class PublishFile(
    val repositoryPath: String,
    val component: ComponentId,
    val sourcePath: String,
    val destinationPath: String,
)

fun interface PublishConfirmation {
    fun confirm(files: List<PublishFile>): String?
}

enum class PublishFailureStatus {
    DIVERGENCE,
    NO_ORIGIN,
    PUSH_FAILED,
    COMMIT_FAILED,
    DETACHED,
    EMPTY_MESSAGE,
}

data class PublishFailure(val status: PublishFailureStatus, val repositoryPath: String)

data class PublishReport(
    val snapshot: InstallationSnapshot,
    val files: List<PublishFile>,
    val publishedRepositories: List<String>,
    val failure: PublishFailure?,
    val cancelled: Boolean,
)

object ComponentPublish {
    fun publish(
        projectRoot: Path,
        repositories: SourceRepositories,
        snapshot: InstallationSnapshot,
        confirm: PublishConfirmation,
        git: GitCommand = ProcessGit,
    ): PublishReport {
        val files = candidates(projectRoot, repositories, snapshot)
        if (files.isEmpty()) {
            return PublishReport(snapshot, emptyList(), emptyList(), null, false)
        }
        val message = confirm.confirm(files)
        if (message == null) {
            return PublishReport(snapshot, files, emptyList(), null, true)
        }
        if (message.isBlank()) {
            return PublishReport(
                snapshot,
                files,
                emptyList(),
                PublishFailure(PublishFailureStatus.EMPTY_MESSAGE, ""),
                false,
            )
        }
        var current = snapshot
        val published = mutableListOf<String>()
        files.groupBy { it.repositoryPath }.forEach { (repositoryPath, _) ->
            when (val result = publishRepository(projectRoot, repositoryPath, current, message, git)) {
                is RepoResult.Published -> {
                    current = result.snapshot
                    published += repositoryPath
                }
                is RepoResult.Rejected ->
                    return PublishReport(current, files, published.toList(), result.failure, false)
            }
        }
        return PublishReport(current, files, published.toList(), null, false)
    }

    private fun candidates(
        projectRoot: Path,
        repositories: SourceRepositories,
        snapshot: InstallationSnapshot,
    ): List<PublishFile> {
        val recorded = snapshot.files().groupBy { it.repositoryPath }
        return repositories.entries()
            .filter { it.mode == RepositoryMode.SYNCHRONISATION }
            .flatMap { repository ->
                changedFiles(projectRoot, recorded[repository.path].orEmpty()).map { file ->
                    PublishFile(repository.path, file.component, file.sourcePath, file.destinationPath)
                }
            }
    }

    private fun changedFiles(projectRoot: Path, recorded: List<InstalledFile>): List<InstalledFile> =
        recorded
            .filter { differs(projectRoot, it) }
            .sortedWith(compareBy({ it.sourcePath }, { it.destinationPath }))

    private fun publishRepository(
        projectRoot: Path,
        repositoryPath: String,
        snapshot: InstallationSnapshot,
        message: String,
        git: GitCommand,
    ): RepoResult {
        val repositoryRoot = Path.of(repositoryPath)
        val branch = when (val lookup = lookupBranch(repositoryRoot, repositoryPath, git)) {
            is BranchLookup.Stop -> return lookup.result
            is BranchLookup.Ready -> lookup.name
        }
        val recorded = snapshot.files().filter { it.repositoryPath == repositoryPath }
        val changed = changedFiles(projectRoot, recorded)
        val plans = when (val planned = planCopies(projectRoot, repositoryRoot, repositoryPath, recorded, changed)) {
            is PlanLookup.Stop -> return planned.result
            is PlanLookup.Ready -> planned.plans
        }
        writeCopies(repositoryRoot, plans)
        commitAndPush(git, plans.map { it.sourcePath }, message, branch, repositoryPath)?.let { return it }
        return RepoResult.Published(withPublishedDigests(projectRoot, repositoryPath, snapshot, changed))
    }

    private fun lookupBranch(repositoryRoot: Path, repositoryPath: String, git: GitCommand): BranchLookup {
        if (git.run(repositoryRoot, listOf("remote", "get-url", "origin")).exitCode != 0) {
            return BranchLookup.Stop(RepoResult.Rejected(PublishFailure(PublishFailureStatus.NO_ORIGIN, repositoryPath)))
        }
        val parsed = git.run(repositoryRoot, listOf("rev-parse", "--abbrev-ref", "HEAD"))
        val branch = parsed.output.trim()
        if (parsed.exitCode != 0 || branch.isEmpty() || branch == "HEAD") {
            return BranchLookup.Stop(RepoResult.Rejected(PublishFailure(PublishFailureStatus.DETACHED, repositoryPath)))
        }
        return BranchLookup.Ready(branch)
    }

    private fun planCopies(
        projectRoot: Path,
        repositoryRoot: Path,
        repositoryPath: String,
        recorded: List<InstalledFile>,
        changed: List<InstalledFile>,
    ): PlanLookup {
        val plans = mutableListOf<CopyPlan>()
        for (sourcePath in changed.map { it.sourcePath }.distinct()) {
            val group = changed.filter { it.sourcePath == sourcePath }
            val payloads = group.map { Files.readAllBytes(resolve(projectRoot, it.destinationPath)) }
            if (payloads.zipWithNext().any { (left, right) -> !left.contentEquals(right) }) {
                return PlanLookup.Stop(RepoResult.Rejected(PublishFailure(PublishFailureStatus.DIVERGENCE, repositoryPath)))
            }
            val bytes = payloads.first()
            val digests = recorded.filter { it.sourcePath == sourcePath }.map { it.sha256 }.distinct()
            if (digests.size != 1) {
                return PlanLookup.Stop(RepoResult.Rejected(PublishFailure(PublishFailureStatus.DIVERGENCE, repositoryPath)))
            }
            val origin = resolve(repositoryRoot, sourcePath)
            if (Files.isSymbolicLink(origin)) {
                throw IOException("Refusing to write through a symbolic link: $origin")
            }
            if (!Files.isRegularFile(origin, LinkOption.NOFOLLOW_LINKS)) {
                return PlanLookup.Stop(RepoResult.Rejected(PublishFailure(PublishFailureStatus.DIVERGENCE, repositoryPath)))
            }
            val originBytes = Files.readAllBytes(origin)
            when {
                originBytes.contentEquals(bytes) -> plans += CopyPlan(sourcePath, bytes, copy = false)
                ContentDigest.sha256(originBytes) == digests.single() -> plans += CopyPlan(sourcePath, bytes, copy = true)
                else -> return PlanLookup.Stop(RepoResult.Rejected(PublishFailure(PublishFailureStatus.DIVERGENCE, repositoryPath)))
            }
        }
        return PlanLookup.Ready(plans)
    }

    private fun writeCopies(repositoryRoot: Path, plans: List<CopyPlan>) {
        plans.filter { it.copy }.forEach { plan ->
            val target = resolve(repositoryRoot, plan.sourcePath)
            if (Files.isSymbolicLink(target)) {
                throw IOException("Refusing to write through a symbolic link: $target")
            }
            target.parent?.let { Files.createDirectories(it) }
            Files.write(target, plan.bytes)
        }
    }

    private fun commitAndPush(
        git: GitCommand,
        paths: List<String>,
        message: String,
        branch: String,
        repositoryPath: String,
    ): RepoResult.Rejected? {
        val repositoryRoot = Path.of(repositoryPath)
        val status = git.run(repositoryRoot, listOf("status", "--porcelain", "--") + paths)
        if (status.exitCode != 0) {
            return RepoResult.Rejected(PublishFailure(PublishFailureStatus.COMMIT_FAILED, repositoryPath))
        }
        if (status.output.isNotBlank()) {
            val added = git.run(repositoryRoot, listOf("add", "--") + paths)
            if (added.exitCode != 0) {
                return RepoResult.Rejected(PublishFailure(PublishFailureStatus.COMMIT_FAILED, repositoryPath))
            }
            val committed = git.run(repositoryRoot, listOf("commit", "-m", message, "--") + paths)
            if (committed.exitCode != 0) {
                return RepoResult.Rejected(PublishFailure(PublishFailureStatus.COMMIT_FAILED, repositoryPath))
            }
        }
        val pushed = git.run(repositoryRoot, listOf("push", "origin", branch))
        if (pushed.exitCode != 0) {
            return RepoResult.Rejected(PublishFailure(PublishFailureStatus.PUSH_FAILED, repositoryPath))
        }
        return null
    }

    private fun withPublishedDigests(
        projectRoot: Path,
        repositoryPath: String,
        snapshot: InstallationSnapshot,
        changed: List<InstalledFile>,
    ): InstallationSnapshot {
        val publishedDigest = changed.associate { file ->
            file.destinationPath to ContentDigest.sha256(Files.readAllBytes(resolve(projectRoot, file.destinationPath)))
        }
        val updated = snapshot.files().map { file ->
            val digest = publishedDigest[file.destinationPath]
            if (file.repositoryPath == repositoryPath && digest != null) file.copy(sha256 = digest) else file
        }
        return InstallationSnapshot.restore(updated)
    }

    private fun differs(projectRoot: Path, file: InstalledFile): Boolean {
        val path = resolve(projectRoot, file.destinationPath)
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) return false
        return ContentDigest.sha256(Files.readAllBytes(path)) != file.sha256
    }

    private fun resolve(root: Path, relative: String): Path =
        relative.split('/').fold(root) { parent, name -> parent.resolve(name) }

    private class CopyPlan(val sourcePath: String, val bytes: ByteArray, val copy: Boolean)

    private sealed interface BranchLookup {
        class Ready(val name: String) : BranchLookup
        class Stop(val result: RepoResult) : BranchLookup
    }

    private sealed interface PlanLookup {
        class Ready(val plans: List<CopyPlan>) : PlanLookup
        class Stop(val result: RepoResult) : PlanLookup
    }

    private sealed interface RepoResult {
        data class Published(val snapshot: InstallationSnapshot) : RepoResult
        data class Rejected(val failure: PublishFailure) : RepoResult
    }
}
