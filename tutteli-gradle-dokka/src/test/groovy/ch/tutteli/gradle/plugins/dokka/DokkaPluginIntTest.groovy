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
    def static final KOTLIN_VERSION = '1.5.30'

    @Test
    void smokeTestUsesSimple_repoUrl(SettingsExtensionObject settingsSetup) throws IOException {
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
            repoUrl = '$url'
        }
        ${printInfo()}
        """
        new File(settingsSetup.tmp, "src/main/kotlin").mkdirs()
        new File(settingsSetup.tmp, "src/test/kotlin").mkdirs()

        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("dokka")
            .build()
        //assert
        assertTrue(result.output.contains("main: was here url: $url/tree/main/src/main/kotlin"), "url should be in output:\n" + result.output)
        assertTrue(result.output.contains("test: was here url: $url/tree/main/src/test/kotlin"), "url should be in output:\n" + result.output)
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
            .withArguments("dokka")
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
            .withArguments("dokka")
            .build()
        //assert
        assertTrue(result.output.contains("main: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/main/kotlin"), "url should be in output:\n" + result.output)
        assertTrue(result.output.contains("test: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/test/kotlin"), "url should be in output:\n" + result.output)
        assertTrue(result.output.contains("main: was here extLink: https://robstoll.github.io/test-project/kdoc/test-project"), "extLink should be in output:\n" + result.output)
        assertTrue(result.output.contains("test: was here extLink: https://robstoll.github.io/test-project/kdoc/test-project"), "extLink should be in output:\n" + result.output)
    }

    @Test
    void smokeTestNotModeSimple_githubUserAndReleaseVersion(SettingsExtensionObject settingsSetup) throws IOException {
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
            modeSimple = false
        }
        ${printInfo()}
        """
        new File(settingsSetup.tmp, "src/main/kotlin").mkdirs()
        new File(settingsSetup.tmp, "src/test/kotlin").mkdirs()
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("dokka")
            .build()
        //assert
        assertTrue(result.output.contains("main: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/main/kotlin"), "url should be in output:\n" + result.output)
        assertTrue(result.output.contains("test: was here url: https://github.com/robstoll/test-project/tree/v1.0.0/src/test/kotlin"), "url should be in output:\n" + result.output)
        assertTrue(result.output.contains("main: was here extLink: https://robstoll.github.io/test-project/1.0.0/doc/test-project"), "extLink should be in output:\n" + result.output)
        assertTrue(result.output.contains("test: was here extLink: https://robstoll.github.io/test-project/1.0.0/doc/test-project"), "extLink should be in output:\n" + result.output)
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
            .withArguments("dokka")
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


    private static String printInfo() {
        """
        project.afterEvaluate {
            dokkaHtml.dokkaSourceSets.each { set -> set.sourceLinks.get().each { println("\$set.name: was here url: \${it.remoteUrl.get()}") }}
            dokkaHtml.dokkaSourceSets.each { set -> set.externalDocumentationLinks.get().each{ println("\$set.name: was here extLink: \${it.url.get()}") }}
        }
        """
    }
}
