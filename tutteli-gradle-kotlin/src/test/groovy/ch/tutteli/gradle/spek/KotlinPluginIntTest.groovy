package ch.tutteli.gradle.spek

import ch.tutteli.gradle.test.Asserts
import ch.tutteli.gradle.test.SettingsExtension
import ch.tutteli.gradle.test.SettingsExtensionObject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import static org.junit.jupiter.api.Assertions.assertFalse

@ExtendWith(SettingsExtension)
class KotlinPluginIntTest {

    @Test
    void smokeTest(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << """
        rootProject.name='test-project'
        """
        File buildGradle = new File(settingsSetup.tmp, 'build.gradle')
        buildGradle << """
        buildscript {
            dependencies {
                classpath files($settingsSetup.pluginClasspath)
            }
        }
        apply plugin: 'ch.tutteli.kotlin'
        
        repositories{
            mavenCentral()
        }
        
        dependencies {
            compile kotlinStdLib(), withoutKbox
            compile kotlinReflect(), withoutKotlin
        }
        """
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("dependencies")
            .build()
        //assert
        assertFalse(result.output.contains("\\--- org.jetbrains.kotlin:kotlin-stdlib"), "kotlin was not excluded:\n" + result.output)
        Asserts.assertStatusOk(result, ":dependencies")
    }
}
