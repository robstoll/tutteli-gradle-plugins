package ch.tutteli.gradle.settings

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class SettingsUtilsPluginExtension {
    private Settings settings
    private String folder

    SettingsUtilsPluginExtension(Settings settings, String folder) {
        this.settings = settings
        this.folder = folder
    }

    void folder(String folderName, Action<SettingsUtilsPluginExtension> configure) {
        configure.execute(new SettingsUtilsPluginExtension(settings, folderName))
    }

    void modules(String... modules) {
        IncludeCustomInFolder.includeInFolder(settings, folder, modules)
    }

    void custom(String... modulesWithoutPrefix) {
        IncludeCustomInFolder.includeCustomInFolder(settings, folder, modulesWithoutPrefix)
    }
}

class IncludeCustomInFolder {
    static void includeInFolder(Settings settings, String folder, String... namesWithoutPrefix) {
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
        settings.extensions.create('include', SettingsUtilsPluginExtension, settings, "")

        settings.ext.includeCustomInFolder = { String folder, String... customNames ->
            IncludeCustomInFolder.includeCustomInFolder(settings, folder, customNames)
        }

        settings.ext.includeInFolder = { String folder, String... namesWithoutPrefix ->
            IncludeCustomInFolder.includeInFolder(settings, folder, namesWithoutPrefix)
        }

        settings.ext.includeOwn = { String... namesWithoutPrefix ->
            IncludeCustomInFolder.includeInFolder(settings, "", namesWithoutPrefix)
        }
    }
}
