package ch.tutteli.gradle.plugins.spek

import ch.tutteli.gradle.plugins.junitjacoco.JunitJacocoPlugin
import ch.tutteli.gradle.plugins.spek.generated.Dependencies
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.UnknownPluginException
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

class SpekPluginExtension {
    String version = Dependencies.spek_version
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
                // seems like running test from Intellij sometimes fails with:
                // java.lang.NoSuchMethodError: 'org.junit.platform.commons.function.Try org.junit.platform.commons.util.ReflectionUtils.tryToLoadClass(java.lang.String)'
                // adding the following dependency fixes the problem -- no idea why testRuntimeOnly is not enough
                // but I don't want to bother anymore
                testImplementation "org.junit.platform:junit-platform-commons:$Dependencies.junit_platform_version"

                if (isVersion1) {
                    testImplementation("org.jetbrains.spek:spek-api:$spekVersion") {
                        exclude group: 'org.jetbrains.kotlin'
                    }
                    testRuntimeOnly("org.jetbrains.spek:spek-junit-platform-engine:$spekVersion") {
                        exclude group: 'org.junit.platform'
                        exclude group: 'org.jetbrains.kotlin'
                    }

                    testImplementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
                    testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine:$Dependencies.junit_jupiter_version"
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
