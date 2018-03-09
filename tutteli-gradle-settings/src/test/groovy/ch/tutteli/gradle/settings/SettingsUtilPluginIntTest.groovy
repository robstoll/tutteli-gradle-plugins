package ch.tutteli.gradle.settings

import ch.tutteli.gradle.test.Asserts
import ch.tutteli.gradle.test.SettingsExtension
import ch.tutteli.gradle.test.SettingsExtensionObject
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import static ch.tutteli.gradle.test.Asserts.assertProjectInOutput

@ExtendWith(SettingsExtension)
class SettingsUtilPluginIntTest {

    @Test
    void extensionVoodoo(SettingsExtensionObject settingsSetup) throws IOException {

        //arrange
        createDirs(settingsSetup.tmp)
        settingsSetup.settings << """
        rootProject.name='test-project'
        buildscript {
            dependencies {
                classpath files($settingsSetup.pluginClasspath)
            }
        }
        apply plugin: 'ch.tutteli.settings'

        // The most consice style, Extension object paired with propertyMissing/methodMissing voodoo
        
        include {
            one                 // short for `include ":\${rootProject.name}-one"`
            _ 'two-with-slash'  // short for `include ":\${rootProject.name}-two-with-slash"`
            
            test {              // defines that the following projects are in folder test
    
                three           // short for `include ":\${rootProject.name}-three"`
                                // and it sets `project.projectDir` to: 
                                // "\${rootProject.projectDir}/test/\${rootProject.name}-three"
                
                four            // same as for three but with four ;)
                
                subfolder {     // defines that the following projects are in folder test/subfolder
                    five        // same as three but projectDir base path is \${rootProject.projectDir}/test/subfolder     
                }
            }
            
            // You can also include non prefixed projects with this style. 
            // Have a look at the method extensionWithMethodCalls, 
            // you can use all methods shown there also here (mix both styles)
        }
        """
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("projects")
            .build()
        //assert
        assertProjectOneTwoFiveInOutput(result)
        assertStatusOk(result)
    }

    @Test
    void extensionWithMethodCalls(SettingsExtensionObject settingsSetup) {
        //arrange
        createDirs(settingsSetup.tmp)
        settingsSetup.settings << """        
        rootProject.name='test-project'
        buildscript {
            dependencies {
                classpath files($settingsSetup.pluginClasspath)
            }
        }
        apply plugin: 'ch.tutteli.settings'
        
        // The style using an extension object and calling methods
        
        include {
            prefixed 'one'                      // short for `include ":\${rootProject.name}-one"`
            prefixed ('one', 'two-with-slash')  // you can also define multiple projects in one line
            
            folder ('test') {                   // defines that the following projects are in folder test
            
                prefixed 'three'                // short for `include ":\${rootProject.name}-three"`
                                                // and it sets `project.projectDir` to: 
                                                // "\${rootProject.projectDir}/test/\${rootProject.name}-three"
                                      
                prefixed ('three', 'four')      //also here, you can define multiple projects
                
                folder ('subfolder') {
                    prefixed 'five'             // same as three but `project.projectDir` is 
                                                // \${rootProject.projectDir}/test/subfolder/\${rootProject.name}-five 
                }
            }
            
            folder ('test') {
                project 'six'                   // short for `include ":six"`
                                                // and it sets `project.projectDir` to:
                                                // "\${rootProject.projectDir}/test/six"
                                      
                project ('six', 'seven')        // also here, you can define multiple projects
            }
            
            project 'eight'                     // short for `include ":eight"`
            project ('eight', 'nine')           // also here, you can define multiple projects
        }
        """
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("projects")
            .build()
        //assert
        assertProjectOneTwoFiveInOutput(result)
        assertProjectInOutput(result, ':six')
        assertProjectInOutput(result, ':seven')
        assertProjectInOutput(result, ':eight')
        assertProjectInOutput(result, ':nine')
        assertStatusOk(result)
    }

    @Test
    void functions(SettingsExtensionObject settingsSetup) {
        //arrange
        createDirs(settingsSetup.tmp)
        settingsSetup.settings << """   
        rootProject.name='test-project'
        buildscript {
            dependencies {
                classpath files($settingsSetup.pluginClasspath)
            }
        }
        apply plugin: 'ch.tutteli.settings'
             
        // Simple functions
        
        //short for `include ":\${rootProject.name}-one"`
        includePrefixed 'one'
        
        //short for `include(":\${rootProject.name}-one", ":\${rootProject.name}-two-with-slash")`
        includePrefixed ('one', 'two-with-slash')
        
        /**
         * Shortcut for `include ":\${rootProject.name}-three"`
         * and it sets `project.projectDir` accordingly: 
         * "\${rootProject.projectDir}/test/\${rootProject.name}-three"
         */
        includePrefixedInFolder('test', 'three')
        
        /**
         * Shortcut for `include(":\${rootProject.name}-three", "\${rootProject.name}-four;")`
         * and it sets `project.projectDir` accordingly: 
         * "\${rootProject.projectDir}/test/\${rootProject.name}-three"    and
         * "\${rootProject.projectDir}/test/\${rootProject.name}-four"
         */
        includePrefixedInFolder('test', 'three', 'four')
        
         /**
         * Shortcut for `include ":\${rootProject.name}-five"`
         * and it sets `project.projectDir` accordingly: 
         * "\${rootProject.projectDir}/test/subfolder/\${rootProject.name}-five"
         */
        includePrefixedInFolder('test/subfolder', 'five')
        
        /**
         * Shortcut for `include ":six"`
         * and it sets `project.projectDir` accordingly: 
         * "\${rootProject.projectDir}/test/six"
         */
        includeCustomInFolder('test', 'six')
        
        /**
         * Shortcut for `include(":six", ":seven")`
         * and it sets `project.projectDir` accordingly: 
         * "\${rootProject.projectDir}/test/six"    and
         * "\${rootProject.projectDir}/test/seven"
         */
        includeCustomInFolder('test', 'six', 'seven')
        """
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("projects")
            .build()
        //assert
        assertProjectOneTwoFiveInOutput(result)
        assertProjectInOutput(result, ':six')
        assertProjectInOutput(result, ':seven')
        assertStatusOk(result)
    }

    private static void createDirs(File tmp) {
        new File(tmp, 'test-project-one').mkdir()
        new File(tmp, 'test-project-two-with-slash').mkdir()
        new File(tmp, 'test/test-project-three').mkdirs()
        new File(tmp, 'test/test-project-four').mkdirs()
        new File(tmp, 'test/subfolder/test-project-five').mkdirs()
        new File(tmp, 'test/six').mkdir()
        new File(tmp, 'test/seven').mkdir()
        new File(tmp, 'eight').mkdir()
        new File(tmp, 'nine').mkdir()
    }

    private static void assertProjectOneTwoFiveInOutput(BuildResult result) {
        assertProjectInOutput(result, ':test-project-one')
        assertProjectInOutput(result, ':test-project-two-with-slash')
        assertProjectInOutput(result, ':test-project-three')
        assertProjectInOutput(result, ':test-project-four')
        assertProjectInOutput(result, ':test-project-five')
    }

    private static assertStatusOk(BuildResult result) {
        Asserts.assertStatusOk(result, ":projects")
    }
}
