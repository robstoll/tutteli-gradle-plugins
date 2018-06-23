package ch.tutteli.gradle.kotlin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.UnknownPluginException
import org.jetbrains.kotlin.gradle.plugin.*

class KotlinUtilsPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def kotlinVersion = getKotlinVersion(project)
        project.ext.kotlinStdLib = { "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion" }
        project.ext.kotlinStdJsLib = { "org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion" }
        project.ext.kotlinStdCommonLib = { "org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion" }
        project.ext.kotlinReflect = { "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion" }

        project.ext.withoutKbox = { exclude group: 'ch.tutteli.kbox' }
        project.ext.withoutKotlin = { exclude group: 'org.jetbrains.kotlin' }
    }

    private static String getKotlinVersion(Project project) {
        try {
            def plugins = project.plugins
            def kotlinPlugin = plugins.hasPlugin(KotlinPluginWrapper) ? plugins.getPlugin(KotlinPluginWrapper)
                : plugins.hasPlugin(Kotlin2JsPluginWrapper) ? plugins.getPlugin(Kotlin2JsPluginWrapper)
                : plugins.getPlugin(KotlinCommonPluginWrapper)
            return kotlinPlugin.getKotlinPluginVersion()
        } catch (UnknownPluginException e) {
            throw new IllegalStateException("You need to apply a kotlin plugin before applying the ch.tutteli.kotlin.utils plugin.", e)
        }
    }
}
