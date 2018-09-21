package ch.tutteli.gradle.kotlin

import ch.tutteli.gradle.test.Asserts
import ch.tutteli.gradle.test.SettingsExtension
import ch.tutteli.gradle.test.SettingsExtensionObject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import java.util.regex.Matcher

import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

@ExtendWith(SettingsExtension)
class KotlinUtilsPluginIntTest {

    private static final String KOTLIN_VERSION = '1.2.40'
    private static final String settingsFileContent = """
        rootProject.name='test-project'
        include 'test1-common'
        include 'test2-common'
        include 'test1-js'
        include 'test2-js'
        include 'test1-jvm'
        include 'test2-jvm'
        """

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
        kotlinutils.kotlinVersion = '$KOTLIN_VERSION'
        
        repositories{
            mavenCentral()
        }
        
        dependencies {
            compile kotlinStdlib(), withoutKbox
            compile kotlinStdlibJs(), withoutKbox
            compile kotlinStdlibCommon(), withoutKbox
            compile kotlinReflect(), withoutKotlin
        }
        """
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("dependencies")
            .build()
        //assert
        assertTrue(result.output.contains("\n+--- org.jetbrains.kotlin:kotlin-stdlib:$KOTLIN_VERSION"), "should contain stdlib:\n" + result.output)
        assertTrue(result.output.contains("\n+--- org.jetbrains.kotlin:kotlin-stdlib-js:$KOTLIN_VERSION"), "should contain stdlib-js:\n" + result.output)
        assertTrue(result.output.contains("\n+--- org.jetbrains.kotlin:kotlin-stdlib-common:$KOTLIN_VERSION"), "should contain stdlib-common:\n" + result.output)
        assertTrue(result.output.contains("\n\\--- org.jetbrains.kotlin:kotlin-reflect:$KOTLIN_VERSION"), "should contain reflect:\n" + result.output)

        def matcher = result.output =~ /(compile|default|runtime)[\S\s]+?\--- org.jetbrains.kotlin:kotlin-reflect:$KOTLIN_VERSION\r?\n\s*\\--- org.jetbrains.kotlin:kotlin-stdlib:/
        assertFalse(matcher.find(), "stdlib should have been excluded:\n" + result.output)
        Asserts.assertStatusOk(result, ":dependencies")
    }

    @Test
    void configureCommonProjectsOnly(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << settingsFileContent
        File buildGradle = new File(settingsSetup.tmp, 'build.gradle')
        buildGradle << headerBuildFile(settingsSetup) + "configureCommonProjects()"
        //act
        def gradleRunner = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)

        executeDependenciesAndAssertOnlyCommon(gradleRunner, ":test1-common")
        executeDependenciesAndAssertOnlyCommon(gradleRunner, ":test2-common")

        executeDependenciesAndAssertNotExisting(gradleRunner, ":test1-js")
        executeDependenciesAndAssertNotExisting(gradleRunner, ":test2-js")
        executeDependenciesAndAssertNotExisting(gradleRunner, ":test1-jvm")
        executeDependenciesAndAssertNotExisting(gradleRunner, ":test2-jvm")
    }

    @Test
    void configureCommonAndJsProjects(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << settingsFileContent
        File buildGradle = new File(settingsSetup.tmp, 'build.gradle')
        buildGradle << headerBuildFile(settingsSetup) + "configureCommonProjects()\n configureJsProjects()"
        //act
        def gradleRunner = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)

        executeDependenciesAndAssertOnlyCommon(gradleRunner, ":test1-common")
        executeDependenciesAndAssertOnlyCommon(gradleRunner, ":test2-common")
        executeDependenciesAndAssertCommonAndJs(gradleRunner, ":test1")
        executeDependenciesAndAssertCommonAndJs(gradleRunner, ":test2")

        executeDependenciesAndAssertNotExisting(gradleRunner, ":test1-jvm")
        executeDependenciesAndAssertNotExisting(gradleRunner, ":test2-jvm")
    }

    @Test
    void configureCommonAndJvmProjects(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << settingsFileContent
        File buildGradle = new File(settingsSetup.tmp, 'build.gradle')
        buildGradle << headerBuildFile(settingsSetup) + "configureCommonProjects()\n configureJvmProjects()"
        //act
        def gradleRunner = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)

        executeDependenciesAndAssertOnlyCommon(gradleRunner, ":test1-common")
        executeDependenciesAndAssertOnlyCommon(gradleRunner, ":test2-common")
        executeDependenciesAndAssertCommonAndJvm(gradleRunner, ":test1")
        executeDependenciesAndAssertCommonAndJvm(gradleRunner, ":test2")

        executeDependenciesAndAssertNotExisting(gradleRunner, ":test1-js")
        executeDependenciesAndAssertNotExisting(gradleRunner, ":test2-js")
    }

    @Test
    void configureCommonJsAndJvmProjects(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << settingsFileContent
        File buildGradle = new File(settingsSetup.tmp, 'build.gradle')
        buildGradle << headerBuildFile(settingsSetup) + "configureCommonProjects()\n configureJsProjects() \n configureJvmProjects()"
        //act
        def gradleRunner = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)


        executeDependenciesAndAssertOnlyCommon(gradleRunner, ":test1-common")
        executeDependenciesAndAssertOnlyCommon(gradleRunner, ":test2-common")
        executeDependenciesAndAssertCommonAndJs(gradleRunner, ":test1")
        executeDependenciesAndAssertCommonAndJs(gradleRunner, ":test2")
        executeDependenciesAndAssertCommonAndJvm(gradleRunner, ":test1")
        executeDependenciesAndAssertCommonAndJvm(gradleRunner, ":test2")
    }

    private static GString headerBuildFile(SettingsExtensionObject settingsSetup) {
        def headerBuildFile = """
        buildscript {
            dependencies {
                classpath files($settingsSetup.pluginClasspath)
            }
        }
        
        apply plugin: 'ch.tutteli.kotlin.utils'
        kotlinutils {
            kotlinVersion = '$KOTLIN_VERSION'
        }

        repositories{
            mavenCentral()
        }

        """
        headerBuildFile
    }

    private static void executeDependenciesAndAssertOnlyCommon(GradleRunner gradleRunner, String subproject) {
        def result = gradleRunner
            .withArguments(subproject + ":dependencies")
            .build()
        //assert
        assertTrue(result.output.contains("\n\\--- org.jetbrains.kotlin:kotlin-stdlib-common:$KOTLIN_VERSION"), "should contain stdlib-common:\n" + result.output)
        assertFalse(result.output.contains("org.jetbrains.kotlin:kotlin-stdlib:"), "stdlib should not be in output:\n" + result.output)
        assertFalse(result.output.contains("org.jetbrains.kotlin:kotlin-stdlib-js:"), "stdlib-js should not be in output:\n" + result.output)
        Asserts.assertStatusOk(result, subproject + ":dependencies")
    }

    private static void executeDependenciesAndAssertCommonAndJs(GradleRunner gradleRunner, String prefix) {
        executeDependenciesAndAssertCommonAnd(gradleRunner, prefix, "-js", "stdlib-js", "stdlib")
    }

    private static void executeDependenciesAndAssertCommonAndJvm(GradleRunner gradleRunner, String prefix) {
        executeDependenciesAndAssertCommonAnd(gradleRunner, prefix, "-jvm", "stdlib", "stdlib-js")
    }

    private static void executeDependenciesAndAssertCommonAnd(
        GradleRunner gradleRunner,
        String prefix,
        String suffix,
        String libInThere,
        String libNotInThere
    ) {
        //act
        def result = gradleRunner
            .withArguments(prefix + suffix + ":dependencies")
            .build()
        //assert
        assertTrue(result.output.contains("\n+--- org.jetbrains.kotlin:kotlin-" + libInThere + ":$KOTLIN_VERSION"), "should contain $libInThere:\n" + result.output)
        assertTrue(result.output.contains("\n\\--- project $prefix-common"), "should contain project $prefix-common:\n" + result.output)
        assertTrue(result.output.contains("  \\--- org.jetbrains.kotlin:kotlin-stdlib-common:$KOTLIN_VERSION"), "should contain stdlib-common:\n" + result.output)
        assertFalse(result.output.contains("org.jetbrains.kotlin:kotlin-" + libNotInThere + ":"), "$libNotInThere should not be in output:\n" + result.output)

        Asserts.assertStatusOk(result, prefix + suffix + ":dependencies")
    }

    private static void executeDependenciesAndAssertNotExisting(GradleRunner gradleRunner, String subproject) {
        try {
            gradleRunner
                .withArguments(subproject + ":dependencies")
                .build()
        } catch (UnexpectedBuildFailure ex) {
            assertTrue(ex.message.contains("Project 'dependencies ' not found in project '$subproject'."))
        }
    }
}
