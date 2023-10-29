package ch.tutteli.gradle.plugins.dokka

import ch.tutteli.gradle.plugins.test.SettingsExtension
import ch.tutteli.gradle.plugins.test.SettingsExtensionObject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

@ExtendWith(SettingsExtension)
class DokkaPluginIntTest {
    def static final KOTLIN_VERSION = '1.6.10'

    @Test
    void smokeTest_usesSimple_noVersionDefined_NoExtLink(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << "rootProject.name='test-project'"
        def url = 'https://github.com/robstoll/tutteli-gradle-plugins'

        settingsSetup.buildGradle << """
        ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}

        repositories {
            mavenCentral()
        }

       apply plugin: 'org.jetbrains.kotlin.jvm'
       apply plugin: 'ch.tutteli.gradle.plugins.dokka'

        tutteliDokka {
            repoUrl.set("$url")
        }
        ${printInfo()}
        """
        new File(settingsSetup.tmp, "src/main/kotlin").mkdirs()
        new File(settingsSetup.tmp, "src/test/kotlin").mkdirs()

        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("dokkaHtml")
            .build()
        //assert

        def expectedMainUrl = "$url/tree/main/src/main/kotlin"
        assertTrue(result.output.contains("main: was here url: $expectedMainUrl"), "url ($expectedMainUrl) be in output:\n" + result.output)

        def expectedTestUrl = "$url/tree/main/src/test/kotlin"
        assertTrue(result.output.contains("test: was here url: $expectedTestUrl"), "url ($expectedTestUrl should be in output:\n" + result.output)
        assertFalse(result.output.contains("was here extLink"), "should not contain extLink in output:\n" + result.output)
    }

    @Test
    void smokeTest_usesSimple_notInRoot_repoUrlIsRelative(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << """
            rootProject.name='test-project'
            include 'sub'
            project(':sub').projectDir = file("\${rootProject.projectDir}/subDir")
        """
        def url = 'https://github.com/robstoll/tutteli-gradle-plugins'

        settingsSetup.buildGradle << """
        ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}

        repositories {
            mavenCentral()
        }

        configure(project(':sub')) { project ->
            repositories {
                mavenCentral()
            }
            apply plugin: 'org.jetbrains.kotlin.jvm'
            apply plugin: 'ch.tutteli.gradle.plugins.dokka'

            tutteliDokka {
                repoUrl = '$url'
            }

            ${printInfo()}
        }
        """
        new File(settingsSetup.tmp, "subDir/src/main/kotlin").mkdirs()
        new File(settingsSetup.tmp, "subDir/src/test/kotlin").mkdirs()

        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("dokkaHtml")
            .build()
        //assert
        def expectedMainUrl = "$url/tree/main/subDir/src/main/kotlin"
        assertTrue(result.output.contains("main: was here url: $expectedMainUrl"), "url ($expectedMainUrl) be in output:\n" + result.output)

        def expectedTestUrl = "$url/tree/main/subDir/src/test/kotlin"
        assertTrue(result.output.contains("test: was here url: $expectedTestUrl"), "url ($expectedTestUrl should be in output:\n" + result.output)
        assertFalse(result.output.contains("was here extLink"), "should not contain extLink in output:\n" + result.output)
    }

    @Test
    void smokeTest_multiModule_dokkaHtmlMultiModule_repoUrlIsRelative(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << """
            rootProject.name='test-project'
            include 'sub'
            project(':sub').projectDir = file("\${rootProject.projectDir}/subDir")
        """
        def url = 'https://github.com/robstoll/tutteli-gradle-plugins'

        settingsSetup.buildGradle << """
        ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}

        repositories {
            mavenCentral()
        }
        apply plugin: 'ch.tutteli.gradle.plugins.dokka'
        tutteliDokka {
            repoUrl = '$url'
        }


        subprojects { project ->
            repositories {
                mavenCentral()
            }
            apply plugin: 'org.jetbrains.kotlin.jvm'
            apply plugin: 'ch.tutteli.gradle.plugins.dokka'
        }
        allprojects {
            ${printInfo()}
        }
        """
        new File(settingsSetup.tmp, "subDir/src/main/kotlin").mkdirs()
        new File(settingsSetup.tmp, "subDir/src/test/kotlin").mkdirs()

        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("dokkaHtmlMultiModule", "--stacktrace")
            .build()
        //assert
        def expectedRootMainUrl = "$url/tree/main/subDir/src/main/kotlin"
        assertFalse(result.output.contains("test-project main: was here url: $expectedRootMainUrl"), "test-project main url ($expectedRootMainUrl) shoule not be in output:\n" + result.output)

        def expectedRootTestUrl = "$url/tree/main/subDir/src/test/kotlin"
        assertFalse(result.output.contains("test-project test: was here url: $expectedRootTestUrl"), "test-project test url ($expectedRootTestUrl should not be in output:\n" + result.output)

        def expectedSubMainUrl = "$url/tree/main/subDir/src/main/kotlin"
        assertTrue(result.output.contains("sub main: was here url: $expectedSubMainUrl"), "sub main url ($expectedSubMainUrl) be in output:\n" + result.output)

        def expectedSubTestUrl = "$url/tree/main/subDir/src/test/kotlin"
        assertTrue(result.output.contains("sub test: was here url: $expectedSubTestUrl"), "sub test url ($expectedSubTestUrl should be in output:\n" + result.output)

        assertFalse(result.output.contains("was here extLink"), "should not contain extLink in output:\n" + result.output)
    }

    @Test
    void smokeTest_githubUser(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        def githubUser = 'test-user'
        settingsSetup.settings << "rootProject.name='test-project'"

        settingsSetup.buildGradle << """
        ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}

         repositories {
            mavenCentral()
        }

        apply plugin: 'org.jetbrains.kotlin.jvm'
        apply plugin: 'ch.tutteli.gradle.plugins.dokka'

        tutteliDokka {
            //uses the githubUser to create the repo url as well as the externalDocumentationLink if one uses a release version (x.y.z)
            githubUser = '$githubUser'
        }
        ${printInfo()}
        """
        new File(settingsSetup.tmp, "src/main/kotlin").mkdirs()
        new File(settingsSetup.tmp, "src/test/kotlin").mkdirs()
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("dokkaHtml")
            .build()
        //assert
        assertTrue(result.output.contains("main: was here url: https://github.com/$githubUser/test-project/tree/main/src/main/kotlin"), "url should be in output:\n" + result.output)
        assertTrue(result.output.contains("test: was here url: https://github.com/$githubUser/test-project/tree/main/src/test/kotlin"), "url should be in output:\n" + result.output)
        assertFalse(result.output.contains("was here extLink"), "should not contain extLink in output:\n" + result.output)
    }

    @Test
    void smokeTest_tutteliProjectAndReleaseVersion(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << "rootProject.name='test-project'"

        settingsSetup.buildGradle << """
        ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}
         repositories {
            mavenCentral()
        }
        project.version = '1.0.0'
        project.group = 'ch.tutteli'

        apply plugin: 'org.jetbrains.kotlin.jvm'
        apply plugin: 'ch.tutteli.gradle.plugins.dokka'

        ${printInfo()}
        """
        new File(settingsSetup.tmp, "src/main/kotlin").mkdirs()
        new File(settingsSetup.tmp, "src/test/kotlin").mkdirs()
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("dokkaHtml")
            .build()
        //assert
        assertTrue(result.output.contains("main: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/main/kotlin"), "url should be in output:\n" + result.output)
        assertTrue(result.output.contains("test: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/test/kotlin"), "url should be in output:\n" + result.output)
        assertTrue(result.output.contains("main: was here extLink: https://robstoll.github.io/test-project/kdoc/test-project"), "extLink should be in output:\n" + result.output)
        assertTrue(result.output.contains("test: was here extLink: https://robstoll.github.io/test-project/kdoc/test-project"), "extLink should be in output:\n" + result.output)
    }

    @Test
    void smokeTestNotWriteToDocs_githubUserAndReleaseVersion(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << "rootProject.name='test-project'"

        settingsSetup.buildGradle << """
        ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}
         repositories {
            mavenCentral()
        }
        project.version = '1.0.0'

        apply plugin: 'org.jetbrains.kotlin.jvm'
        apply plugin: 'ch.tutteli.gradle.plugins.dokka'

        tutteliDokka {
            //uses the githubUser to create the repo url as well as the externalDocumentationLink if one uses a release version (x.y.z)
            githubUser = 'robstoll'
            writeToDocs = false
        }
        ${printInfo()}
        """
        new File(settingsSetup.tmp, "src/main/kotlin").mkdirs()
        new File(settingsSetup.tmp, "src/test/kotlin").mkdirs()
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("dokkaHtml")
            .build()
        //assert
        assertTrue(result.output.contains("main: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/main/kotlin"), "url should be in output:\n" + result.output)
        assertTrue(result.output.contains("test: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/test/kotlin"), "url should be in output:\n" + result.output)
        assertTrue(result.output.contains("main: was here extLink: https://robstoll.github.io/test-project/1.0.0/kdoc"), "extLink should be in output:\n" + result.output)
        assertTrue(result.output.contains("test: was here extLink: https://robstoll.github.io/test-project/1.0.0/kdoc"), "extLink should be in output:\n" + result.output)
    }

    @Test
    void smokeTestMpp_githubUserAndReleaseVersion(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << "rootProject.name='test-project'"

        settingsSetup.buildGradle << """
        import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTests

        ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}
         repositories {
            mavenCentral()
        }
        project.version = '1.0.0'

        apply plugin: 'org.jetbrains.kotlin.multiplatform'
        apply plugin: 'ch.tutteli.gradle.plugins.dokka'

        kotlin {
           jvm { }
           js { browser { } }
           def hostOs = System.getProperty("os.name")
           def isMingwX64 = hostOs.startsWith("Windows")
           KotlinNativeTargetWithTests nativeTarget
           if (hostOs == "Mac OS X") nativeTarget = macosX64('native')
           else if (hostOs == "Linux") nativeTarget = linuxX64("native")
           else if (isMingwX64) nativeTarget = mingwX64("native")
           else throw new GradleException("Host OS is not supported in Kotlin/Native.")


           sourceSets {
               commonMain { }
               commonTest { }
               jvmMain { }
               jvmTest { }
               jsMain { }
               jsTest { }
               nativeMain { }
               nativeTest { }
           }
        }

        tutteliDokka {
            //uses the githubUser to create the repo url as well as the externalDocumentationLink if one uses a release version (x.y.z)
            githubUser = 'robstoll'
        }
        ${printInfo()}
        """
        new File(settingsSetup.tmp, "src/commonMain/kotlin").mkdirs()
        new File(settingsSetup.tmp, "src/commonTest/kotlin").mkdirs()
        new File(settingsSetup.tmp, "src/jvmMain/kotlin").mkdirs()
        new File(settingsSetup.tmp, "src/jvmTest/kotlin").mkdirs()
        new File(settingsSetup.tmp, "src/jsMain/kotlin").mkdirs()
        new File(settingsSetup.tmp, "src/jsTest/kotlin").mkdirs()
        new File(settingsSetup.tmp, "src/nativeMain/kotlin").mkdirs()
        new File(settingsSetup.tmp, "src/nativeTest/kotlin").mkdirs()
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("dokkaHtml")
            .build()
        //assert
        assertTrue(result.output.contains("commonMain: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/commonMain/kotlin"), "commonMain url should be in output:\n" + result.output)
        assertTrue(result.output.contains("commonTest: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/commonTest/kotlin"), "commonTest url should be in output:\n" + result.output)
        assertTrue(result.output.contains("jvmMain: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/jvmMain/kotlin"), "jvmMain url should be in output:\n" + result.output)
        assertTrue(result.output.contains("jvmTest: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/jvmTest/kotlin"), "jvmTest url should be in output:\n" + result.output)
        assertTrue(result.output.contains("jsMain: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/jsMain/kotlin"), "jsMain url should be in output:\n" + result.output)
        assertTrue(result.output.contains("jsTest: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/jsTest/kotlin"), "jsTest url should be in output:\n" + result.output)
        assertTrue(result.output.contains("nativeMain: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/nativeMain/kotlin"), "nativeMain url should be in output:\n" + result.output)
        assertTrue(result.output.contains("nativeTest: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/nativeTest/kotlin"), "nativeTest url should be in output:\n" + result.output)

        assertTrue(result.output.contains("commonMain: was here extLink: https://robstoll.github.io/test-project/kdoc/test-project"), "commonMain extLink should be in output:\n" + result.output)
        assertTrue(result.output.contains("commonTest: was here extLink: https://robstoll.github.io/test-project/kdoc/test-project"), "commonTest extLink should be in output:\n" + result.output)
        assertTrue(result.output.contains("jvmMain: was here extLink: https://robstoll.github.io/test-project/kdoc/test-project"), "jvmMain extLink should be in output:\n" + result.output)
        assertTrue(result.output.contains("jvmTest: was here extLink: https://robstoll.github.io/test-project/kdoc/test-project"), "jvmTest extLink should be in output:\n" + result.output)
        assertTrue(result.output.contains("jsMain: was here extLink: https://robstoll.github.io/test-project/kdoc/test-project"), "jsMain extLink should be in output:\n" + result.output)
        assertTrue(result.output.contains("jsTest: was here extLink: https://robstoll.github.io/test-project/kdoc/test-project"), "jsTest extLink should be in output:\n" + result.output)
        assertTrue(result.output.contains("nativeMain: was here extLink: https://robstoll.github.io/test-project/kdoc/test-project"), "nativeMain extLink should be in output:\n" + result.output)
        assertTrue(result.output.contains("nativeTest: was here extLink: https://robstoll.github.io/test-project/kdoc/test-project"), "nativeTest extLink should be in output:\n" + result.output)
    }

    @Test
    void smokeTestMpp_notAllSourceFoldersExist(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << "rootProject.name='test-project'"

        settingsSetup.buildGradle << """
        import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTests

        ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}
         repositories {
            mavenCentral()
        }
        project.version = '1.0.0'

        apply plugin: 'org.jetbrains.kotlin.multiplatform'
        apply plugin: 'ch.tutteli.gradle.plugins.dokka'

        kotlin {
           jvm { }
           js { browser { } }
           def hostOs = System.getProperty("os.name")
           def isMingwX64 = hostOs.startsWith("Windows")
           KotlinNativeTargetWithTests nativeTarget
           if (hostOs == "Mac OS X") nativeTarget = macosX64('native')
           else if (hostOs == "Linux") nativeTarget = linuxX64("native")
           else if (isMingwX64) nativeTarget = mingwX64("native")
           else throw new GradleException("Host OS is not supported in Kotlin/Native.")


           sourceSets {
               commonMain { }
               commonTest { }
               jvmMain { }
               jvmTest { }
               jsMain { }
               jsTest { }
               nativeMain { }
               nativeTest { }
           }
        }

        tutteliDokka {
            //uses the githubUser to create the repo url as well as the externalDocumentationLink if one uses a release version (x.y.z)
            githubUser = 'robstoll'
        }
        ${printInfo()}
        """
        new File(settingsSetup.tmp, "src/commonMain/kotlin").mkdirs()
        new File(settingsSetup.tmp, "src/commonTest/kotlin").mkdirs()
        new File(settingsSetup.tmp, "src/jvmMain/kotlin").mkdirs()
        // no jvm specific tests on purpose
//        new File(settingsSetup.tmp, "src/jvmTest/kotlin").mkdirs()
        // no js specific main sources on purpose
//        new File(settingsSetup.tmp, "src/jsMain/kotlin").mkdirs()
        new File(settingsSetup.tmp, "src/jsTest/kotlin").mkdirs()
        // no native specific main sources on purpose
//        new File(settingsSetup.tmp, "src/nativeMain/kotlin").mkdirs()
        new File(settingsSetup.tmp, "src/nativeTest/kotlin").mkdirs()
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("dokkaHtml")
            .build()
        //assert
        assertTrue(result.output.contains("commonMain: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/commonMain/kotlin"), "commonMain url should be in output:\n" + result.output)
        assertTrue(result.output.contains("commonTest: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/commonTest/kotlin"), "commonTest url should be in output:\n" + result.output)
        assertTrue(result.output.contains("jvmMain: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/jvmMain/kotlin"), "jvmMain url should be in output:\n" + result.output)
        assertFalse(result.output.contains("jvmTest: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/jvmTest/kotlin"), "jvmTest url should *not* be in output:\n" + result.output)
        assertFalse(result.output.contains("jsMain: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/jsMain/kotlin"), "jsMain url should *not* be in output:\n" + result.output)
        assertTrue(result.output.contains("jsTest: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/jsTest/kotlin"), "jsTest url should be in output:\n" + result.output)
        assertFalse(result.output.contains("nativeMain: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/nativeMain/kotlin"), "nativeMain url should *not* be in output:\n" + result.output)
        assertTrue(result.output.contains("nativeTest: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/nativeTest/kotlin"), "nativeTest url should be in output:\n" + result.output)

        assertTrue(result.output.contains("commonMain: was here extLink: https://robstoll.github.io/test-project/kdoc/test-project"), "commonMain extLink should be in output:\n" + result.output)
        assertTrue(result.output.contains("commonTest: was here extLink: https://robstoll.github.io/test-project/kdoc/test-project"), "commonTest extLink should be in output:\n" + result.output)
        assertTrue(result.output.contains("jvmMain: was here extLink: https://robstoll.github.io/test-project/kdoc/test-project"), "jvmMain extLink should be in output:\n" + result.output)
        assertTrue(result.output.contains("jvmTest: was here extLink: https://robstoll.github.io/test-project/kdoc/test-project"), "jvmTest extLink should be in output:\n" + result.output)
        assertTrue(result.output.contains("jsMain: was here extLink: https://robstoll.github.io/test-project/kdoc/test-project"), "jsMain extLink should be in output:\n" + result.output)
        assertTrue(result.output.contains("jsTest: was here extLink: https://robstoll.github.io/test-project/kdoc/test-project"), "jsTest extLink should be in output:\n" + result.output)
        assertTrue(result.output.contains("nativeMain: was here extLink: https://robstoll.github.io/test-project/kdoc/test-project"), "nativeMain extLink should be in output:\n" + result.output)
        assertTrue(result.output.contains("nativeTest: was here extLink: https://robstoll.github.io/test-project/kdoc/test-project"), "nativeTest extLink should be in output:\n" + result.output)
    }

    @Test
    void smokeTest_multiModuleAndVersionSet_dokkaHtmlMultiModule_repoUrlAndExtLinkAreRelative(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << """
            rootProject.name='test-project'
            include 'sub'
            project(':sub').projectDir = file("\${rootProject.projectDir}/subDir")
        """
        def url = 'https://github.com/robstoll/tutteli-gradle-plugins'
        def githubUser = 'test-user'
        settingsSetup.buildGradle << """
        ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}

        repositories {
            mavenCentral()
        }

        version = "1.2.3"

        apply plugin: 'ch.tutteli.gradle.plugins.dokka'
        tutteliDokka {
            githubUser = '$githubUser'
            repoUrl = '$url'
        }

        subprojects { project ->
            repositories {
                mavenCentral()
            }
            apply plugin: 'org.jetbrains.kotlin.jvm'
            apply plugin: 'ch.tutteli.gradle.plugins.dokka'
        }
        allprojects {
            ${printInfo()}
        }
        """
        new File(settingsSetup.tmp, "subDir/src/main/kotlin").mkdirs()
        new File(settingsSetup.tmp, "subDir/src/test/kotlin").mkdirs()

        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("dokkaHtmlMultiModule", "--stacktrace")
            .build()
        //assert
        def expectedRootMainUrl = "$url/tree/v1.2.3/subDir/src/main/kotlin"
        assertFalse(result.output.contains("test-project main: was here url: $expectedRootMainUrl"), "test-project url main ($expectedRootMainUrl) shoule not be in output:\n" + result.output)

        def expectedRootTestUrl = "$url/tree/v1.2.3/subDir/src/test/kotlin"
        assertFalse(result.output.contains("test-project test: was here url: $expectedRootTestUrl"), "test-project url test ($expectedRootTestUrl) should not be in output:\n" + result.output)

        def expectedSubMainUrl = "$url/tree/v1.2.3/subDir/src/main/kotlin"
        assertTrue(result.output.contains("sub main: was here url: $expectedSubMainUrl"), "sub url main ($expectedSubMainUrl) should be in output:\n" + result.output)

        def expectedSubTestUrl = "$url/tree/v1.2.3/subDir/src/test/kotlin"
        assertTrue(result.output.contains("sub test: was here url: $expectedSubTestUrl"), "sub url test ($expectedSubTestUrl should be in output:\n" + result.output)

        def extLink = "https://test-user.github.io/test-project/kdoc/test-project"
        assertTrue(result.output.contains("sub main: was here extLink: $extLink"), "sub main extLink ($extLink) should be in output:\n" + result.output)
        assertTrue(result.output.contains("sub test: was here extLink: $extLink"), "sub test extLink ($extLink) should be in output:\n" + result.output)
    }

    @Test
    void smokeTestUsingDokka1_8_10_whereProviderFileWasUsed_writeToGhPages(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << """
            pluginManagement {
                resolutionStrategy {
                    eachPlugin {
                        if (requested.id.namespace == 'org.jetbrains.dokka') {
                            useModule('org.jetbrains.dokka:dokka-gradle-plugin:1.8.10')
                        }
                    }
                }
            }
            rootProject.name='test-project'
        """

        settingsSetup.buildGradle << """
        import org.gradle.api.tasks.testing.logging.TestLogEvent
        buildscript {
            repositories {
                maven { url "https://plugins.gradle.org/m2/" }
            }
            dependencies {
                classpath 'org.jetbrains.dokka:dokka-gradle-plugin:1.8.10'
                classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION'
                classpath files(${settingsSetup.pluginClasspath.findAll { !it.contains("org.jetbrains.dokka") }})
            }
        }
        repositories {
            mavenCentral()
        }
        project.version = '1.0.0'

        apply plugin: 'org.jetbrains.dokka'
        apply plugin: 'org.jetbrains.kotlin.jvm'

        project.afterEvaluate {
            project.tasks.withType(org.jetbrains.dokka.gradle.AbstractDokkaLeafTask).configureEach {
                // custom output directory
                println("outputDirectoryClass is: \${outputDirectory.getClass().name}")
            }
        }

        apply plugin: 'ch.tutteli.gradle.plugins.dokka'

        tutteliDokka {
            //uses the githubUser to create the repo url as well as the externalDocumentationLink if one uses a release version (x.y.z)
            githubUser = 'robstoll'
            writeToDocs = false
        }
        ${ printInfo() }
        """
        new File(settingsSetup.tmp, "src/main/kotlin").mkdirs()
        new File(settingsSetup.tmp, "src/test/kotlin").mkdirs()
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("dokkaHtml", "--stacktrace")
            .build()

        //assert
        assertTrue(result.output.contains("outputDirectoryClass is: org.gradle.api.internal.provider.DefaultProperty"))
        assertTrue(result.output.contains("main: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/main/kotlin"), "url should be in output:\n" + result.output)
        assertTrue(result.output.contains("test: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/test/kotlin"), "url should be in output:\n" + result.output)
        assertTrue(result.output.contains("main: was here extLink: https://robstoll.github.io/test-project/1.0.0/kdoc"), "extLink should be in output:\n" + result.output)
        assertTrue(result.output.contains("test: was here extLink: https://robstoll.github.io/test-project/1.0.0/kdoc"), "extLink should be in output:\n" + result.output)
    }

    private static String printInfo() {
        """
        project.afterEvaluate {
            dokkaHtml.dokkaSourceSets.each { set -> set.sourceLinks.get().each { println("\$project.name \$set.name: was here url: \${it.remoteUrl.get()}") } }
            dokkaHtml.dokkaSourceSets.each { set -> set.externalDocumentationLinks.get().each { println("\$project.name \$set.name: was here extLink: \${it.url.get()}") } }
        }
        """
    }
}
