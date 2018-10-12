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

    @Test
    void smokeTest(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << """
        rootProject.name='test-project'
        """
        settingsSetup.buildGradle << """
        buildscript {
            dependencies {
                classpath files($settingsSetup.pluginClasspath)
            }
        }
        apply plugin: 'kotlin'
        apply plugin: 'ch.tutteli.spek'
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
                ":junitPlatformTest",
                ":junitPlatformJacocoReport",
                ":check",
                ":build"
            ],
            [":test"],
            [":classes", ":testClasses"]
        )
    }
}
