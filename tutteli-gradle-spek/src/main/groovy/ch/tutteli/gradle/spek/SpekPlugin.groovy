package ch.tutteli.gradle.spek

import ch.tutteli.gradle.junitjacoco.JunitJacocoPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.UnknownPluginException
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

class SpekPluginExtension {
    String version = '2.0.4'
}

class SpekPlugin implements Plugin<Project> {
    static final String JUNIT_PLATFORM_VERSION = "5.5.1"
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
            project.test {
                options {
                    if (isVersion1) {
                        includeEngines 'spek'
                    } else {
                        includeEngines 'spek2'
                    }
                }
            }

            project.dependencies {
                if (isVersion1) {
                    testImplementation("org.jetbrains.spek:spek-api:$spekVersion") {
                        exclude group: 'org.jetbrains.kotlin'
                    }
                    testRuntimeOnly("org.jetbrains.spek:spek-junit-platform-engine:$spekVersion") {
                        exclude group: 'org.junit.platform'
                        exclude group: 'org.jetbrains.kotlin'
                    }

                    testImplementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
                    testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine:$JUNIT_PLATFORM_VERSION"
                    testRuntimeOnly "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion" //spek requires reflect

                } else {
                    testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion") {
                        exclude group: 'org.jetbrains.kotlin'
                    }
                    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:$spekVersion") {
                        exclude group: 'org.jetbrains.kotlin'
                    }

                    testImplementation "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion"
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
