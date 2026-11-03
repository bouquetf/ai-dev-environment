package ai.assistant.improver.install

import ai.assistant.improver.assistant.AssistantId
import ai.assistant.improver.assistant.EnabledAssistants
import ai.assistant.improver.component.ComponentDiscovery
import ai.assistant.improver.component.ComponentId
import ai.assistant.improver.component.ComponentSelection
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

data class ImportReport(
    val snapshot: InstallationSnapshot,
    val destinations: List<ImportDestination>,
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
    ): ImportReport {
        val files = snapshot.files().toMutableList()
        val destinations = mutableListOf<ImportDestination>()
        repositories.entries().forEach { repository ->
            val work = ImportWork(
                projectRoot,
                repository.path,
                Path.of(repository.path),
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
        return ImportReport(InstallationSnapshot.restore(files), destinations)
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
