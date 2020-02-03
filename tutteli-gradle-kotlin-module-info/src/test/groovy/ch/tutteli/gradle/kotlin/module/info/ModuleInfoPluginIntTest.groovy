package ch.tutteli.gradle.kotlin.module.info

import ch.tutteli.gradle.test.Asserts
import ch.tutteli.gradle.test.SettingsExtension
import ch.tutteli.gradle.test.SettingsExtensionObject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.core.Every.everyItem
import static org.hamcrest.core.IsEqual.equalTo
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue
import static org.junit.jupiter.api.Assumptions.assumeFalse
import static com.jayway.jsonpath.matchers.JsonPathMatchers.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue

@ExtendWith(SettingsExtension)
class ModuleInfoPluginIntTest {
    private static final String KOTLIN_VERSION = '1.3.30'
    private static final String ATRIUM_VERSION = '0.7.0'

    @Test
    void moduleInfoFails(SettingsExtensionObject settingsSetup) {
        //not for jdk8
        assumeFalse(System.getProperty("java.version").startsWith("1.8"))
        //act
        setupModuleInfo(settingsSetup, "requires kotlin.stdlib;")
        def exception = assertThrows(UnexpectedBuildFailure) {
            runGradleModuleBuild(settingsSetup, "jar")
        }
        //assert
        assertTrue(exception.message.contains("TaskExecutionException: Execution failed for task ':compileKotlin'"), ":compileKotlin did not fail.\n$exception.message")
        assertTrue(exception.message.contains('Unresolved reference: atrium'), "not atrium was the problem.\n$exception.message")
    }

    @Test
    void moduleInfoSucceeds(SettingsExtensionObject settingsSetup) {
        //not for jdk8
        assumeFalse(System.getProperty("java.version").startsWith("1.8"))
        //act
        setupModuleInfo(settingsSetup, "requires kotlin.stdlib; requires ch.tutteli.atrium.bundle.cc.en_GB.robstoll;")
        def result = runGradleModuleBuild(settingsSetup, "jar")
        //assert
        Asserts.assertStatusOk(result, [':compileKotlin', ':compileModuleKotlin', ':compileModuleJava', ':inspectClassesForKotlinIC', ':jar'], [], [':classes'])
        assertTrue(result.output.contains("source set 'module'"), "should contain source set 'module':\n$result.output")
    }

    @Test
    void moduleInfoInSubprojectFails(SettingsExtensionObject settingsSetup) throws IOException {
        //not for jdk8
        assumeFalse(System.getProperty("java.version").startsWith("1.8"))
        //act
        setupModuleInfoInSubproject(settingsSetup, "requires kotlin.stdlib;")
        def exception = assertThrows(UnexpectedBuildFailure) {
            runGradleModuleBuild(settingsSetup, "sub1:jar")
        }
        //assert
        assertTrue(exception.message.contains("TaskExecutionException: Execution failed for task ':sub1:compileKotlin'"), ":sub1:compileKotlin did not fail.\n$exception.message")
        assertTrue(exception.message.contains('Unresolved reference: atrium'), "not atrium was the problem.\n$exception.message")
    }

    @Test
    void moduleInfoInSubprojectSucceeds(SettingsExtensionObject settingsSetup) {
        //not for jdk8
        assumeFalse(System.getProperty("java.version").startsWith("1.8"))
        //act
        setupModuleInfoInSubproject(settingsSetup, "requires kotlin.stdlib; requires ch.tutteli.atrium.bundle.cc.en_GB.robstoll;")
        def result = runGradleModuleBuild(settingsSetup, "sub1:jar")
        //assert
        Asserts.assertStatusOk(result, [':sub1:compileKotlin', ':sub1:compileModuleKotlin', ':sub1:compileModuleJava', ':sub1:inspectClassesForKotlinIC', ':sub1:jar'], [], [':sub1:classes'])
        assertTrue(result.output.contains("root has no sourceSets"), "root should not have sourceSets:\n$result.output")
        assertTrue(result.output.contains("sub1: source set 'module'"), "should contain sub1 source set 'module':\n$result.output")
    }

    @Test
    void compatibleToJava8(SettingsExtensionObject settingsSetup) {
        //only for jdk8
        assumeFalse(System.getProperty("java.version").startsWith("1.8"))
        setupModuleInfo(settingsSetup, "requires kotlin.stdlib; requires ch.tutteli.atrium.bundle.cc.en_GB.robstoll;")
        settingsSetup.buildGradle << """
            sourceCompatibility = 8
            targetCompatibility = 8

            apply plugin: 'maven-publish'

            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }
        """.stripIndent()
        def result = runGradleModuleBuild(settingsSetup, "jar", "generateMetadataFileForMavenPublication")
        //assert
        Asserts.assertStatusOk(result, [':compileKotlin', ':compileModuleKotlin', ':compileModuleJava', ':inspectClassesForKotlinIC', ':jar', ':generateMetadataFileForMavenPublication'], [], [':classes'])
        def gradleMetadataFile = settingsSetup.tmpPath.resolve("build/publications/maven/module.json").toFile()
        assertThat(gradleMetadataFile, hasJsonPath("\$.variants[*].attributes['org.gradle.jvm.version']", everyItem(equalTo(8))))
    }

    static final def GRADLE_PROJECT_DEPENDENCIES = """
        dependencies {
            implementation "ch.tutteli.atrium:atrium-cc-en_GB-robstoll:$ATRIUM_VERSION"

            constraints {
                implementation "org.jetbrains.kotlin:kotlin-stdlib:$KOTLIN_VERSION"
                implementation "org.jetbrains.kotlin:kotlin-reflect:$KOTLIN_VERSION"
            }
        }
    """

    static def setupModuleInfo(SettingsExtensionObject settingsSetup, String moduleInfoContent) throws IOException {
        //arrange
        settingsSetup.settings << "rootProject.name='test-project'"
        def module = new File(settingsSetup.tmp, 'src/module/')
        module.mkdirs()
        def moduleInfo = new File(module, 'module-info.java')
        moduleInfo << "module ch.tutteli.test { $moduleInfoContent }"
        def kotlin = new File(settingsSetup.tmp, 'src/main/kotlin')
        kotlin.mkdirs()
        def test = new File(kotlin, 'test.kt')
        test << """
            import ch.tutteli.atrium.api.cc.en_GB.toBe
            import ch.tutteli.atrium.verbs.assert

            fun foo() {
                assert(1).toBe(1)
            }
            """
        settingsSetup.buildGradle << """
            ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}

            apply plugin: 'kotlin'
            apply plugin: 'ch.tutteli.kotlin.module.info'

            repositories {
                maven { url "http://dl.bintray.com/robstoll/tutteli-jars" }
                jcenter()
            }

            $GRADLE_PROJECT_DEPENDENCIES

            project.afterEvaluate {
                project.sourceSets.each{
                    println(it)
                }
            }
            """
    }

    static def runGradleModuleBuild(SettingsExtensionObject settingsSetup, String... tasks) {
        return GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments(tasks.toList() + "--stacktrace")
            .build()
    }

    static def setupModuleInfoInSubproject(SettingsExtensionObject settingsSetup, String moduleInfoContent) throws IOException {
        //not for jdk8
        assumeFalse(System.getProperty("java.version").startsWith("1.8"))
        //arrange
        settingsSetup.settings << """
        rootProject.name='test-project'
        include 'sub1'
        """
        def module = new File(settingsSetup.tmp, 'sub1/src/module/')
        module.mkdirs()
        def moduleInfo = new File(module, 'module-info.java')
        moduleInfo << "module ch.tutteli.test { $moduleInfoContent }"
        def kotlin = new File(settingsSetup.tmp, 'sub1/src/main/kotlin')
        kotlin.mkdirs()
        def test = new File(kotlin, 'test.kt')
        test << """
            import ch.tutteli.atrium.api.cc.en_GB.toBe
            import ch.tutteli.atrium.verbs.assert

            fun foo() {
                assert(1).toBe(1)
            }
            """
        settingsSetup.buildGradle << """
            ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}

            def sub1 = project(':sub1')
            configure(sub1) {
                apply plugin: 'kotlin-platform-jvm'
                apply plugin: 'ch.tutteli.kotlin.module.info'
                repositories {
                    maven { url "http://dl.bintray.com/robstoll/tutteli-jars" }
                    jcenter()
                }
                $GRADLE_PROJECT_DEPENDENCIES
            }

            project.afterEvaluate {
                if(!project.hasProperty('sourceSets')){
                    println("root has no sourceSets")
                } else {
                    project.sourceSets.each{
                        println("root: \$it")
                    }
                }
            }
            sub1.afterEvaluate {
                sub1.sourceSets.each{
                    println("sub1: \$it")
                }
            }
            """
    }
}
