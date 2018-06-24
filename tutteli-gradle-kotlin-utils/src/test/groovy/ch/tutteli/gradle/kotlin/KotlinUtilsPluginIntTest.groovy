package ch.tutteli.gradle.kotlin

import ch.tutteli.gradle.test.Asserts
import ch.tutteli.gradle.test.SettingsExtension
import ch.tutteli.gradle.test.SettingsExtensionObject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

@ExtendWith(SettingsExtension)
class KotlinUtilsPluginIntTest {

    @Test
    void compileAndWithoutXy(SettingsExtensionObject settingsSetup) throws IOException {
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
        kotlinutils.kotlinVersion = '1.2.40'
        
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
        assertTrue(result.output.contains("\n+--- org.jetbrains.kotlin:kotlin-stdlib:1.2.40"), "should contain stdlib:\n" + result.output)
        assertTrue(result.output.contains("\n+--- org.jetbrains.kotlin:kotlin-stdlib-js:1.2.40"), "should contain stdlib-js:\n" + result.output)
        assertTrue(result.output.contains("\n+--- org.jetbrains.kotlin:kotlin-stdlib-common:1.2.40"), "should contain stdlib-common:\n" + result.output)
        assertTrue(result.output.contains("\n\\--- org.jetbrains.kotlin:kotlin-reflect:1.2.40"), "should contain reflect:\n" + result.output)

        assertFalse(result.output.contains("  \\--- org.jetbrains.kotlin:kotlin-stdlib:"), "stdlib should have been excluded:\n" + result.output)
        assertFalse(result.output.contains("  +--- org.jetbrains.kotlin:kotlin-stdlib:"), "stdlib should have been excluded:\n" + result.output)
        Asserts.assertStatusOk(result, ":dependencies")
    }

    @Test
    void configureCommonProjectsOnly(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << """
        rootProject.name='test-project'
        include 'test1-common'
        include 'test2-common'
        include 'test-js'
        include 'test-jvm'
        """
        File buildGradle = new File(settingsSetup.tmp, 'build.gradle')
        buildGradle << """
        buildscript {
            dependencies {
                classpath files($settingsSetup.pluginClasspath)
            }
        }
        
        apply plugin: 'ch.tutteli.kotlin.utils'
        kotlinutils {
            kotlinVersion = '1.2.40'
        }

        repositories{
            mavenCentral()
        }

        configureCommonProjects()

        """
        //act
        def gradleRunner = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)

        executeDependenciesAndAssertOnlyCommon(gradleRunner, ":test1-common")
        executeDependenciesAndAssertOnlyCommon(gradleRunner, ":test2-common")

        executeDependenciesAndAssertNotExisting(gradleRunner, ":test-js")
        executeDependenciesAndAssertNotExisting(gradleRunner, ":test-jvm")
    }

    private static void executeDependenciesAndAssertOnlyCommon(GradleRunner gradleRunner, String subproject) {
        def result = gradleRunner
            .withArguments(subproject + ":dependencies")
            .build()
        //assert
        assertTrue(result.output.contains("org.jetbrains.kotlin:kotlin-stdlib-common:1.2.40"), "should contain stdlib-common:\n" + result.output)
        assertFalse(result.output.contains("org.jetbrains.kotlin:kotlin-stdlib:"), "stdlib should not be in output:\n" + result.output)
        Asserts.assertStatusOk(result, subproject + ":dependencies")
    }

    private static void executeDependenciesAndAssertNotExisting(GradleRunner gradleRunner, String subproject) {
        try {
            gradleRunner
                .withArguments(subproject + ":dependencies")
                .build()
        } catch (UnexpectedBuildFailure ex) {
            assertTrue(ex.message.contains("Project 'dependencies ' not found in project ':test-js'."))
        }
    }
}
