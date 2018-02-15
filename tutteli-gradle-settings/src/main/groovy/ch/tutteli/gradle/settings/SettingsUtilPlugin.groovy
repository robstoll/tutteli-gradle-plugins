package ch.tutteli.gradle.settings

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class SettingsUtilPlugin implements Plugin<Settings> {
    @Override
    void apply(Settings settings) {

        def includeCustomInFolder = {String folder, String customName ->
            settings.include ":$customName"
            settings.project(":$customName").projectDir = new File(
                "${settings.rootProject.projectDir}/$folder/$customName"
            )
        }

        settings.ext.includeCustomInFolder = includeCustomInFolder

        settings.ext.includeInFolder = { String folder, String nameWithoutPrefix ->
            includeCustomInFolder(folder, "${settings.rootProject.name}-$nameWithoutPrefix")
        }

        settings.ext.includeOwn = { String nameWithoutPrefix ->
            includeCustomInFolder("", "${settings.rootProject.name}-$nameWithoutPrefix")
        }
    }
}
