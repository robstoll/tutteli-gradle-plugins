package ch.tutteli.gradle.plugins.kotlin.module.info

import ch.tutteli.gradle.plugins.test.Asserts
import ch.tutteli.gradle.plugins.test.SettingsExtension
import ch.tutteli.gradle.plugins.test.SettingsExtensionObject
import org.gradle.api.JavaVersion
import org.gradle.internal.impldep.com.google.common.reflect.Types
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

import static org.junit.jupiter.api.Assertions.*
import static org.junit.jupiter.api.Assumptions.assumeFalse

@ExtendWith(SettingsExtension)
class ModuleInfoPluginIntTest {
    private static final String KOTLIN_VERSION = '2.0.21'
    private static final String ATRIUM_VERSION = '1.0.0'
    private static final String MULTIPLATFORM_PLUGIN = "org.jetbrains.kotlin.multiplatform"

    @Test
    void moduleInfoFails_JvmPlugin(SettingsExtensionObject settingsSetup) {
        setupPluginAndCheckFails("kotlin-jvm-fails", settingsSetup, "org.jetbrains.kotlin.jvm")
    }

    @Test
    void moduleInfoFails_MultiplatformPlugin(SettingsExtensionObject settingsSetup) {
        setupPluginAndCheckFails("kotlin-mpp-fails", settingsSetup, MULTIPLATFORM_PLUGIN, """
            kotlin {
                jvm {
                    withJava()
                }
            }
            """)
    }

    private static void setupPluginAndCheckFails(String projectName, SettingsExtensionObject settingsSetup, String kotlinPlugin, String additions = "") {
        //arrange
        setupModuleInfo(projectName, settingsSetup, "requires kotlin.stdlib;", kotlinPlugin, KOTLIN_VERSION, additions)
        //act
        checkFails(settingsSetup, kotlinPlugin, "")
    }

    private static void checkFails(SettingsExtensionObject settingsSetup, String kotlinPlugin, String project) {
        def isMpp = kotlinPlugin == MULTIPLATFORM_PLUGIN
        def exception = assertThrows(UnexpectedBuildFailure) {
            runGradleModuleBuild(settingsSetup, null, project + (isMpp ? ":jvmJar" : ":jar"))
        }

        checkDoesNotFailDueToUnnamedModuleBug(settingsSetup, exception)

        //assert
        def taskName = isMpp ? "compileKotlinJvm" : "compileKotlin"
        assertTrue(exception.message.contains("TaskExecutionException: Execution failed for task '$project:$taskName'"), "$project:$taskName did not fail.\n$exception.message")
        assertTrue(exception.message.contains("Symbol is declared in module 'ch.tutteli.atrium.verbs'"), "not atrium was the problem.\n$exception.message")
        assertTrue(exception.message.contains("Symbol is declared in module 'ch.tutteli.atrium.api.fluent.en_GB'"), "not atrium-verbs was the problem.\n$exception.message")
    }

    private static void checkDoesNotFailDueToUnnamedModuleBug(SettingsExtensionObject settingsSetup, UnexpectedBuildFailure exception) {
        if (exception.message.contains("Symbol is declared in unnamed module") || exception.message.contains("The Kotlin standard library is not found in the module graph")) {
            def javaSrcDir = settingsSetup.tmpPath.resolve("src/main/java")
            if (!Files.exists(javaSrcDir)) {
                javaSrcDir = settingsSetup.tmpPath.resolve("sub1/src/main/java")
            }
            if (!Files.exists(javaSrcDir)) {
                javaSrcDir = settingsSetup.tmpPath.resolve("src/jvmMain/java")
            }
            String moduleInfoContent = null
            if (Files.exists(javaSrcDir)) {
                def moduleInfo = javaSrcDir.resolve("module-info.java")
                moduleInfoContent = moduleInfo.text
            }
            if (moduleInfoContent == null) {
                moduleInfoContent = "looks like java src dir or moduleInfo does not exist."
            }

            println("unnamed module bug detected, following the content of the tmp folder:")
            Files.walkFileTree(settingsSetup.tmpPath, new SimpleFileVisitor<Path>() {

                @Override
                FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    println(file.toAbsolutePath().toString())
                    return FileVisitResult.CONTINUE
                }

                @Override
                FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE
                }

                @Override
                FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE
                }
            })
            println("\n" +
                "===================================================\n" +
                "Content of module-info.java:\n" +
                "${moduleInfoContent}"
            )
            def kotlinSrcDir = settingsSetup.tmpPath.resolve("src/main/kotlin")
            if (!Files.exists(kotlinSrcDir)) {
                kotlinSrcDir = settingsSetup.tmpPath.resolve("sub1/src/main/kotlin")
            }
            if (!Files.exists(kotlinSrcDir)) {
                kotlinSrcDir = settingsSetup.tmpPath.resolve("src/jvmMain/kotlin")
            }
            String testKtContent = null
            if (Files.exists(kotlinSrcDir)) {
                def moduleInfo = kotlinSrcDir.resolve("test.kt")
                testKtContent = moduleInfo.text
            }
            println("\n" +
                "===================================================\n" +
                "Content of src/main/kotlin/test.kt:\n" +
                "${testKtContent}"
            )
            throw exception
        }
    }

    @Test
    void moduleInfoCannotApplyJavaMissingMultiplatformPlugin(SettingsExtensionObject settingsSetup) {
        //arrange
        setupModuleInfo("mpp-missing-kotlin", settingsSetup, "requires kotlin.stdlib;", MULTIPLATFORM_PLUGIN, KOTLIN_VERSION, "")
        //act
        def exception = assertThrows(UnexpectedBuildFailure) {
            runGradleModuleBuild(settingsSetup, null, "jar")
        }
        assertTrue(exception.message.contains("There is no compileJava task. Did you forget to apply the kotlin plugin?"), "did not fail due to missing `withJava")
    }

    @Test
    void moduleInfoSucceeds_KotlinPlugin(SettingsExtensionObject settingsSetup) {
        checkSucceeds("kotlin-jvm-success", settingsSetup, null, "org.jetbrains.kotlin.jvm")
    }

    @Test
    void moduleInfoSucceeds_KotlinPlugin_1_8_21(SettingsExtensionObject settingsSetup) {
        checkSucceeds("kotlin-jvm-success", settingsSetup, null, "org.jetbrains.kotlin.jvm", "1.8.21")
    }

    @Test
    void moduleInfoSucceeds_MultiplatformPlugin(SettingsExtensionObject settingsSetup) {
        checkSucceeds("kotlin-mpp-success", settingsSetup, null, MULTIPLATFORM_PLUGIN, KOTLIN_VERSION,
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
    void moduleInfoSucceeds_MultiplatformPlugin_kotlin_1_8_21(SettingsExtensionObject settingsSetup) {
        checkSucceeds("kotlin-mpp-success", settingsSetup, null, MULTIPLATFORM_PLUGIN, "1.8.21",
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
        def javaVersion = JavaVersion.toVersion(System.getProperty("java.version"))
        assumeFalse(javaVersion >= JavaVersion.VERSION_15)
        checkSucceeds("mpp-6.9.3-success", settingsSetup, "6.9.3", MULTIPLATFORM_PLUGIN, KOTLIN_VERSION,
            """
            kotlin {
                jvm {
                    withJava()
                }
            }
            """
        )
    }

    private static void checkSucceeds(
        String projectName,
        SettingsExtensionObject settingsSetup,
        String gradleVersion,
        String kotlinPlugin,
        String kotlinVersion = KOTLIN_VERSION,
        String additions = ""
    ) {
        //arrange
        setupModuleInfo(projectName, settingsSetup, "requires kotlin.stdlib; requires ch.tutteli.atrium.fluent.en_GB;", kotlinPlugin, kotlinVersion, additions)
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

        //arrange
        setupModuleInfoInSubproject("sub-jvm-fails", settingsSetup, "requires kotlin.stdlib;")
        //act
        checkFails(settingsSetup, "kotlin-platform-jvm", ":sub1")
    }

    @Test
    void moduleInfoInSubprojectSucceeds(SettingsExtensionObject settingsSetup) {

        //arrange
        setupModuleInfoInSubproject("sub-jvm-success", settingsSetup, "requires kotlin.stdlib; requires ch.tutteli.atrium.fluent.en_GB;")
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

    @Test
    void moduleInfoInSubproject_gradle6x_Succeeds(SettingsExtensionObject settingsSetup) {
        //not for jdk8, and gradle 6.9.3 is not compatible with
        def javaVersion = JavaVersion.toVersion(System.getProperty("java.version"))
        assumeFalse(javaVersion <= JavaVersion.VERSION_1_8)
        assumeFalse(javaVersion >= JavaVersion.VERSION_15)
        //arrange
        setupModuleInfoInSubproject("sub-jvm-6.9.3-success", settingsSetup, "requires kotlin.stdlib; requires ch.tutteli.atrium.fluent.en_GB;")
        try {
            //act
            def result = runGradleModuleBuild(settingsSetup, "6.9.3", "sub1:jar")
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
            $configuration "ch.tutteli.atrium:atrium-fluent:$ATRIUM_VERSION"
            constraints {
                implementation "org.jetbrains.kotlin:kotlin-stdlib:$KOTLIN_VERSION"
                implementation "org.jetbrains.kotlin:kotlin-reflect:$KOTLIN_VERSION"
            }
        }"""
    }

    static def setupModuleInfo(
        String projectName,
        SettingsExtensionObject settingsSetup,
        String moduleInfoContent,
        String kotlinPlugin,
        String kotlinVersion,
        String additions
    ) throws IOException {
        settingsSetup.settings << "rootProject.name='$projectName'"

        def module = new File(settingsSetup.tmp, kotlinPlugin == MULTIPLATFORM_PLUGIN ? "src/jvmMain/java" : "src/main/java")
        module.mkdirs()
        def moduleInfo = new File(module, 'module-info.java')
        def moduleInfoId = (projectName.replace(".", "_") + "_" + UUID.randomUUID().toString()).replace('-', '_')
        moduleInfo << "module ch.tutteli.$moduleInfoId { $moduleInfoContent }"
        def kotlin = new File(settingsSetup.tmp, kotlinPlugin == MULTIPLATFORM_PLUGIN ? "src/jvmMain/kotlin" : "src/main/kotlin")
        kotlin.mkdirs()
        def test = new File(kotlin, 'test.kt')
        test << """
            import ch.tutteli.atrium.api.fluent.en_GB.toEqual
            import ch.tutteli.atrium.api.verbs.expect

            fun foo() {
                expect(1).toEqual(1)
            }
            """
        settingsSetup.buildGradle << """
            ${settingsSetup.buildscriptWithKotlin(kotlinVersion)}

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

    static def setupModuleInfoInSubproject(String projectName, SettingsExtensionObject settingsSetup, String moduleInfoContent) throws IOException {
        settingsSetup.settings << """
        rootProject.name='$projectName'
        include 'sub1'
        """
        def module = new File(settingsSetup.tmp, 'sub1/src/main/java/')
        module.mkdirs()
        def moduleInfo = new File(module, 'module-info.java')
        def moduleInfoId = (projectName.replace(".", "_") + "_" + UUID.randomUUID().toString()).replace('-', '_')
        moduleInfo << "module ch.tutteli.${moduleInfoId} { $moduleInfoContent }"
        def kotlin = new File(settingsSetup.tmp, 'sub1/src/main/kotlin')
        kotlin.mkdirs()
        def test = new File(kotlin, 'test.kt')
        test << """
            import ch.tutteli.atrium.api.fluent.en_GB.toEqual
            import ch.tutteli.atrium.api.verbs.expect

            fun foo() {
                expect(1).toEqual(1)
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

    @Test
    void worksIfOneHasAppliedJavaToolchain(SettingsExtensionObject settingsSetup) {
        def projectName = "java-toolchain"
        settingsSetup.settings << "rootProject.name='$projectName'"
        settingsSetup.buildGradle << """
            ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}

            apply plugin: 'org.jetbrains.kotlin.jvm'
            apply plugin: 'ch.tutteli.gradle.plugins.kotlin.module.info'

            repositories {
                mavenCentral()
            }

            project.version = '1.2.3'

            java {
                toolchain {
                     languageVersion.set(JavaLanguageVersion.of(11))
                }
            }
            """
        def module = new File(settingsSetup.tmp, 'src/main/java/')
        module.mkdirs()
        def moduleInfo = new File(module, 'module-info.java')
        def moduleInfoId = (projectName.replace(".", "_") + "_" + UUID.randomUUID().toString()).replace('-', '_')
        moduleInfo << "module ch.tutteli.$moduleInfoId { requires java.base; }"
        def result = runGradleModuleBuild(settingsSetup, null, "build")
        //assert
        Asserts.assertTaskRunSuccessfully(result, ":compileJava")
    }


}
