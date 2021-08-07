package ch.tutteli.gradle.plugins.project

import ch.tutteli.gradle.plugins.test.SettingsExtension
import ch.tutteli.gradle.plugins.test.SettingsExtensionObject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import static ch.tutteli.gradle.plugins.test.Asserts.assertProjectInOutput
import static ch.tutteli.gradle.plugins.test.Asserts.assertStatusOk
import static org.junit.jupiter.api.Assertions.assertTrue

@ExtendWith(SettingsExtension)
class UtilsPluginIntTest {
    def static final KOTLIN_VERSION = '1.3.61'

    @Test
    void smokeTest(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        new File(settingsSetup.tmp, 'test-project-one').mkdir()
        settingsSetup.settings << """
        rootProject.name='test-project'
        include 'test-project-one'
        """
        settingsSetup.buildGradle << """
            ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}
           apply plugin: 'ch.tutteli.gradle.plugins.project.utils'

            println("here we are: \${prefixedProject('one').name}")
            println("another one: \${prefixedProjectName('one')}")

            subprojects {
                apply plugin: 'kotlin'
                createTestJarTask(it)
            }
            """
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("projects", "--stacktrace")
            .build()
        //assert
        assertProjectInOutput(result, ':test-project-one')
        assertTrue(result.output.contains("here we are: test-project-one"), "println `here we are` in output:\n" + result.output)
        assertTrue(result.output.contains("another one: :test-project-one"), "println `another one` in output:\n" + result.output)
        assertStatusOk(result, ":projects")
    }
}
