package ch.tutteli.gradle.plugins.settings

import org.gradle.api.initialization.Settings

class IncludeCustomInFolder {
    static void includePrefixedInFolder(Settings settings, String folder, String... projects) {
        projects.each {
            include(settings, folder, "${settings.rootProject.name}-$it")
        }
    }

    static void includeCustomInFolder(Settings settings, String folder, String... projectsWithoutPrefix) {
        projectsWithoutPrefix.each {
            include(settings, folder, it)
        }
    }

    private static include(Settings settings, String folder, String projectName){
        def dir = new File("${settings.rootProject.projectDir}/$folder/$projectName")
        if (!dir.exists()) {
            throw new IllegalArgumentException("cannot include the project $projectName, its folder does not exist: ${dir.canonicalPath}")
        }
        settings.include ":$projectName"
        settings.project(":$projectName").projectDir = dir
    }
}
