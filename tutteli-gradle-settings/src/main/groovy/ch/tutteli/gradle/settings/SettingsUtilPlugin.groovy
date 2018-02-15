package ch.tutteli.gradle.settings

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class SettingsUtilPlugin implements Plugin<Settings> {
    @Override
    void apply(Settings settings) {

        def includeCustomInFolder = { String folder, String customName ->
            def dir = new File("${settings.rootProject.projectDir}/$folder/$customName")
            if (!dir.exists()) {
                throw new IllegalArgumentException("cannot include the project $customName, its folder does not exist: ${dir.canonicalPath}")
            }
            settings.include ":$customName"
            settings.project(":$customName").projectDir = dir
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
