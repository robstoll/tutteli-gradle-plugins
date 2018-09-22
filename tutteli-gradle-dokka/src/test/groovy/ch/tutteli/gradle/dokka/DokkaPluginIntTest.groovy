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
    void smokeTest(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        settingsSetup.settings << """
        rootProject.name='test-project'
        """
        def url = 'https://github.com/robstoll/tutteli-gradle-plugin'
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
}
