package ch.tutteli.gradle.plugins.junitjacoco

import ch.tutteli.gradle.plugins.test.Asserts
import ch.tutteli.gradle.plugins.test.SettingsExtension
import ch.tutteli.gradle.plugins.test.SettingsExtensionObject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import java.nio.file.Files

import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

@ExtendWith(SettingsExtension)
class JunitJacocoPluginIntTest {
    private static final String KOTLIN_VERSION = '1.8.22'
    private static final String MULTIPLATFORM_PLUGIN = "org.jetbrains.kotlin.multiplatform"

    @Test
    void withTests_OldKotlinPlugin(SettingsExtensionObject settingsSetup) {
        checkSucceeds(settingsSetup, "kotlin")
    }

    @Test
    void withTests_KotlinPlugin(SettingsExtensionObject settingsSetup) {
        checkSucceeds(settingsSetup, "org.jetbrains.kotlin.jvm")
    }

    @Test
    void withTests_OldJvmPlatformPlugin(SettingsExtensionObject settingsSetup) {
        checkSucceeds(settingsSetup, "kotlin-platform-jvm")
    }

    @Test
    void withTests_MultiplatformPlugin(SettingsExtensionObject settingsSetup) {
        checkSucceeds(settingsSetup, MULTIPLATFORM_PLUGIN)
    }

    private static void checkSucceeds(SettingsExtensionObject settingsSetup, String kotlinPlugin) {
        //arrange
        setupProject(settingsSetup, kotlinPlugin)
        //act
        def result = runGradleModuleBuild(settingsSetup, "build")
        //assert
        Asserts.assertTaskRunSuccessfully(result, ":jacocoTestReport")
        assertFalse(result.output.contains("Execution optimizations have been disabled"), "Execution optimizations have been disabled! maybe due to jacocoTestReport?\n$result.output")
        def reportXml = "build/reports/jacoco/report.xml"
        assertTrue(Files.exists(settingsSetup.tmpPath.resolve(reportXml)), "$reportXml did not exist")
    }


    static def setupProject(SettingsExtensionObject settingsSetup, String kotlinPlugin) throws IOException {
        settingsSetup.settings << "rootProject.name='test-project'"

        def main = new File(settingsSetup.tmp, kotlinPlugin == MULTIPLATFORM_PLUGIN ? "src/jvmMain/kotlin" : "src/main/kotlin")
        main.mkdirs()
        def foo = new File(main, 'test.kt')
        foo << """
            package test
            fun foo(b: Boolean) = if(b) 1 else 2
            """
        def test = new File(settingsSetup.tmp, kotlinPlugin == MULTIPLATFORM_PLUGIN ? "src/jvmTest/kotlin" : "src/test/kotlin")
        test.mkdirs()
        def dummyTest = new File(test, 'DummyTest.kt')
        dummyTest << """
            package test
            import kotlin.test.Test

            class DummyTest {
                @Test
                fun test() {
                    foo(true)
                }
            }
            """
        settingsSetup.buildGradle << """
            ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}

            apply plugin: '$kotlinPlugin'
            apply plugin: 'ch.tutteli.gradle.plugins.junitjacoco'

            repositories {
                mavenCentral()
            }

            ${gradleProjectDependencies(kotlinPlugin)}
            """
    }

    static final def gradleProjectDependencies(String kotlinPlugin) {
        if (kotlinPlugin == MULTIPLATFORM_PLUGIN) {
            return """
            kotlin {
            jvm {
                compilations.all {
                    kotlinOptions.jvmTarget = '1.8'
                }
                testRuns["test"].executionTask.configure {
                    useJUnitPlatform()
                }
            }
            sourceSets {
                commonTest {
                    dependencies {
                        implementation kotlin('test')
                    }
                }
            }
        }

            """
        } else {
            return """
            dependencies {
                testImplementation 'org.jetbrains.kotlin:kotlin-test'
            }
            test {
                useJUnitPlatform()
            }"""
        }
    }

    static def runGradleModuleBuild(SettingsExtensionObject settingsSetup, String... tasks) {
        return GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments(tasks.toList() + ["--stacktrace", "--info"])
            .build()
    }
}
