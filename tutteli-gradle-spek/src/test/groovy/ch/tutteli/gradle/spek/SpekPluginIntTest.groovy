package ch.tutteli.gradle.spek

import ch.tutteli.gradle.test.Asserts
import ch.tutteli.gradle.test.SettingsExtension
import ch.tutteli.gradle.test.SettingsExtensionObject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import static org.junit.jupiter.api.Assertions.assertTrue

@ExtendWith(SettingsExtension)
class SpekPluginIntTest {
    def static final KOTLIN_VERSION = '1.3.61'

    @Test
    void smokeTestVersion1(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << """
        rootProject.name='test-project'
        """
        settingsSetup.buildGradle << """
        ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}
        apply plugin: 'kotlin'
        apply plugin: 'ch.tutteli.spek'
        spek.version = '1.1.5'
        ${settingsSetup.configureTestLogging()}
        """
        File kotlin = new File(settingsSetup.tmp, 'src/test/kotlin/')
        kotlin.mkdirs()
        File spec = new File(kotlin, 'TestSpec.kt')
        spec << """
        import org.jetbrains.spek.api.Spek
        import org.jetbrains.spek.api.dsl.it

        object TestSpec : Spek({
            it("successful test") {
                println("was here")
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

    @Test
    void smokeTestVersion2(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << """
        rootProject.name='test-project'
        """
        settingsSetup.buildGradle << """
        ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}
        apply plugin: 'kotlin'
        apply plugin: 'ch.tutteli.spek'
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
