package ch.tutteli.gradle.project

import org.gradle.api.Plugin
import org.gradle.api.Project

class UtilsPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def prefixedProjectName = { String name ->
            ":${project.rootProject.name}-$name"
        }

        project.ext.prefixedProject = { String name ->
            project.project(prefixedProjectName(name))
        }
        project.ext.prefixedProjectName = prefixedProjectName
    }
}
