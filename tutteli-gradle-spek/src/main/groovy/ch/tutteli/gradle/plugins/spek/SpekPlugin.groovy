package ch.tutteli.gradle.plugins.spek

import ch.tutteli.gradle.plugins.junitjacoco.JunitJacocoPlugin
import ch.tutteli.gradle.plugins.spek.generated.Dependencies
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapperKt

class SpekPluginExtension {
    String version = Dependencies.spek_version
}

class SpekPlugin implements Plugin<Project> {
    static final String EXTENSION_NAME = 'spek'

    protected static final String ERR_KOTLIN_PLUGIN = "You need to apply a JVM compliant kotlin plugin." +
        "\n For instance, the 'org.jetbrains.kotlin.jvm' or the 'org.jetbrains.kotlin.multiplatform' plugin."

    @Override
    void apply(Project project) {
        project.pluginManager.apply(JunitJacocoPlugin)

        def extension = project.extensions.create(EXTENSION_NAME, SpekPluginExtension)
        project.afterEvaluate {
            String spekVersion = extension.version
            if (spekVersion.startsWith("1")) {
                throw new IllegalStateException("spek 1 is no longer supported by this plugin.")
            }

            if (project.plugins.hasPlugin('org.jetbrains.kotlin.multiplatform')) {
                configureForMpp(project, spekVersion)
            } else {
                configureForJvm(project, spekVersion)
            }
        }
    }

    private static configureForJvm(Project project, String spekVersion) {

        def kotlinVersion = getKotlinVersion(project)
        project.test {
            options {
                includeEngines 'spek2'
            }
        }

        project.dependencies {
            // seems like running test from Intellij sometimes fails with:
            // java.lang.NoSuchMethodError: 'org.junit.platform.commons.function.Try org.junit.platform.commons.util.ReflectionUtils.tryToLoadClass(java.lang.String)'
            // adding the following dependency fixes the problem -- no idea why testRuntimeOnly is not enough
            // but I don't want to bother anymore
            testImplementation "org.junit.platform:junit-platform-commons:$Dependencies.junit_platform_version"

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

    private static String getKotlinVersion(Project project) {
        def version
        try {
            version = KotlinPluginWrapperKt.getKotlinPluginVersion(project)
        } catch (MissingMethodException _) {
            // KotlinPluginWrapperKt (source where extension method getKotlinPluginVersion is defined) might not exist
            // if no kotlin plugin was applied or an old one or an old gradle version is used where the extension
            // method on Project does not exist yet
            version = null
        }

        //TODO 6.0.0 drop once we no longer support gradle < 8 ?
        if (version == null) {
            def plugins = project.plugins
            def kotlinPlugin =
                plugins.findPlugin("org.jetbrains.kotlin.multiplatform")
                    ?: plugins.findPlugin("org.jetbrains.kotlin.jvm")
                    ?: plugins.findPlugin("org.jetbrains.kotlin.js")
            version = kotlinPlugin?.getKotlinPluginVersion()
        }

        if (version != null) {
            return version
        } else {
            throw new IllegalStateException(ERR_KOTLIN_PLUGIN)
        }
    }

    private static configureForMpp(Project project, String spekVersion) {
        def extension = project.extensions.getByType(KotlinMultiplatformExtension)
        extension.jvm {
            testRuns["test"].executionTask.configure {
                useJUnitPlatform {
                    includeEngines("spek2")
                }
            }
        }
        extension.sourceSets.configure {
            def commonTest = extension.sourceSets.findByName("commonTest")
            if (commonTest != null) {
                commonTest.dependencies {
                    implementation "org.spekframework.spek2:spek-dsl-metadata:$spekVersion"
                }
            }

            def jvmTest = extension.sourceSets.findByName("jvmTest")
            if (jvmTest != null) {
                jvmTest.dependencies {
                    implementation "org.junit.platform:junit-platform-commons:$Dependencies.junit_platform_version"

                    implementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion") {
                        exclude group: 'org.jetbrains.kotlin'
                    }
                    runtimeOnly("org.spekframework.spek2:spek-runner-junit5:$spekVersion") {
                        exclude group: 'org.jetbrains.kotlin'
                    }
                    implementation kotlin("stdlib-jdk8")
                    runtimeOnly kotlin("reflect") //spek requires reflect
                }
            }
            def jsTest = extension.sourceSets.findByName("jsTest")
            if (jsTest != null) {
                jsTest.dependencies {
                    implementation "org.spekframework.spek2:spek-dsl-js:$spekVersion"
                }
            }
        }
    }
}
