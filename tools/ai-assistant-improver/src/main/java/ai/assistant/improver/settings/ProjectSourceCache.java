package ai.assistant.improver.settings;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;

import java.nio.file.Path;

final class ProjectSourceCache {
    private ProjectSourceCache() {
    }

    static Path path(Project project, String cacheDirectory) {
        Path path = ProjectUtil.getProjectCachePath(project, cacheDirectory);
        if (path == null) {
            throw new IllegalStateException("Project cache path is unavailable");
        }
        return path;
    }
}
