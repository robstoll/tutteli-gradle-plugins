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
            one                              // short for `include ":\${rootProject.name}-one"`
            _ 'two-with-slash'               // short for `include ":\${rootProject.name}-two-with-slash"`

            test {                           // defines that the following projects are in folder test

                three                        // short for `include ":\${rootProject.name}-three"`
                                             // and it sets `project.projectDir` to: 
                                             // "\${rootProject.projectDir}/test/\${rootProject.name}-three"                                
            }     

            apis('api-') {                   // defines that the following projects are in folder `apis` 
                                             // and all prefixed projects are additionally prefixed with `api-`

                four                         // short for `include ":\${rootProject.name}-api-four"`
                                             // and it sets `project.projectDir` to: 
                                             // "\${rootProject.projectDir}/apis/\${rootProject.name}-api-four"

                subfolder {                  // defines that the following projects are in folder apis/subfolder
                    five                     // same as four but projectDir base path is \${rootProject.projectDir}/apis/subfolder     
                }
            }

            kotlinJvmJs('core', 'core-')   // defines three projects which are contained in folder 'core' and are 
                                             // additionally prefixed with 'eleven-' named 'common', 'js' and 'jvm'
                                             // and sets `project.projectDir` accordingly. For instance, for 'jvm':
                                             // "\${rootProject.projectDir}/core/\${rootProject.name}-core-jvm" 

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
        assertProjectInOutput(result, ':test-project-core-common')
        assertProjectInOutput(result, ':test-project-core-js')
        assertProjectInOutput(result, ':test-project-core-jvm')
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
            
            folder('test') {                    // defines that the following projects are in folder test
            
                prefixed 'three'                // short for `include ":\${rootProject.name}-three"`
                                                // and it sets `project.projectDir` to: 
                                                // "\${rootProject.projectDir}/test/\${rootProject.name}-three"
            }
            
            folder('apis', 'api-') {            // defines that the following projects are in folder `apis` 
                                                // and all prefixed projects are additionally prefixed with `api-`
                                                  
                prefixed ('four')               // short for `include ":\${rootProject.name}-api-four"`
                                                // and it sets `project.projectDir` to: 
                                                // "\${rootProject.projectDir}/apis/\${rootProject.name}-api-four"
                
                folder ('subfolder') {
                    prefixed 'five'             // same as four but `project.projectDir` is 
                                                // \${rootProject.projectDir}/api/subfolder/\${rootProject.name}-api-five 
                }
                
                project 'six'                   // short for `include ":six"` => additional prefix is ignored
                                                // and it sets `project.projectDir` to:
                                                // "\${rootProject.projectDir}/apis/six"
            }
            
            folder ('test') {
                project 'seven'                 // short for `include ":seven"`
                                                // and it sets `project.projectDir` to:
                                                // "\${rootProject.projectDir}/test/seven"
                                      
                project ('seven', 'eight')      // also here, you can define multiple projects
            }
            
            project 'nine'                      // short for `include ":eight"`
            project ('nine', 'ten')             // also here, you can define multiple projects
            
            kotlinJvmJs('core', 'core-')        // defines three projects which are contained in folder 'core' and are 
                                                // additionally prefixed with 'eleven-' named 'common', 'js' and 'jvm'
                                                // and sets `project.projectDir` accordingly. For instance, for 'jvm':
                                                // "\${rootProject.projectDir}/core/\${rootProject.name}-core-jvm" 
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
        assertProjectInOutput(result, ':test-project-core-common')
        assertProjectInOutput(result, ':test-project-core-js')
        assertProjectInOutput(result, ':test-project-core-jvm')
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
         * Shortcut for `include(":"\${rootProject.name}-api-four")`
         * and it sets `project.projectDir` accordingly: 
         * "\${rootProject.projectDir}/apis/\${rootProject.name}-api-four"
         */
        includePrefixedInFolder('apis', 'api-four')
        
         /**
         * Shortcut for `include ":\${rootProject.name}-api-five"`
         * and it sets `project.projectDir` accordingly: 
         * "\${rootProject.projectDir}/apis/subfolder/\${rootProject.name}-api-five"
         */
        includePrefixedInFolder('apis/subfolder', 'api-five')
        
        /**
         * Shortcut for `include ":six"`
         * and it sets `project.projectDir` accordingly: 
         * "\${rootProject.projectDir}/apis/six"
         */
        includeCustomInFolder('apis', 'six')
        
        /**
         * Shortcut for `include(":six", ":seven")`
         * and it sets `project.projectDir` accordingly: 
         * "\${rootProject.projectDir}/test/seven"    and
         * "\${rootProject.projectDir}/test/eight"
         */
        includeCustomInFolder('test', 'seven', 'eight')
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
        new File(tmp, 'apis/test-project-api-four').mkdirs()
        new File(tmp, 'apis/subfolder/test-project-api-five').mkdirs()
        new File(tmp, 'apis/six').mkdir()
        new File(tmp, 'test/seven').mkdir()
        new File(tmp, 'test/eight').mkdir()
        new File(tmp, 'nine').mkdir()
        new File(tmp, 'ten').mkdir()
        new File(tmp, 'core').mkdir()
        new File(tmp, 'core/test-project-core-common').mkdir()
        new File(tmp, 'core/test-project-core-js').mkdir()
        new File(tmp, 'core/test-project-core-jvm').mkdir()
    }

    private static void assertProjectOneTwoFiveInOutput(BuildResult result) {
        assertProjectInOutput(result, ':test-project-one')
        assertProjectInOutput(result, ':test-project-two-with-slash')
        assertProjectInOutput(result, ':test-project-three')
        assertProjectInOutput(result, ':test-project-api-four')
        assertProjectInOutput(result, ':test-project-api-five')
    }

    private static assertStatusOk(BuildResult result) {
        Asserts.assertStatusOk(result, ":projects")
    }
}
