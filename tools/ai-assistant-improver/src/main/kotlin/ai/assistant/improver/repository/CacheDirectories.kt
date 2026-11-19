package ai.assistant.improver.repository

import ai.assistant.improver.install.ContentDigest
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

internal object CacheDirectories {
    fun workTree(storedPath: String, cacheRoot: Path?): Path =
        when (val parsed = RepositoryAddress.parse(storedPath)) {
            is RepositoryAddress.Parsed.Remote -> directory(requireNotNull(cacheRoot), parsed.url)
            else -> Path.of(storedPath)
        }

    fun directory(cacheRoot: Path, retainedUrl: String): Path {
        val digest = ContentDigest.sha256(retainedUrl.toByteArray(StandardCharsets.UTF_8))
        check(digest.matches(SHA256)) { "SHA-256 must be lowercase hex" }
        val root = cacheRoot.toAbsolutePath().normalize()
        val destination = root.resolve(digest).normalize()
        check(destination.parent == root) { "Clone directory escapes the cache" }
        return destination
    }

    fun deleteIfPresent(cacheRoot: Path, retainedUrl: String) {
        val destination = directory(cacheRoot, retainedUrl)
        if (!Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) return
        if (Files.isSymbolicLink(destination)) {
            Files.delete(destination)
            return
        }
        destination.toFile().setWritable(true)
        Files.walkFileTree(
            destination,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (!attrs.isSymbolicLink) file.toFile().setWritable(true)
                    Files.delete(file)
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                    if (exc != null) throw exc
                    dir.toFile().setWritable(true)
                    Files.delete(dir)
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
                    file.toFile().setWritable(true)
                    Files.deleteIfExists(file)
                    return FileVisitResult.CONTINUE
                }
            },
        )
    }

    private val SHA256 = Regex("[0-9a-f]{64}")
}
