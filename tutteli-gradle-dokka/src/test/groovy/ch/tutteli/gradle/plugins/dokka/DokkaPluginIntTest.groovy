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

    @Test
    void smokeTest_repoUrl(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << "rootProject.name='test-project'"
        def url = 'https://github.com/robstoll/tutteli-gradle-plugins'
        def outputFormat = "javadoc"

        settingsSetup.buildGradle << """
        buildscript {
            project.version = '1.0.0-SNAPSHOT'
            dependencies {
                classpath files($settingsSetup.pluginClasspath)
            }
        }
        repositories {
            maven { url 'https://dl.bintray.com/kotlin/dokka' }
        }

       apply plugin: 'ch.tutteli.gradle.plugins.dokka'

        tutteliDokka {
            repoUrl = '$url'

            //delegates to the Dokka task (just for your convenience, everything in one place)
            dokka {
                outputFormat = '$outputFormat'
            }
        }
        ${printInfos()}
        """
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("dokka")
            .build()
        //assert
        assertTrue(result.output.contains("was here url: $url/tree/master"), "url should be in output:\n" + result.output)
        assertFalse(result.output.contains("was here extLink"), "should not contain extLink in output:\n" + result.output)
        assertTrue(result.output.contains("outputFormat: $outputFormat"), "outputFormat should be in output:\n" + result.output)
    }

    @Test
    void smokeTest_githubUser(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << "rootProject.name='test-project'"
        def outputFormat = "markdown"

        settingsSetup.buildGradle << """
        buildscript {
            project.version = '1.0.0-SNAPSHOT'
            dependencies {
                classpath files($settingsSetup.pluginClasspath)
            }
        }
        repositories {
            maven { url 'https://dl.bintray.com/kotlin/dokka' }
        }

       apply plugin: 'ch.tutteli.gradle.plugins.dokka'

        tutteliDokka {
            //uses the githubUser to create the repo url as well as the externalDocumentationLink if one enables ghPages
            githubUser = 'robstoll'

            //adds an externalDocumentationLink based on the given githubUser as follows:
            //https://\$githubUser.github.io/\$rootProject.name/\$rootProject.version/doc/
            ghPages = true

            //delegates to the Dokka task (just for your convenience, everything in one place)
            dokka {
                outputFormat = '$outputFormat'
            }
        }
        ${printInfos()}
        """
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("dokka")
            .build()
        //assert
        assertTrue(result.output.contains("was here url: https://github.com/robstoll/test-project/tree/master"), "url should be in output:\n" + result.output)
        assertFalse(result.output.contains("was here extLink"), "should not contain extLink in output:\n" + result.output)
        assertTrue(result.output.contains("outputFormat: $outputFormat"), "outputFormat should be in output:\n" + result.output)
    }


    @Test
    void smokeTest_githubUserAndGhPages(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << "rootProject.name='test-project'"
        def outputFormat = "markdown"

        settingsSetup.buildGradle << """
        buildscript {
            project.version = '1.0.0'
            dependencies {
                classpath files($settingsSetup.pluginClasspath)
            }
        }
        repositories {
            maven { url 'https://dl.bintray.com/kotlin/dokka' }
        }

       apply plugin: 'ch.tutteli.gradle.plugins.dokka'

        tutteliDokka {
            //uses the githubUser to create the repo url as well as the externalDocumentationLink if one enables ghPages
            githubUser = 'robstoll'

            //adds an externalDocumentationLink based on the given githubUser as follows:
            //https://\$githubUser.github.io/\$rootProject.name/\$rootProject.version/doc/
            ghPages = true

            //delegates to the Dokka task (just for your convenience, everything in one place)
            dokka {
                outputFormat = '$outputFormat'
            }
        }
        ${printInfos()}
        """
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("dokka")
            .build()
        //assert
        assertTrue(result.output.contains("was here url: https://github.com/robstoll/test-project/tree/v1.0.0"), "url should be in output:\n" + result.output)
        assertTrue(result.output.contains("was here extLink: https://robstoll.github.io/test-project/1.0.0/doc"), "extLink should be in output:\n" + result.output)
        assertTrue(result.output.contains("outputFormat: $outputFormat"), "outputFormat should be in output:\n" + result.output)
    }

    private static String printInfos() {
        """
        project.afterEvaluate {
            dokka.linkMappings.each{ println("was here url: \$it.url") }
            dokka.externalDocumentationLinks.each{ println("was here extLink: \$it.url") }
            println("outputFormat: \${dokka.outputFormat}")
        }
        """
    }
}
