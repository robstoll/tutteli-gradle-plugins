package ch.tutteli.gradle.settings

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class SettingsUtilPluginExtension {
    private Settings settings
    private String folder

    SettingsUtilPluginExtension(Settings settings, String folder) {
        this.settings = settings
        this.folder = folder
    }

    void folder(String folderName, Action<SettingsUtilPluginExtension> configure) {
        configure.execute(new SettingsUtilPluginExtension(settings, folderName))
    }

    void prefixed(String... modules) {
        IncludeCustomInFolder.includePrefixedInFolder(settings, folder, modules)
    }

    void project(String... modulesWithoutPrefix) {
        IncludeCustomInFolder.includeCustomInFolder(settings, folder, modulesWithoutPrefix)
    }
}

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

class SettingsUtilPlugin implements Plugin<Settings> {
    @Override
    void apply(Settings settings) {
        settings.extensions.create('include', SettingsUtilPluginExtension, settings, "")

        println("test: ${settings.ext.toString()}")
        settings.ext.includeCustomInFolder = { String folder, String... customNames ->
            IncludeCustomInFolder.includeCustomInFolder(settings, folder, customNames)
        }

        settings.ext.includePrefixedInFolder = { String folder, String... namesWithoutPrefix ->
            IncludeCustomInFolder.includePrefixedInFolder(settings, folder, namesWithoutPrefix)
        }

        settings.ext.includePrefixed = { String... namesWithoutPrefix ->
            IncludeCustomInFolder.includePrefixedInFolder(settings, "", namesWithoutPrefix)
        }
    }
}
