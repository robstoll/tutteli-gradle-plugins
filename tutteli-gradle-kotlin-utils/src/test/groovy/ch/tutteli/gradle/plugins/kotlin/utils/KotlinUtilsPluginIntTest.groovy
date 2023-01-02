package ch.tutteli.gradle.plugins.kotlin.utils

import ch.tutteli.gradle.plugins.test.SettingsExtension
import ch.tutteli.gradle.plugins.test.SettingsExtensionObject
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import static ch.tutteli.gradle.plugins.test.Asserts.assertContainsNotRegex
import static ch.tutteli.gradle.plugins.test.Asserts.assertStatusOk
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.core.Every.everyItem
import static org.hamcrest.core.IsEqual.equalTo
import static com.jayway.jsonpath.matchers.JsonPathMatchers.*

@ExtendWith(SettingsExtension)
class KotlinUtilsPluginIntTest {

    private static final String KOTLIN_VERSION = '1.3.61'
    private static final String settingsFileContent = """
        rootProject.name='test-project'
        include 'test1-common'
        include 'test2-common'
        include 'test1-js'
        include 'test2-js'
        include 'test1-jvm'
        include 'test2-jvm'
        include 'test1-android'
        include 'test2-android'
        """

    @Test
    void compileAndExcludeXy(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << "rootProject.name='test-project'"
        settingsSetup.buildGradle << """
        ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}

        apply plugin: 'kotlin-platform-js'
       apply plugin: 'ch.tutteli.gradle.plugins.kotlin.utils'
        kotlinutils.kotlinVersion = '$KOTLIN_VERSION'

        repositories{
            mavenCentral()
        }

        dependencies {
            implementation kotlinStdlib(), excludeKbox
            implementation kotlinStdlibJs(), excludeAtriumVerbs
            implementation kotlinStdlibCommon(), excludeKotlin
            implementation kotlinReflect(), excludeKotlin

            testImplementation ("ch.tutteli.atrium:atrium-fluent-en_GB:0.16.0"), excludeAtriumVerbs

            testRuntimeOnly ("org.jetbrains.spek:spek-junit-platform-engine:1.1.5") {
                exclude group: 'org.jetbrains.spek', module: 'spek-api'
            }
            testImplementation kotlinTest()
            testImplementation kotlinTestJunit5()
            testImplementation kotlinTestJs()
            testImplementation kotlinTestCommon()
            testImplementation kotlinTestAnnotationsCommon()
        }
        """
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("dependencies", "--stacktrace")
            .build()
        //assert
        assertStatusOk(result, ":dependencies")

        assertTrue(result.output.contains("\n+--- org.jetbrains.kotlin:kotlin-stdlib:$KOTLIN_VERSION"), "should contain stdlib:\n" + result.output)
        assertTrue(result.output.contains("\n+--- org.jetbrains.kotlin:kotlin-stdlib-js:$KOTLIN_VERSION"), "should contain stdlib-js:\n" + result.output)
        assertTrue(result.output.contains("\n+--- org.jetbrains.kotlin:kotlin-stdlib-common:$KOTLIN_VERSION"), "should contain stdlib-common:\n" + result.output)
        assertTrue(result.output.contains("\n\\--- org.jetbrains.kotlin:kotlin-reflect:$KOTLIN_VERSION"), "should contain reflect:\n" + result.output)

        assertContainsNotRegex(result.output, "stdlib", /(compile|default|runtime)[\S\s]+?\\--- org.jetbrains.kotlin:kotlin-reflect:$KOTLIN_VERSION\r?\n\s*\\--- org.jetbrains.kotlin:kotlin-stdlib:/)
        assertContainsNotRegex(result.output, "atrium-verbs", /ch.tutteli.atrium:atrium-verbs/)
    }

    @Test
    void commonProjectsHaveSourceTargetJdk8(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << settingsFileContent
        settingsSetup.buildGradle << headerBuildFile(settingsSetup) + """
            configureCommonProjects()
            println("name: " + getProjectNameWithoutSuffix(project(':test1-js')))
            configure(project(':test1-common')) {
                apply plugin: 'maven-publish'
                publishing {
                    publications {
                        maven(MavenPublication) {
                            from components.java
                        }
                    }
                }
            }"""
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments(":test1-common:generateMetadataFileForMavenPublication")
            .build()
        assertTrue(result.output.contains("name: test1"), "getProjectNameWithoutSuffix(test1-js) == test1:\n" + result.output)
        def gradleMetadataFile = settingsSetup.tmpPath.resolve("test1-common/build/publications/maven/module.json").toFile()
        assertThat(gradleMetadataFile, hasJsonPath("\$.variants[*].attributes['org.gradle.jvm.version']", everyItem(equalTo(8))))
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
        executeDependenciesAndAssertNotExisting(gradleRunner, ":test1-android")
        executeDependenciesAndAssertNotExisting(gradleRunner, ":test2-android")
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
        executeDependenciesAndAssertNotExisting(gradleRunner, ":test1-android")
        executeDependenciesAndAssertNotExisting(gradleRunner, ":test2-android")
    }


    @Test
    void jvmProjectsHaveSourceTargetJdk8(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << settingsFileContent
        settingsSetup.buildGradle << headerBuildFile(settingsSetup) + """
            configureCommonProjects()
            configureJvmProjects()
            configure(project(':test1-jvm')) {
                apply plugin: 'maven-publish'
                publishing {
                    publications {
                        maven(MavenPublication) {
                            from components.java
                        }
                    }
                }
            }"""
        //act
        GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments(":test1-jvm:generateMetadataFileForMavenPublication")
            .build()
        def gradleMetadataFile = settingsSetup.tmpPath.resolve("test1-jvm/build/publications/maven/module.json").toFile()
        assertThat(gradleMetadataFile, hasJsonPath("\$.variants[*].attributes['org.gradle.jvm.version']", everyItem(equalTo(8))))
    }

    @Test
    void configureCommonAndAndroidProjects(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << settingsFileContent
        settingsSetup.buildGradle << headerBuildFile(settingsSetup) + "configureCommonProjects()\n configureAndroidProjects()"
        //act
        def gradleRunner = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)

        executeDependenciesAndAssertOnlyCommon(gradleRunner, ":test1-common")
        executeDependenciesAndAssertOnlyCommon(gradleRunner, ":test2-common")
        executeDependenciesAndAssertCommonAndAndroid(gradleRunner, ":test1")
        executeDependenciesAndAssertCommonAndAndroid(gradleRunner, ":test2")

        executeDependenciesAndAssertNotExisting(gradleRunner, ":test1-js")
        executeDependenciesAndAssertNotExisting(gradleRunner, ":test2-js")
        executeDependenciesAndAssertNotExisting(gradleRunner, ":test1-jvm")
        executeDependenciesAndAssertNotExisting(gradleRunner, ":test2-jvm")
    }

    @Test
    void configureCommonJsAndJvmProjects(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << settingsFileContent
        settingsSetup.buildGradle << headerBuildFile(settingsSetup) + "configureCommonProjects()\n configureJsProjects() \n configureAndroidProjects()"
        //act
        def gradleRunner = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)


        executeDependenciesAndAssertOnlyCommon(gradleRunner, ":test1-common")
        executeDependenciesAndAssertOnlyCommon(gradleRunner, ":test2-common")
        executeDependenciesAndAssertCommonAndJs(gradleRunner, ":test1")
        executeDependenciesAndAssertCommonAndJs(gradleRunner, ":test2")
        executeDependenciesAndAssertCommonAndAndroid(gradleRunner, ":test1")
        executeDependenciesAndAssertCommonAndAndroid(gradleRunner, ":test2")

        executeDependenciesAndAssertNotExisting(gradleRunner, ":test1-jvm")
        executeDependenciesAndAssertNotExisting(gradleRunner, ":test2-jvm")
    }

    @Test
    void configureCommonJsAndAndroidProjects(SettingsExtensionObject settingsSetup) throws IOException {
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

    @Test
    void buildAllJsNotConfigured(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << settingsFileContent
        settingsSetup.buildGradle << headerBuildFile(settingsSetup)
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments('buildAllJs')
            .build()
        //assert
        assertStatusOk(result, [],[],[':buildAllJs'])
    }

    @Test
    void buildAllCommon(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << settingsFileContent
        settingsSetup.buildGradle << headerBuildFile(settingsSetup) + "configureCommonProjects()"
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments('buildAllCommon')
            .build()
        //assert
        assertStatusOk(result,
            [':test1-common:inspectClassesForKotlinIC',
             ':test1-common:jar',
             ':test1-common:assemble',
             ':test1-common:build',
             ':test2-common:inspectClassesForKotlinIC',
             ':test2-common:jar',
             ':test2-common:assemble',
             ':test2-common:build',
             ':buildAllCommon'
            ],
            [],
            [':test1-common:classes',
            ':test1-common:testClasses',
            ':test1-common:check',
             ':test2-common:classes',
             ':test2-common:testClasses',
             ':test2-common:check'
            ])
    }

    @Test
    void buildAllJs(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << settingsFileContent
        settingsSetup.buildGradle << headerBuildFile(settingsSetup) + "configureCommonProjects()\n configureJsProjects()"
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments('buildAllJs')
            .build()
        //assert
        assertStatusOk(result,
            [':test1-common:inspectClassesForKotlinIC',
             ':test1-common:jar',
             ':test1-js:inspectClassesForKotlinIC',
             ':test1-js:jar',
             ':test1-js:assemble',
             ':test1-js:build',
             ':test2-common:inspectClassesForKotlinIC',
             ':test2-common:jar',
             ':test2-js:inspectClassesForKotlinIC',
             ':test2-js:jar',
             ':test2-js:assemble',
             ':test2-js:build',
             ':buildAllJs'
            ],
            [],
            [':test1-common:classes',
             ':test1-js:classes',
             ':test1-js:testClasses',
             ':test1-js:check',
             ':test2-common:classes',
             ':test2-js:classes',
             ':test2-js:testClasses',
             ':test2-js:check'
            ])
    }

    @Test
    void buildAllAndroid(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << settingsFileContent
        settingsSetup.buildGradle << headerBuildFile(settingsSetup) + "configureCommonProjects()\n configureAndroidProjects()"
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments('buildAllAndroid', '--stacktrace', '--warning-mode', 'all')
            .build()
        //assert
        assertStatusOk(result,
            [':test1-common:inspectClassesForKotlinIC',
             ':test1-common:jar',
             ':test1-android:inspectClassesForKotlinIC',
             ':test1-android:jar',
             ':test1-android:assemble',
             ':test1-android:build',
             ':test2-common:inspectClassesForKotlinIC',
             ':test2-common:jar',
             ':test2-android:inspectClassesForKotlinIC',
             ':test2-android:jar',
             ':test2-android:assemble',
             ':test2-android:build',
             ':buildAllAndroid'
            ],
            [],
            [':test1-common:classes',
             ':test1-android:classes',
             ':test1-android:testClasses',
             ':test1-android:check',
             ':test2-common:classes',
             ':test2-android:classes',
             ':test2-android:testClasses',
             ':test2-android:check'
            ])
    }

    @Test
    void buildAllJvm(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << settingsFileContent
        settingsSetup.buildGradle << headerBuildFile(settingsSetup) + "configureCommonProjects()\n configureJvmProjects()"
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments('buildAllJvm')
            .build()
        //assert
        assertStatusOk(result,
            [':test1-common:inspectClassesForKotlinIC',
             ':test1-common:jar',
             ':test1-jvm:inspectClassesForKotlinIC',
             ':test1-jvm:jar',
             ':test1-jvm:assemble',
             ':test1-jvm:build',
             ':test2-common:inspectClassesForKotlinIC',
             ':test2-common:jar',
             ':test2-jvm:inspectClassesForKotlinIC',
             ':test2-jvm:jar',
             ':test2-jvm:assemble',
             ':test2-jvm:build',
             ':buildAllJvm'
            ],
            [],
            [':test1-common:classes',
             ':test1-jvm:classes',
             ':test1-jvm:testClasses',
             ':test1-jvm:check',
             ':test2-common:classes',
             ':test2-jvm:classes',
             ':test2-jvm:testClasses',
             ':test2-jvm:check'
            ])
    }

    private static GString headerBuildFile(SettingsExtensionObject settingsSetup) {
        def headerBuildFile = """
        ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}

       apply plugin: 'ch.tutteli.gradle.plugins.kotlin.utils'
        kotlinutils {
            kotlinVersion = '$KOTLIN_VERSION'
        }

        subprojects {
            repositories{
                mavenCentral()
            }
        }

        """
        headerBuildFile
    }

    private static void executeDependenciesAndAssertOnlyCommon(GradleRunner gradleRunner, String subproject) {
        def result = gradleRunner
            .withArguments(subproject + ":dependencies")
            .build()
        //assert
        def output = outputWithoutKotlinCompilerClasspath(result)

        assertTrue(output.contains("\n\\--- org.jetbrains.kotlin:kotlin-stdlib-common:$KOTLIN_VERSION"), "should contain stdlib-common:\n" + output)
        assertFalse(output.contains("org.jetbrains.kotlin:kotlin-stdlib:"), "stdlib should not be in output:\n" + output)
        assertFalse(output.contains("org.jetbrains.kotlin:kotlin-stdlib-js:"), "stdlib-js should not be in output:\n" + output)
        assertStatusOk(result, subproject + ":dependencies")
    }

    private static void executeDependenciesAndAssertCommonAndJs(GradleRunner gradleRunner, String prefix) {
        executeDependenciesAndAssertCommonAnd(gradleRunner, prefix, "-js", "stdlib-js", "stdlib")
    }

    private static void executeDependenciesAndAssertCommonAndJvm(GradleRunner gradleRunner, String prefix) {
        executeDependenciesAndAssertCommonAnd(gradleRunner, prefix, "-jvm", "stdlib", "stdlib-js")
    }

    private static void executeDependenciesAndAssertCommonAndAndroid(GradleRunner gradleRunner, String prefix) {
        executeDependenciesAndAssertCommonAnd(gradleRunner, prefix, "-android", "stdlib", "stdlib-js")
    }

    private static String outputWithoutKotlinCompilerClasspath(BuildResult result) {
        return result.output.replaceFirst(/kotlinCompilerClasspath[\S\s]+kotlinCompilerPluginClasspath/, '')
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
        def output = outputWithoutKotlinCompilerClasspath(result)
        assertTrue(output.contains("\n+--- org.jetbrains.kotlin:kotlin-" + libInThere + ":$KOTLIN_VERSION"), "should contain $libInThere:\n" + output)
        assertTrue(output.contains("\n\\--- project $prefix-common"), "should contain project $prefix-common:\n" + output)
        assertTrue(output.contains("  \\--- org.jetbrains.kotlin:kotlin-stdlib-common:$KOTLIN_VERSION"), "should contain stdlib-common:\n" + output)
        assertFalse(output.contains("org.jetbrains.kotlin:kotlin-" + libNotInThere + ":"), "$libNotInThere should not be in output:\n" + output)

        assertStatusOk(result, prefix + suffix + ":dependencies")
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
