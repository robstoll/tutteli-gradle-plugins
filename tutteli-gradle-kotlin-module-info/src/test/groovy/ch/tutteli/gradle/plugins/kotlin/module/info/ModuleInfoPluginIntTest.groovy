package ch.tutteli.gradle.plugins.kotlin.module.info

import ch.tutteli.gradle.plugins.test.Asserts
import ch.tutteli.gradle.plugins.test.SettingsExtension
import ch.tutteli.gradle.plugins.test.SettingsExtensionObject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import java.nio.file.Files
import java.util.stream.Collectors

import static org.junit.jupiter.api.Assertions.*
import static org.junit.jupiter.api.Assumptions.assumeFalse

@ExtendWith(SettingsExtension)
class ModuleInfoPluginIntTest {
    private static final String KOTLIN_VERSION = '1.5.21'
    private static final String ATRIUM_VERSION = '0.17.0'
    private static final String MULTIPLATFORM_PLUGIN = "org.jetbrains.kotlin.multiplatform"

    @Test
    void moduleInfoFails_JvmPlugin(SettingsExtensionObject settingsSetup) {
        setupPluginAndCheckFails(settingsSetup, "org.jetbrains.kotlin.jvm")
    }

    @Test
    void moduleInfoFails_MultiplatformPlugin(SettingsExtensionObject settingsSetup) {
        setupPluginAndCheckFails(settingsSetup, MULTIPLATFORM_PLUGIN, """
            kotlin {
                jvm {
                    withJava()
                }
            }
            """)
    }

    private static void setupPluginAndCheckFails(SettingsExtensionObject settingsSetup, String kotlinPlugin, String additions = "") {
        //not for jdk8
        assumeFalse(System.getProperty("java.version").startsWith("1.8"))
        //arrange
        setupModuleInfo(settingsSetup, "requires kotlin.stdlib;", kotlinPlugin, additions)
        //act
        checkFails(settingsSetup, kotlinPlugin, "")
    }

    private static void checkFails(SettingsExtensionObject settingsSetup, String kotlinPlugin, String project) {
        def exception = assertThrows(UnexpectedBuildFailure) {
            runGradleModuleBuild(settingsSetup, null, project + ":jar")
        }

        checkDoesNotFailDueToUnnamedModuleBug(settingsSetup, exception)

        //assert
        def taskName = kotlinPlugin == MULTIPLATFORM_PLUGIN ? "compileKotlinJvm" : "compileKotlin"
        assertTrue(exception.message.contains("TaskExecutionException: Execution failed for task '$project:$taskName'"), "$project:$taskName did not fail.\n$exception.message")
        assertTrue(exception.message.contains("Symbol is declared in module 'ch.tutteli.atrium.verbs'"), "not atrium was the problem.\n$exception.message")
        assertTrue(exception.message.contains("Symbol is declared in module 'ch.tutteli.atrium.api.fluent.en_GB'"), "not atrium-verbs was the problem.\n$exception.message")
    }

    private static void checkDoesNotFailDueToUnnamedModuleBug(SettingsExtensionObject settingsSetup, UnexpectedBuildFailure exception) {
        if (exception.message.contains("Symbol is declared in unnamed module")) {
            def moduleInfo = settingsSetup.tmpPath.resolve("src/main/java/module-info.java")

            if (!Files.exists(moduleInfo)) {
                moduleInfo = settingsSetup.tmpPath.resolve("sub1/src/main/java/module-info.java")
            }
            println("unnamed module bug detected, following the content of the tmp folder:\n" +
                "- ${Files.list(settingsSetup.tmpPath).collect(Collectors.toList()).join("\n- ")}\n" +
                "\n" +
                "===================================================\n" +
                "Content of module-info.java\n" +
                "${moduleInfo.text}"
            )
            throw exception
        }
    }

    @Test
    void moduleInfoCannotApplyJavaMissingMultiplatformPlugin(SettingsExtensionObject settingsSetup) {
        //not for jdk8
        assumeFalse(System.getProperty("java.version").startsWith("1.8"))
        //arrange
        setupModuleInfo(settingsSetup, "requires kotlin.stdlib;", MULTIPLATFORM_PLUGIN, "")
        //act
        def exception = assertThrows(UnexpectedBuildFailure) {
            runGradleModuleBuild(settingsSetup, null, "jar")
        }
        assertTrue(exception.message.contains("Looks like the java plugin was not applied. Did you forget to apply the kotlin plugin?"), "did not fail due to missing `withJava")
    }

    @Test
    void moduleInfoSucceeds_KotlinPlugin(SettingsExtensionObject settingsSetup) {
        checkSucceeds(settingsSetup, null, "org.jetbrains.kotlin.jvm")
    }

    @Test
    void moduleInfoSucceeds_MultiplatformPlugin(SettingsExtensionObject settingsSetup) {
        checkSucceeds(settingsSetup, null, MULTIPLATFORM_PLUGIN,
            """
            kotlin {
                jvm {
                    withJava()
                }
            }
            """
        )
    }

    @Test
    void moduleInfoSucceeds_MultiplatformPlugin_Gradle6_9_3(SettingsExtensionObject settingsSetup) {
        checkSucceeds(settingsSetup, "6.9.3", MULTIPLATFORM_PLUGIN,
            """
            kotlin {
                jvm {
                    withJava()
                }
            }
            """
        )
    }

    private static void checkSucceeds(SettingsExtensionObject settingsSetup, String gradleVersion, String kotlinPlugin, String additions = "") {
        //not for jdk8
        assumeFalse(System.getProperty("java.version").startsWith("1.8"))
        //arrange
        setupModuleInfo(settingsSetup, "requires kotlin.stdlib; requires ch.tutteli.atrium.fluent.en_GB;", kotlinPlugin, additions)
        //act
        try {
            def result = runGradleModuleBuild(settingsSetup, gradleVersion, "build")
            //assert
            Asserts.assertTaskRunSuccessfully(result, ":compileJava")
            assertFalse(result.output.contains("Execution optimizations have been disabled"), "Execution optimizations have been disabled! maybe due to module-info?\n$result.output")
        } catch (UnexpectedBuildFailure ex) {
            checkDoesNotFailDueToUnnamedModuleBug(settingsSetup, ex)
            throw ex
        }
    }

    @Test
    void moduleInfoInSubprojectMppFails(SettingsExtensionObject settingsSetup) throws IOException {
        //not for jdk8
        assumeFalse(System.getProperty("java.version").startsWith("1.8"))
        //arrange
        setupModuleInfoInSubproject(settingsSetup, "requires kotlin.stdlib;")
        //act
        checkFails(settingsSetup, "kotlin-platform-jvm", ":sub1")
    }

    @Test
    void moduleInfoInSubprojectSucceeds(SettingsExtensionObject settingsSetup) {
        //not for jdk8
        assumeFalse(System.getProperty("java.version").startsWith("1.8"))
        //arrange
        setupModuleInfoInSubproject(settingsSetup, "requires kotlin.stdlib; requires ch.tutteli.atrium.fluent.en_GB;")
        try {
            //act
            def result = runGradleModuleBuild(settingsSetup, null, "sub1:jar")
            //assert
            Asserts.assertTaskRunSuccessfully(result, ":sub1:compileJava")
            assertFalse(result.output.contains("Execution optimizations have been disabled"), "Execution optimizations have been disabled! maybe due to module-info?\n$result.output")
        } catch (UnexpectedBuildFailure ex) {
            checkDoesNotFailDueToUnnamedModuleBug(settingsSetup, ex)
            throw ex
        }
    }

    static final def gradleProjectDependencies(String kotlinPlugin) {
        def configuration = kotlinPlugin == MULTIPLATFORM_PLUGIN ? "jvmMainImplementation" : "implementation"
        return """
        dependencies {
            $configuration "ch.tutteli.atrium:atrium-fluent-en_GB:$ATRIUM_VERSION"
            constraints {
                implementation "org.jetbrains.kotlin:kotlin-stdlib:$KOTLIN_VERSION"
                implementation "org.jetbrains.kotlin:kotlin-reflect:$KOTLIN_VERSION"
            }
        }"""
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

            project.version = '1.2.3'

            $additions

            ${gradleProjectDependencies(kotlinPlugin)}
            """
    }

    static def runGradleModuleBuild(SettingsExtensionObject settingsSetup, String gradleVersion, String... tasks) {
        def builder = GradleRunner.create()
        if (gradleVersion != null) {
            builder = builder.withGradleVersion(gradleVersion)
        }
        return builder
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

            project.version = '1.2.3'

            def sub1 = project(':sub1')
            configure(sub1) {
                apply plugin: 'org.jetbrains.kotlin.jvm'
                apply plugin: 'ch.tutteli.gradle.plugins.kotlin.module.info'
                repositories {
                    mavenCentral()
                }
                ${gradleProjectDependencies("kotlin")}
            }
            """
    }
}
