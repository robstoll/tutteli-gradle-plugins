package ch.tutteli.gradle.plugins.spek

import ch.tutteli.gradle.plugins.test.SettingsExtension
import ch.tutteli.gradle.plugins.test.SettingsExtensionObject
import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource


import static org.junit.jupiter.api.Assertions.assertTrue
import static org.junit.jupiter.api.Assumptions.assumeFalse

@ExtendWith(SettingsExtension)
class SpekPluginIntTest {

    @ParameterizedTest
    @MethodSource("kotlinVersionAndGradle")
    void smokeTest_JvmPlatform(String kotlinVersion, String gradleVersion, SettingsExtensionObject settingsSetup) throws IOException {
        settingsSetup.settings << """
        rootProject.name='test-project'
        """
        settingsSetup.buildGradle << """
        ${settingsSetup.buildscriptWithKotlin(kotlinVersion)}
        apply plugin: 'org.jetbrains.kotlin.jvm'
        apply plugin: 'ch.tutteli.gradle.plugins.spek'
        spek.version = '2.0.15'

        repositories {
            mavenCentral()
        }
        ${settingsSetup.configureTestLogging()}
        """
        File kotlin = new File(settingsSetup.tmp, 'src/test/kotlin/')
        kotlin.mkdirs()
        File spec = new File(kotlin, 'TestSpec.kt')
        spec << """
        import org.spekframework.spek2.Spek
        import org.spekframework.spek2.style.specification.describe

        object TestSpec : Spek({
            describe("dummy test") {
                it("successful test") {
                    println("was here")
                }
            }
        })
        """
        def builder = GradleRunner.create()
        if (gradleVersion != null) {
            builder = builder.withGradleVersion(gradleVersion)
        }
        def result = builder
            .withProjectDir(settingsSetup.tmp)
            .withArguments("clean", "build")
            .build()
        assertTrue(result.output.contains("was here"), "println in output:\n" + result.output)
        def failed = result.taskPaths(TaskOutcome.FAILED)
        assertTrue(failed.empty, 'FAILED is empty but was not: ' + failed)
    }

    static List<Arguments> kotlinVersionAndGradle() {
        return ['1.7.20', '1.8.10', '1.9.10'].collectMany { kotlinVersion ->
            def gradleVersions = ['8.1.1', '8.3']
            gradleVersions.collect { gradleVersion ->
                Arguments.of(kotlinVersion, gradleVersion)
            }
        }
    }

}
