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

class SettingsUtilPlugin implements Plugin<Settings> {
    @Override
    void apply(Settings settings) {
        settings.extensions.create('include', SettingsUtilPluginExtension, settings, "")

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
