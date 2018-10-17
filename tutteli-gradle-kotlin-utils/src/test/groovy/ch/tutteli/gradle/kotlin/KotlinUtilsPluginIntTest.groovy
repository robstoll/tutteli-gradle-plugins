package ch.tutteli.gradle.kotlin

import ch.tutteli.gradle.test.Asserts
import ch.tutteli.gradle.test.SettingsExtension
import ch.tutteli.gradle.test.SettingsExtensionObject
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import static ch.tutteli.gradle.test.Asserts.assertContainsNotRegex
import static org.junit.jupiter.api.Assertions.*
import static org.junit.jupiter.api.Assumptions.assumeFalse

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
    void moduleInfoFails(SettingsExtensionObject settingsSetup) {
        //not for jdk8
        assumeFalse(System.getProperty("java.version").startsWith("1.8"))
        //act
        def exception = assertThrows(UnexpectedBuildFailure) {
            moduleInfo(settingsSetup, "requires kotlin.stdlib;")
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
        def result = moduleInfo(settingsSetup, "requires kotlin.stdlib; requires ch.tutteli.atrium.bundle.cc.en_GB.robstoll;")
        //assert
        Asserts.assertStatusOk(result, [':compileKotlin', ':compileModuleKotlin', ':compileModuleJava', ':jar'], [], [':classes'])
        assertTrue(result.output.contains("source set 'module'"), "should contain source set 'module':\n$result.output")
    }

    static def moduleInfo(SettingsExtensionObject settingsSetup, String moduleInfoContent) throws IOException {
        //arrange
        settingsSetup.settings << "rootProject.name='test-project'"
        def module = new File(settingsSetup.tmp, 'src/module/')
        module.mkdirs()
        def moduleInfo = new File(module, 'module-info.java')
        moduleInfo << """module ch.tutteli.test {
            $moduleInfoContent
        }"""
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
        buildscript {
            repositories { maven { url "https://plugins.gradle.org/m2/" } }
            dependencies {
                classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION'
                classpath files($settingsSetup.pluginClasspath)
            }
        }
        
        apply plugin: 'kotlin'
        apply plugin: 'ch.tutteli.kotlin.utils'
        kotlinutils.kotlinVersion = '$KOTLIN_VERSION'
        
        repositories {
            maven { url "http://dl.bintray.com/robstoll/tutteli-jars" }
            jcenter()
        }
        
        dependencies {
            compile "ch.tutteli.atrium:atrium-cc-en_GB-robstoll:0.7.0"
        }
        
        project.afterEvaluate {
            project.sourceSets.each{
                println(it)
            }
        }
        """
        //act
        return GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("jar", "--stacktrace")
            .build()
    }

    @Test
    void compileAndExcludeXy(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << "rootProject.name='test-project'"
        settingsSetup.buildGradle << """
        buildscript {
            repositories { maven { url "https://plugins.gradle.org/m2/" } }
            dependencies {
                classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION'
                classpath files($settingsSetup.pluginClasspath)
            }
        }
        
        apply plugin: 'kotlin-platform-js'
        apply plugin: 'ch.tutteli.kotlin.utils'
        kotlinutils.kotlinVersion = '$KOTLIN_VERSION'
        
        repositories{
            mavenCentral()
            maven { url "http://dl.bintray.com/robstoll/tutteli-jars" }
        }
        
        dependencies {
            compile kotlinStdlib(), excludeKbox
            compile kotlinStdlibJs(), excludeAtriumVerbs
            compile kotlinStdlibCommon(), excludeKotlin
            compile kotlinReflect(), excluding {
                kotlin()
                kbox()
                atriumVerbs()
            }
            testCompile "ch.tutteli.atrium:atrium-cc-en_GB-robstoll:0.7.0", excluding {
                kotlin()
                atriumVerbs()
                kbox()
            }
            testRuntime "org.jetbrains.spek:spek-junit-platform-engine:1.1.5", excluding {
                kotlin()
                exclude group: 'org.jetbrains.spek', module: 'spek-api'
            }
        }        
        """
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("dependencies", "--stacktrace")
            .build()
        //assert
        Asserts.assertStatusOk(result, ":dependencies")

        assertTrue(result.output.contains("\n+--- org.jetbrains.kotlin:kotlin-stdlib:$KOTLIN_VERSION"), "should contain stdlib:\n" + result.output)
        assertTrue(result.output.contains("\n+--- org.jetbrains.kotlin:kotlin-stdlib-js:$KOTLIN_VERSION"), "should contain stdlib-js:\n" + result.output)
        assertTrue(result.output.contains("\n+--- org.jetbrains.kotlin:kotlin-stdlib-common:$KOTLIN_VERSION"), "should contain stdlib-common:\n" + result.output)
        assertTrue(result.output.contains("\n\\--- org.jetbrains.kotlin:kotlin-reflect:$KOTLIN_VERSION"), "should contain reflect:\n" + result.output)

        assertContainsNotRegex(result.output, "stdlib", /(compile|default|runtime)[\S\s]+?\\--- org.jetbrains.kotlin:kotlin-reflect:$KOTLIN_VERSION\r?\n\s*\\--- org.jetbrains.kotlin:kotlin-stdlib:/)
        assertContainsNotRegex(result.output, "atrium-verbs", /ch.tutteli.atrium:atrium-verbs/)
        assertContainsNotRegex(result.output, "kbox", /ch.tutteli.kbox/)
        assertContainsNotRegex(result.output, "kbox", /org.jetbrains.spek:spek-api/)
    }

    @Test
    void configureCommonProjectsOnly(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << settingsFileContent
        settingsSetup.buildGradle << headerBuildFile(settingsSetup) + "configureCommonProjects()"
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
        settingsSetup.buildGradle << headerBuildFile(settingsSetup) + "configureCommonProjects()\n configureJsProjects()"
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
        settingsSetup.buildGradle << headerBuildFile(settingsSetup) + "configureCommonProjects()\n configureJvmProjects()"
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
        settingsSetup.buildGradle << headerBuildFile(settingsSetup) + "configureCommonProjects()\n configureJsProjects() \n configureJvmProjects()"
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
                repositories { maven { url "https://plugins.gradle.org/m2/" } }
                classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION'
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
