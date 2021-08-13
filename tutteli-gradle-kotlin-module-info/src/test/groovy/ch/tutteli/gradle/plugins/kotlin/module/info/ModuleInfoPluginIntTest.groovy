package ch.tutteli.gradle.plugins.kotlin.module.info

import ch.tutteli.gradle.plugins.test.Asserts
import ch.tutteli.gradle.plugins.test.SettingsExtension
import ch.tutteli.gradle.plugins.test.SettingsExtensionObject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import static org.junit.jupiter.api.Assertions.*
import static org.junit.jupiter.api.Assumptions.assumeFalse

@ExtendWith(SettingsExtension)
class ModuleInfoPluginIntTest {
    private static final String KOTLIN_VERSION = '1.5.21'
    private static final String ATRIUM_VERSION = '0.16.0'
    private static final String MULTIPLATFORM_PLUGIN = "org.jetbrains.kotlin.multiplatform"

    @Test
    void moduleInfoFails_OldKotlinPlugin(SettingsExtensionObject settingsSetup) {
        checkFails(settingsSetup, "kotlin")
    }

    @Test
    void moduleInfoFails_JvmPlugin(SettingsExtensionObject settingsSetup) {
        checkFails(settingsSetup, "org.jetbrains.kotlin.jvm")
    }

    @Test
    void moduleInfoFailsOldJvmPlatformPlugin(SettingsExtensionObject settingsSetup) {
        checkFails(settingsSetup, "kotlin-platform-jvm")
    }

    @Test
    void moduleInfoFails_MultiplatformPlugin(SettingsExtensionObject settingsSetup) {
        checkFails(settingsSetup, MULTIPLATFORM_PLUGIN, """
            kotlin {
                jvm {
                    withJava()
                }
            }
            """)
    }

    private static void checkFails(SettingsExtensionObject settingsSetup, String kotlinPlugin, String additions = "") {
        //not for jdk8
        assumeFalse(System.getProperty("java.version").startsWith("1.8"))
        //arrange
        setupModuleInfo(settingsSetup, "requires kotlin.stdlib;", kotlinPlugin, additions)
        //act
        def exception = assertThrows(UnexpectedBuildFailure) {
            runGradleModuleBuild(settingsSetup, "jar")
        }

        //assert
        def taskName = kotlinPlugin == MULTIPLATFORM_PLUGIN ? "compileKotlinJvm" : "compileKotlin"
        assertTrue(exception.message.contains("TaskExecutionException: Execution failed for task ':$taskName'"), ":$taskName did not fail.\n$exception.message")
        assertTrue(exception.message.contains("Symbol is declared in module 'ch.tutteli.atrium.verbs'"), "not atrium was the problem.\n$exception.message")
        assertTrue(exception.message.contains("Symbol is declared in module 'ch.tutteli.atrium.api.fluent.en_GB'"), "not atrium-verbs was the problem.\n$exception.message")
    }


    @Test
    void moduleInfoCannotApplyJavaMissingMultiplatformPlugin(SettingsExtensionObject settingsSetup) {
        //not for jdk8
        assumeFalse(System.getProperty("java.version").startsWith("1.8"))
        //arrange
        setupModuleInfo(settingsSetup, "requires kotlin.stdlib;", MULTIPLATFORM_PLUGIN, "")
        //act
        def exception = assertThrows(UnexpectedBuildFailure) {
            runGradleModuleBuild(settingsSetup, "jar")
        }
        assertTrue(exception.message.contains("Looks like the java plugin was not applied. Did you forget to apply the kotlin plugin?"), "did not fail due to missing `withJava")
    }

    @Test
    void moduleInfoSucceeds_OldKotlinPlugin(SettingsExtensionObject settingsSetup) {
        checkSucceeds(settingsSetup, "kotlin")
    }

    @Test
    void moduleInfoSucceeds_KotlinPlugin(SettingsExtensionObject settingsSetup) {
        checkSucceeds(settingsSetup, "org.jetbrains.kotlin.jvm")
    }

    @Test
    void moduleInfoSucceeds_OldJvmPlatformPlugin(SettingsExtensionObject settingsSetup) {
        checkSucceeds(settingsSetup, "kotlin-platform-jvm")
    }

    @Test
    void moduleInfoSucceeds_MultiplatformPlugin(SettingsExtensionObject settingsSetup) {
        checkSucceeds(settingsSetup, MULTIPLATFORM_PLUGIN,
            """
            kotlin {
                jvm {
                    withJava()
                }
            }
            """
        )
    }

    private static void checkSucceeds(SettingsExtensionObject settingsSetup, String kotlinPlugin, String additions = "") {
        //not for jdk8
        assumeFalse(System.getProperty("java.version").startsWith("1.8"))
        //arrange
        setupModuleInfo(settingsSetup, "requires kotlin.stdlib; requires ch.tutteli.atrium.fluent.en_GB;", kotlinPlugin, additions)
        //act
        def result = runGradleModuleBuild(settingsSetup, "build")
        //assert
        Asserts.assertTaskRunSuccessfully(result,":compileJava")
        assertFalse(result.output.contains("Execution optimizations have been disabled"), "Execution optimizations have been disabled! maybe due to module-info?\n$result.output")
    }

    @Test
    void moduleInfoInSubprojectFails(SettingsExtensionObject settingsSetup) throws IOException {
        //not for jdk8
        assumeFalse(System.getProperty("java.version").startsWith("1.8"))
        //arrange
        setupModuleInfoInSubproject(settingsSetup, "requires kotlin.stdlib;")
        //act
        def exception = assertThrows(UnexpectedBuildFailure) {
            runGradleModuleBuild(settingsSetup, "sub1:jar")
        }
        //assert
        assertTrue(exception.message.contains("TaskExecutionException: Execution failed for task ':sub1:compileKotlin'"), ":sub1:compileKotlin did not fail.\n$exception.message")
        assertTrue(exception.message.contains("Symbol is declared in module 'ch.tutteli.atrium.verbs'"), "not atrium was the problem.\n$exception.message")
        assertTrue(exception.message.contains("Symbol is declared in module 'ch.tutteli.atrium.api.fluent.en_GB'"), "not atrium-verbs was the problem.\n$exception.message")
    }

    @Test
    void moduleInfoInSubprojectSucceeds(SettingsExtensionObject settingsSetup) {
        //not for jdk8
        assumeFalse(System.getProperty("java.version").startsWith("1.8"))
        //arrange
        setupModuleInfoInSubproject(settingsSetup, "requires kotlin.stdlib; requires ch.tutteli.atrium.fluent.en_GB;")
        //act
        def result = runGradleModuleBuild(settingsSetup, "sub1:jar")
        //assert
        Asserts.assertTaskRunSuccessfully(result,":sub1:compileJava")
        assertFalse(result.output.contains("Execution optimizations have been disabled"), "Execution optimizations have been disabled! maybe due to module-info?\n$result.output")
    }

    static final def gradleProjectDependencies(String kotlinPlugin) {
        def content = new StringBuilder()
        if (kotlinPlugin == MULTIPLATFORM_PLUGIN) {
            content.append("kotlin { sourceSets { jvmMain {")
        }
        content.append("""
        dependencies {
            implementation "ch.tutteli.atrium:atrium-fluent-en_GB:$ATRIUM_VERSION"
        """)
        if (kotlinPlugin != MULTIPLATFORM_PLUGIN) {
            content.append("""
                constraints {
                    implementation "org.jetbrains.kotlin:kotlin-stdlib:$KOTLIN_VERSION"
                    implementation "org.jetbrains.kotlin:kotlin-reflect:$KOTLIN_VERSION"
                }
            """)
        }
        content.append("}")
        if (kotlinPlugin == MULTIPLATFORM_PLUGIN) {
            content.append("}}}")
        }
        return content.toString()
    }

    static def setupModuleInfo(SettingsExtensionObject settingsSetup, String moduleInfoContent, String kotlinPlugin, String additions) throws IOException {
        settingsSetup.settings << "rootProject.name='test-project'"

        def module = new File(settingsSetup.tmp, kotlinPlugin == MULTIPLATFORM_PLUGIN ? "src/jvmMain/java" : "src/main/java")
        module.mkdirs()
        def moduleInfo = new File(module, 'module-info.java')
        moduleInfo << "module ch.tutteli.test { $moduleInfoContent }"
        def kotlin = new File(settingsSetup.tmp, kotlinPlugin == MULTIPLATFORM_PLUGIN ? "src/jvmMain/kotlin" : "src/main/kotlin")
        kotlin.mkdirs()
        def test = new File(kotlin, 'test.kt')
        test << """
            import ch.tutteli.atrium.api.fluent.en_GB.toBe
            import ch.tutteli.atrium.api.verbs.expect

            fun foo() {
                expect(1).toBe(1)
            }
            """
        settingsSetup.buildGradle << """
            ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}

            apply plugin: '$kotlinPlugin'
            apply plugin: 'ch.tutteli.gradle.plugins.kotlin.module.info'

            repositories {
                mavenCentral()
            }

            $additions

            ${gradleProjectDependencies(kotlinPlugin)}
            """
    }

    static def runGradleModuleBuild(SettingsExtensionObject settingsSetup, String... tasks) {
        return GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments(tasks.toList() + "--stacktrace")
            .build()
    }

    static def setupModuleInfoInSubproject(SettingsExtensionObject settingsSetup, String moduleInfoContent) throws IOException {
        settingsSetup.settings << """
        rootProject.name='test-project'
        include 'sub1'
        """
        def module = new File(settingsSetup.tmp, 'sub1/src/main/java/')
        module.mkdirs()
        def moduleInfo = new File(module, 'module-info.java')
        moduleInfo << "module ch.tutteli.test { $moduleInfoContent }"
        def kotlin = new File(settingsSetup.tmp, 'sub1/src/main/kotlin')
        kotlin.mkdirs()
        def test = new File(kotlin, 'test.kt')
        test << """
            import ch.tutteli.atrium.api.fluent.en_GB.toBe
            import ch.tutteli.atrium.api.verbs.expect

            fun foo() {
                expect(1).toBe(1)
            }
            """
        settingsSetup.buildGradle << """
            ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}

            def sub1 = project(':sub1')
            configure(sub1) {
                apply plugin: 'kotlin-platform-jvm'
                apply plugin: 'ch.tutteli.gradle.plugins.kotlin.module.info'
                repositories {
                    mavenCentral()
                }
                ${gradleProjectDependencies("kotlin")}
            }
            """
    }
}
