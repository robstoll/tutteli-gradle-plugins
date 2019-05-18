package ch.tutteli.gradle.spek

import ch.tutteli.gradle.junitjacoco.JunitJacocoPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.UnknownPluginException
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.junit.platform.gradle.plugin.JUnitPlatformExtension

class SpekPluginExtension {
    String version = '2.0.4'
}

class SpekPlugin implements Plugin<Project> {
    static final String EXTENSION_NAME = 'spek'
    protected static
    final String ERR_KOTLIN_PLUGIN = "You need to apply a JVM compliant kotlin plugin before applying the ch.tutteli.spek plugin." +
        "\n For instance, the 'kotlin' or the 'kotlin-platform-jvm' plugin."

    @Override
    void apply(Project project) {
        project.pluginManager.apply(JunitJacocoPlugin)
        def kotlinVersion = getKotlinVersion(project)

        project.repositories {
            maven { url "https://dl.bintray.com/jetbrains/spek" }
            maven { url "https://dl.bintray.com/spekframework/spek" }
            mavenCentral()
        }

        def extension = project.extensions.create(EXTENSION_NAME, SpekPluginExtension)
        project.afterEvaluate {
            String spekVersion = extension.version
            boolean isVersion1 = spekVersion.startsWith("1")
            project.extensions.getByType(JUnitPlatformExtension).filters {
                engines {
                    if (isVersion1) {
                        include 'spek'
                    } else {
                        include 'spek2'
                    }
                }
            }

            project.dependencies {
                implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"

                if (isVersion1) {
                    testImplementation("org.jetbrains.spek:spek-api:$spekVersion") {
                        exclude group: 'org.jetbrains.kotlin'
                    }
                    testRuntimeOnly("org.jetbrains.spek:spek-junit-platform-engine:$spekVersion") {
                        exclude group: 'org.junit.platform'
                        exclude group: 'org.jetbrains.kotlin'
                    }
                    testRuntimeOnly "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion" //spek requires reflect
                } else {
                    testImplementation "org.spekframework.spek2:spek-dsl-jvm:$spekVersion"
                    testRuntimeOnly "org.spekframework.spek2:spek-runner-junit5:$spekVersion"

                    testRuntimeOnly "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion" //spek requires reflect
                }
            }
        }
    }

    private static String getKotlinVersion(Project project) {
        try {
            def kotlinPlugin = project.plugins.getPlugin(KotlinPluginWrapper)
            return kotlinPlugin.getKotlinPluginVersion()
        } catch (UnknownPluginException e) {
            throw new IllegalStateException(ERR_KOTLIN_PLUGIN, e)
        }
    }
}
