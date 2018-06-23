package ch.tutteli.gradle.kotlin

import ch.tutteli.gradle.test.Asserts
import ch.tutteli.gradle.test.SettingsExtension
import ch.tutteli.gradle.test.SettingsExtensionObject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import static org.junit.jupiter.api.Assertions.assertFalse

@ExtendWith(SettingsExtension)
class KotlinUtilsPluginIntTest {

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
        apply plugin: 'kotlin-platform-js'
        apply plugin: 'ch.tutteli.kotlin.utils'
        
        repositories{
            mavenCentral()
        }
        
        dependencies {
            compile kotlinStdLib(), withoutKbox
            compile kotlinStdJsLib(), withoutKbox
            compile kotlinStdCommonLib(), withoutKbox
            compile kotlinReflect(), withoutKotlin
        }
        """
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("dependencies")
            .build()
        //assert
        assertFalse(result.output.contains("  \\--- org.jetbrains.kotlin:kotlin-stdlib:"), "kotlin should have been excluded:\n" + result.output)
        assertFalse(result.output.contains("  +--- org.jetbrains.kotlin:kotlin-stdlib:"), "kotlin should have been excluded:\n" + result.output)
        Asserts.assertStatusOk(result, ":dependencies")
    }
}
