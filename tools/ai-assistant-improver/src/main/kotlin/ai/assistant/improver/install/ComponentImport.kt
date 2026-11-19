package ai.assistant.improver.install

import ai.assistant.improver.assistant.AssistantId
import ai.assistant.improver.assistant.EnabledAssistants
import ai.assistant.improver.component.ComponentDiscovery
import ai.assistant.improver.component.ComponentId
import ai.assistant.improver.component.ComponentSelection
import ai.assistant.improver.publish.GitCommand
import ai.assistant.improver.publish.ProcessGit
import ai.assistant.improver.repository.CacheDirectories
import ai.assistant.improver.repository.RepositoryAddress
import ai.assistant.improver.repository.RepositoryMode
import ai.assistant.improver.repository.SourceRepositories
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

data class OverwritePrompt(
    val repositoryPath: String,
    val component: ComponentId,
    val assistant: AssistantId,
    val destinationPath: String,
)

fun interface OverwriteConfirmation {
    fun allow(prompt: OverwritePrompt): Boolean
}

enum class ImportAction { COPIED, REFUSED, KEPT, NO_TARGET }

data class ImportDestination(
    val repositoryPath: String,
    val component: ComponentId,
    val assistant: AssistantId,
    val destinationPath: String?,
    val action: ImportAction,
)

enum class ImportNoticeKind { CLONE_FAILED, FETCH_FAILED, KEPT_TREE }

data class ImportNotice(val repositoryPath: String, val kind: ImportNoticeKind)

data class ImportReport(
    val snapshot: InstallationSnapshot,
    val destinations: List<ImportDestination>,
    val notices: List<ImportNotice> = emptyList(),
)

object ComponentImport {
    fun import(
        projectRoot: Path,
        repositories: SourceRepositories,
        selection: ComponentSelection,
        assistants: EnabledAssistants,
        snapshot: InstallationSnapshot,
        confirm: OverwriteConfirmation,
        discover: (Path) -> List<ComponentId> = ComponentDiscovery::discover,
        cacheRoot: Path? = null,
        git: GitCommand = ProcessGit,
    ): ImportReport {
        val files = snapshot.files().toMutableList()
        val destinations = mutableListOf<ImportDestination>()
        val notices = mutableListOf<ImportNotice>()
        repositories.entries().forEach { repository ->
            when (val refreshed = refresh(repository.path, cacheRoot, git)) {
                is Refresh.Skip -> notices += refreshed.notice
                is Refresh.Ready -> {
                    notices += refreshed.notices
                    val work = ImportWork(
                        projectRoot,
                        repository.path,
                        refreshed.root,
                        repository.mode,
                        confirm,
                        files,
                    )
                    val selected = selection.components(repository.path, discover(work.repositoryRoot))
                        .filter { it.selected }
                    selected.forEach { component ->
                        assistants.enabledIds().forEach { assistant ->
                            destinations += importTarget(work, component.id, assistant)
                        }
                    }
                }
            }
        }
        return ImportReport(InstallationSnapshot.restore(files), destinations, notices)
    }

    private sealed interface Refresh {
        class Ready(val root: Path, val notices: List<ImportNotice>) : Refresh
        class Skip(val notice: ImportNotice) : Refresh
    }

    private fun refresh(storedPath: String, cacheRoot: Path?, git: GitCommand): Refresh {
        val parsed = RepositoryAddress.parse(storedPath)
        if (parsed !is RepositoryAddress.Parsed.Remote) {
            return Refresh.Ready(Path.of(storedPath), emptyList())
        }
        val cache = requireNotNull(cacheRoot)
        val root = CacheDirectories.directory(cache, parsed.url)
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            Files.createDirectories(cache)
            val cloned = git.run(cache, listOf("clone", "--", parsed.url, root.toString()))
            if (cloned.exitCode != 0) {
                CacheDirectories.deleteIfPresent(cache, parsed.url)
                return Refresh.Skip(ImportNotice(parsed.url, ImportNoticeKind.CLONE_FAILED))
            }
        }
        val fetched = git.run(root, listOf("fetch", "origin"))
        if (fetched.exitCode != 0) {
            return Refresh.Ready(root, listOf(ImportNotice(parsed.url, ImportNoticeKind.FETCH_FAILED)))
        }
        val branch = git.run(root, listOf("rev-parse", "--abbrev-ref", "HEAD"))
        val name = branch.output.trim()
        if (branch.exitCode != 0 || name.isEmpty() || name == "HEAD") {
            return Refresh.Ready(root, listOf(ImportNotice(parsed.url, ImportNoticeKind.KEPT_TREE)))
        }
        val status = git.run(root, listOf("status", "--porcelain"))
        val ahead = git.run(root, listOf("rev-list", "--count", "origin/$name..HEAD"))
        val ancestor = git.run(root, listOf("merge-base", "--is-ancestor", "HEAD", "origin/$name"))
        val fastForward = status.exitCode == 0 &&
            status.output.isBlank() &&
            ahead.exitCode == 0 &&
            ahead.output.trim() == "0" &&
            ancestor.exitCode == 0
        if (!fastForward) {
            return Refresh.Ready(root, listOf(ImportNotice(parsed.url, ImportNoticeKind.KEPT_TREE)))
        }
        git.run(root, listOf("reset", "--hard", "origin/$name"))
        return Refresh.Ready(root, emptyList())
    }

    private class ImportWork(
        val projectRoot: Path,
        val repositoryPath: String,
        val repositoryRoot: Path,
        val mode: RepositoryMode,
        val confirm: OverwriteConfirmation,
        val files: MutableList<InstalledFile>,
    )

    private fun importTarget(work: ImportWork, component: ComponentId, assistant: AssistantId): ImportDestination {
        val target = InstallLayout.target(component, assistant)
            ?: return ImportDestination(work.repositoryPath, component, assistant, null, ImportAction.NO_TARGET)
        val action = when (presence(work.projectRoot, target, work.files)) {
            Presence.ABSENT, Presence.IDENTICAL -> {
                copy(work, component, target)
                ImportAction.COPIED
            }
            Presence.DIFFERENT -> when (work.mode) {
                RepositoryMode.SYNCHRONISATION -> ImportAction.KEPT
                RepositoryMode.DOWNLOAD_ONLY -> if (
                    work.confirm.allow(
                        OverwritePrompt(work.repositoryPath, component, assistant, target.projectRelative),
                    )
                ) {
                    copy(work, component, target)
                    ImportAction.COPIED
                } else {
                    ImportAction.REFUSED
                }
            }
        }
        return ImportDestination(work.repositoryPath, component, assistant, target.projectRelative, action)
    }

    private enum class Presence { ABSENT, IDENTICAL, DIFFERENT }

    private data class PlannedFile(
        val sourcePath: String,
        val destinationPath: String,
        val bytes: ByteArray,
    )

    private fun presence(projectRoot: Path, target: InstallTarget, files: List<InstalledFile>): Presence {
        val actual = actualFiles(projectRoot, target)
        if (actual.isEmpty()) return Presence.ABSENT
        val recorded = files.filter { inScope(target, it.destinationPath) }
        if (recorded.size != actual.size) return Presence.DIFFERENT
        return if (recorded.all { actual[it.destinationPath] == it.sha256 }) Presence.IDENTICAL else Presence.DIFFERENT
    }

    private fun copy(work: ImportWork, component: ComponentId, target: InstallTarget) {
        val planned = plan(work.repositoryRoot, component, target)
        planned.forEach { file ->
            val destination = resolve(work.projectRoot, file.destinationPath)
            if (Files.isSymbolicLink(destination)) {
                throw IOException("Refusing to write through a symbolic link: $destination")
            }
            destination.parent?.let { Files.createDirectories(it) }
            Files.write(destination, file.bytes)
        }
        if (target is InstallTarget.Directory) {
            val keep = planned.map { it.destinationPath }.toSet()
            val directory = resolve(work.projectRoot, target.projectRelative)
            regularFiles(directory).forEach { existing ->
                val destination = "${target.projectRelative}/${relative(directory, existing)}"
                if (destination !in keep) Files.delete(existing)
            }
        }
        val kept = work.files.filterNot { inScope(target, it.destinationPath) }
        val added = planned.map { file ->
            InstalledFile(
                work.repositoryPath,
                component,
                file.sourcePath,
                file.destinationPath,
                ContentDigest.sha256(file.bytes),
            )
        }
        work.files.clear()
        work.files.addAll(kept)
        work.files.addAll(added)
    }

    private fun plan(repositoryRoot: Path, component: ComponentId, target: InstallTarget): List<PlannedFile> =
        when (target) {
            is InstallTarget.File -> listOf(
                PlannedFile(
                    component.relativePath,
                    target.projectRelative,
                    Files.readAllBytes(resolve(repositoryRoot, component.relativePath)),
                ),
            )
            is InstallTarget.Directory -> {
                val sourceDir = resolve(repositoryRoot, component.relativePath)
                regularFiles(sourceDir).map { file ->
                    val relative = relative(sourceDir, file)
                    PlannedFile(
                        "${component.relativePath}/$relative",
                        "${target.projectRelative}/$relative",
                        Files.readAllBytes(file),
                    )
                }.sortedBy { it.sourcePath }
            }
        }

    private fun actualFiles(projectRoot: Path, target: InstallTarget): Map<String, String> = when (target) {
        is InstallTarget.File -> {
            val path = resolve(projectRoot, target.projectRelative)
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                emptyMap()
            } else {
                mapOf(target.projectRelative to ContentDigest.sha256(Files.readAllBytes(path)))
            }
        }
        is InstallTarget.Directory -> {
            val directory = resolve(projectRoot, target.projectRelative)
            regularFiles(directory).associate { file ->
                val destination = "${target.projectRelative}/${relative(directory, file)}"
                destination to ContentDigest.sha256(Files.readAllBytes(file))
            }
        }
    }

    private fun inScope(target: InstallTarget, destinationPath: String): Boolean = when (target) {
        is InstallTarget.File -> destinationPath == target.projectRelative
        is InstallTarget.Directory ->
            destinationPath == target.projectRelative || destinationPath.startsWith("${target.projectRelative}/")
    }

    private fun regularFiles(directory: Path): List<Path> {
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) return emptyList()
        val found = mutableListOf<Path>()
        Files.walkFileTree(
            directory,
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult =
                    if (attrs.isSymbolicLink) FileVisitResult.SKIP_SUBTREE else FileVisitResult.CONTINUE

                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (!attrs.isSymbolicLink && attrs.isRegularFile) found.add(file)
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult =
                    FileVisitResult.CONTINUE
            },
        )
        return found
    }

    private fun resolve(root: Path, relative: String): Path =
        relative.split('/').fold(root) { parent, name -> parent.resolve(name) }

    private fun relative(root: Path, path: Path): String = root.relativize(path).joinToString("/")
}
