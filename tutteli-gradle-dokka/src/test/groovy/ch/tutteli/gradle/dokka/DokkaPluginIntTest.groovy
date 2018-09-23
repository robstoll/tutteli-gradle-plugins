package ch.tutteli.gradle.dokka


import ch.tutteli.gradle.test.SettingsExtension
import ch.tutteli.gradle.test.SettingsExtensionObject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import static org.junit.jupiter.api.Assertions.assertTrue

@ExtendWith(SettingsExtension)
class DokkaPluginIntTest {

    @Test
    void smokeTest_repoUrl(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << """
        rootProject.name='test-project'
        """
        def url = 'https://github.com/robstoll/tutteli-gradle-plugins'
        File buildGradle = new File(settingsSetup.tmp, 'build.gradle')
        buildGradle << """
        buildscript {
            project.version = '1.0.0-SNAPSHOT'
            dependencies {
                classpath files($settingsSetup.pluginClasspath)
            }
        }
        apply plugin: 'ch.tutteli.dokka'
        
        tutteliDokka {
            repoUrl = '$url'
            
            //delegates to the Dokka task (just for your convenience, everything in one place)
            dokka {
                outputFormat = 'javadoc'
            }
        }
        dokka.linkMappings.each{ println("was here url: \$it.url") } 
        """
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("projects")
            .build()
        //assert
        assertTrue(result.output.contains("was here url: $url/tree/master"), "println in output:\n" + result.output)
    }

    @Test
    void smokeTest_githubUser(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << """
        rootProject.name='test-project'
        """
        File buildGradle = new File(settingsSetup.tmp, 'build.gradle')
        buildGradle << """
        buildscript {
            project.version = '1.0.0-SNAPSHOT'
            dependencies {
                classpath files($settingsSetup.pluginClasspath)
            }
        }
        apply plugin: 'ch.tutteli.dokka'
        
        tutteliDokka {
            //uses the githubUser to create the repo url as well if one enables ghPages
            githubUser = 'robstoll'
            
            //adds an externalDocumentationLink based on the given githubUser as follows:
            //https://\$githubUser.github.io/\$rootProject.name/\$rootProject.version/doc/
            ghPages = true
            
            //delegates to the Dokka task (just for your convenience, everything in one place)
            dokka {
                outputFormat = 'javadoc'
            }
        }
        project.afterEvaluate {
            dokka.linkMappings.each{ println("was here url: \$it.url") } 
            dokka.externalDocumentationLinks.each{ println("was here extLink: \$it.url") }
        }
        """
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("projects")
            .build()
        //assert
        assertTrue(result.output.contains("was here url: https://github.com/robstoll/test-project/tree/master"), "url in output:\n" + result.output)
        assertTrue(!result.output.contains("was here extLink"), "should not contain extLink in output:\n" + result.output)
    }


    @Test
    void smokeTest_githubUserAndGhPages(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << """
        rootProject.name='test-project'
        """
        File buildGradle = new File(settingsSetup.tmp, 'build.gradle')
        buildGradle << """
        buildscript {
            project.version = '1.0.0'
            dependencies {
                classpath files($settingsSetup.pluginClasspath)
            }
        }
        apply plugin: 'ch.tutteli.dokka'
        
        tutteliDokka {
            //uses the githubUser to create the repo url as well if one enables ghPages
            githubUser = 'robstoll'
            
            //adds an externalDocumentationLink based on the given githubUser as follows:
            //https://\$githubUser.github.io/\$rootProject.name/\$rootProject.version/doc/
            ghPages = true
            
            //delegates to the Dokka task (just for your convenience, everything in one place)
            dokka {
                outputFormat = 'javadoc'
            }
        }
        project.afterEvaluate {
            dokka.linkMappings.each{ println("was here url: \$it.url") } 
            dokka.externalDocumentationLinks.each{ println("was here extLink: \$it.url") }
        }
        """
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("projects")
            .build()
        //assert
        assertTrue(result.output.contains("was here url: https://github.com/robstoll/test-project/tree/v1.0.0"), "url in output:\n" + result.output)
        assertTrue(result.output.contains("was here extLink: https://robstoll.github.io/test-project/1.0.0/doc"), "extLink in output:\n" + result.output)
    }
}
