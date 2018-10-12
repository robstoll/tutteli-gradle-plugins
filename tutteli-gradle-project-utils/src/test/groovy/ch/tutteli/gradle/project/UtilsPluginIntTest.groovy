package ch.tutteli.gradle.project

import ch.tutteli.gradle.test.SettingsExtension
import ch.tutteli.gradle.test.SettingsExtensionObject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import static ch.tutteli.gradle.test.Asserts.assertProjectInOutput
import static ch.tutteli.gradle.test.Asserts.assertStatusOk
import static org.junit.jupiter.api.Assertions.assertTrue

@ExtendWith(SettingsExtension)
class UtilsPluginIntTest {

    @Test
    void smokeTest(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        new File(settingsSetup.tmp, 'test-project-one').mkdir()
        settingsSetup.settings << """
        rootProject.name='test-project'
        include 'test-project-one'
        """
        settingsSetup.buildGradle << """
        buildscript {
            dependencies {
                classpath files($settingsSetup.pluginClasspath)
            }
        }
        apply plugin: 'ch.tutteli.project.utils'
        
        println("here we are: \${prefixedProject('one').name}")
        println("another one: \${prefixedProjectName('one')}")
        """
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("projects")
            .build()
        //assert
        assertProjectInOutput(result, ':test-project-one')
        assertTrue(result.output.contains("here we are: test-project-one"), "println `here we are` in output:\n" + result.output)
        assertTrue(result.output.contains("another one: :test-project-one"), "println `another one` in output:\n" + result.output)
        assertStatusOk(result, ":projects")
    }
}
