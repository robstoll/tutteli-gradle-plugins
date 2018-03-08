package ch.tutteli.gradle.project

import org.gradle.api.Plugin
import org.gradle.api.Project

class UtilsPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.ext.prefixedProject = { String name ->
            project.project(":${project.rootProject.name}-$name")
        }
    }
}
