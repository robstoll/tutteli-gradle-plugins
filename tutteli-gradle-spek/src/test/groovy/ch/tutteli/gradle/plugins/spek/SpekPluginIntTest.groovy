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


    @Test
    void smokeTest_OldJvmPlatform(SettingsExtensionObject settingsSetup) throws IOException {
        checkSmokeTest(settingsSetup, "kotlin-platform-jvm", '1.6.21')
    }


    @ParameterizedTest
    @MethodSource("kotlinVersionAndGradle")
    void smokeTest_JvmPlatform(String kotlinVersion, String gradleVersion, SettingsExtensionObject settingsSetup) throws IOException {
        checkSmokeTest(settingsSetup, "org.jetbrains.kotlin.jvm", kotlinVersion, gradleVersion)
    }

    static List<Arguments> kotlinVersionAndGradle() {
        def javaVersion = JavaVersion.toVersion(System.getProperty("java.version"))
        return ['1.3.61', '1.4.10', '1.5.21', '1.6.20', '1.7.20', '1.8.10'].collectMany { kotlinVersion ->
            def gradleVersions = ((javaVersion < JavaVersion.VERSION_15) ? ['6.9.4', '7.6.1'] : ['7.6.1']) + (kotlinVersion.matches("^1\\.[7-9].*\$") ? ['8.0'] : [])
            gradleVersions.collect { gradleVersion ->
                Arguments.of(kotlinVersion, gradleVersion)
            }
        }
    }

    private static void checkSmokeTest(SettingsExtensionObject settingsSetup, String kotlinPlugin, String kotlinVersion, String gradleVersion = null) {
        //arrange
        settingsSetup.settings << """
        rootProject.name='test-project'
        """
        settingsSetup.buildGradle << """
        ${settingsSetup.buildscriptWithKotlin(kotlinVersion)}
        apply plugin: '$kotlinPlugin'
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
        //act
        def builder = GradleRunner.create()
        if (gradleVersion != null) {
            builder = builder.withGradleVersion(gradleVersion)
        }
        def result = builder
            .withProjectDir(settingsSetup.tmp)
            .withArguments("clean", "build")
            .build()
        //assert
        assertTrue(result.output.contains("was here"), "println in output:\n" + result.output)
        def failed = result.taskPaths(TaskOutcome.FAILED)
        assertTrue(failed.empty, 'FAILED is empty but was not: ' + failed)
    }
}
