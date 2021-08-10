package ch.tutteli.gradle.plugins.spek

import ch.tutteli.gradle.plugins.test.Asserts
import ch.tutteli.gradle.plugins.test.SettingsExtension
import ch.tutteli.gradle.plugins.test.SettingsExtensionObject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import static org.junit.jupiter.api.Assertions.assertTrue

@ExtendWith(SettingsExtension)
class SpekPluginIntTest {
    def static final KOTLIN_VERSION = '1.3.61'

    @Test
    void smokeTest_oldKotlin(SettingsExtensionObject settingsSetup) throws IOException {
        checkSmokeTest(settingsSetup, "kotlin")
    }

    @Test
    void smokeTest_OldJvmPlatform(SettingsExtensionObject settingsSetup) throws IOException {
        checkSmokeTest(settingsSetup, "kotlin-platform-jvm")
    }


    @Test
    void smokeTest_JvmPlatform(SettingsExtensionObject settingsSetup) throws IOException {
        checkSmokeTest(settingsSetup, "org.jetbrains.kotlin.jvm")
    }

    private static void checkSmokeTest(SettingsExtensionObject settingsSetup, String kotlinPlugin) {
        //arrange
        settingsSetup.settings << """
        rootProject.name='test-project'
        """
        settingsSetup.buildGradle << """
        ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}
        apply plugin: '$kotlinPlugin'
        apply plugin: 'ch.tutteli.gradle.plugins.spek'
        spek.version = '2.0.15'
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
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("build")
            .build()
        //assert
        assertTrue(result.output.contains("was here"), "println in output:\n" + result.output)
        Asserts.assertStatusOk(result,
            [
                ":inspectClassesForKotlinIC",
                ":jar",
                ":assemble",
                ":compileTestKotlin",
                ":test",
                ":jacocoTestReport",
                ":check",
                ":build"
            ],
            [],
            [":classes", ":testClasses"]
        )
    }
}
