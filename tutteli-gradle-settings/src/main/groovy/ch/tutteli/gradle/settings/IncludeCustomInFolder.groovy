package ch.tutteli.gradle.settings

import org.gradle.api.initialization.Settings

class IncludeCustomInFolder {
    static void includePrefixedInFolder(Settings settings, String folder, String... namesWithoutPrefix) {
        namesWithoutPrefix.each {
            include(settings, folder, "${settings.rootProject.name}-$it")
        }
    }

    static void includeCustomInFolder(Settings settings, String folder, String... customNames) {
        customNames.each {
            include(settings, folder, it)
        }
    }

    private static include(Settings settings, String folder, String name){
        def dir = new File("${settings.rootProject.projectDir}/$folder/$name")
        if (!dir.exists()) {
            throw new IllegalArgumentException("cannot include the project $name, its folder does not exist: ${dir.canonicalPath}")
        }
        settings.include ":$name"
        settings.project(":$name").projectDir = dir
    }
}
