package ch.tutteli.gradle.spek

import ch.tutteli.gradle.junitjacoco.JunitJacocoPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.junit.platform.gradle.plugin.JUnitPlatformExtension

class SpekPluginExtension {
    String version = '1.1.5'
}

class SpekPlugin implements Plugin<Project> {
    static final String EXTENSION_NAME = 'spek'

    @Override
    void apply(Project project) {
        project.pluginManager.apply(JunitJacocoPlugin)
        project.pluginManager.apply(KotlinPluginWrapper)
        def kotlinPlugin = project.plugins.getPlugin(KotlinPluginWrapper)
        def kotlinVersion = kotlinPlugin.getKotlinPluginVersion()


        project.extensions.getByType(JUnitPlatformExtension).filters {
            engines {
                include 'spek'
            }
        }

        project.repositories {
            maven { url "https://dl.bintray.com/jetbrains/spek" }
            mavenCentral()
        }

        def extension = project.extensions.create(EXTENSION_NAME, SpekPluginExtension)
        project.afterEvaluate {
            project.dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
                testCompile "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion" //spek requires reflect

                testCompile("org.jetbrains.spek:spek-api:$extension.version") {
                    exclude group: 'org.jetbrains.kotlin'
                }
                testRuntimeOnly("org.jetbrains.spek:spek-junit-platform-engine:$extension.version") {
                    exclude group: 'org.junit.platform'
                    exclude group: 'org.jetbrains.kotlin'
                }

            }
        }
    }
}
