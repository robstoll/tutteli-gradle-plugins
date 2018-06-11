package ch.tutteli.gradle.kotlin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

class KotlinPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.pluginManager.apply(KotlinPluginWrapper)
        def kotlinPlugin = project.plugins.getPlugin(KotlinPluginWrapper)
        def kotlinVersion = kotlinPlugin.getKotlinPluginVersion()
        project.ext.kotlinStdLib = { "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion" }
        project.ext.kotlinReflect = { "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion" }

        project.ext.withoutKbox = { exclude group: 'ch.tutteli.kbox' }
        project.ext.withoutKotlin = { exclude group: 'org.jetbrains.kotlin' }

    }
}
