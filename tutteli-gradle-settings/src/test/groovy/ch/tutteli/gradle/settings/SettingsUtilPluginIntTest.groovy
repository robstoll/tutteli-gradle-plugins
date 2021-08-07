package ch.tutteli.gradle.settings

import ch.tutteli.gradle.test.Asserts
import ch.tutteli.gradle.test.SettingsExtension
import ch.tutteli.gradle.test.SettingsExtensionObject
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import static ch.tutteli.gradle.test.Asserts.assertJvmJsInOutput
import static ch.tutteli.gradle.test.Asserts.assertJvmJsAndroidInOutput
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
       apply plugin: 'ch.tutteli.gradle.settings'

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

                    dsl('dsl-') {            // defines that the following projects are in folder `dsl`
                                             // and all prefixed projects are additionally prefixed with `dsl-`
                                             // (resulting in a prefix of `api-dsl-`)

                        six                  // same as five but projectDir base path is
                                             // \${rootProject.projectDir}/apis/subfolder/dsl
                    }
                }
            }

            kotlinJvmJs('core', 'core-')     // defines three projects which are contained in folder 'core' and are
                                             // additionally prefixed with 'core-' named 'common', 'js' and 'jvm'
                                             // and sets `project.projectDir` accordingly. For instance, for 'jvm':
                                             // "\${rootProject.projectDir}/core/\${rootProject.name}-core-jvm"

            kotlinJvmJs('domain')            // similar to kotlinJvmJs('core', 'core-') above but this time we do
                                             // not specify the additional prefix which means it will use the folder
                                             // name + '-' suffix for the prefix resulting in the following for 'js':
                                             // "\${rootProject.projectDir}/domain/\${rootProject.name}-domain-js"

            kotlinJvmJsAndroid('gui', 'ui-') // defines three projects which are contained in folder 'gui' and are
                                             // additionally prefixed with 'ui-' named 'common', 'android', 'js' and
                                             // 'jvm' and sets `project.projectDir` accordingly. For instance, for 'jvm':
                                             // "\${rootProject.projectDir}/gui/\${rootProject.name}-ui-jvm"

            kotlinJvmJsAndroid('web')        // similar to kotlinJvmJsAndroid('gui', 'gui-') above but this time we do
                                             // not specify the additional prefix which means it will use the folder
                                             // name + '-' suffix for the prefix resulting in the following for 'js':
                                             // "\${rootProject.projectDir}/web/\${rootProject.name}-web-js"

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
        assertProjectOneTwoSixInOutput(result)
        assertJvmJsInOutput(result, ':test-project-core')
        assertJvmJsInOutput(result, ':test-project-domain')
        assertJvmJsAndroidInOutput(result, ':test-project-ui')
        assertJvmJsAndroidInOutput(result, ':test-project-web')
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
       apply plugin: 'ch.tutteli.gradle.settings'

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

                folder('subfolder') {
                    prefixed 'five'             // same as four but `project.projectDir` is
                                                // \${rootProject.projectDir}/api/subfolder/\${rootProject.name}-api-five
                    folder('dsl', 'dsl-') {     // defines that the following projects are in folder `dsl`
                                                // and all prefixed projects are additionally prefixed with `dsl-`
                                                // (resulting in a prefix of `api-dsl-`)

                        prefixed 'six'          // same as five but projectDir base path is
                                                // \${rootProject.projectDir}/apis/subfolder/dsl
                    }
                }

                project 'seven'                   // short for `include ":six"` => additional prefix is ignored
                                                // and it sets `project.projectDir` to:
                                                // "\${rootProject.projectDir}/apis/six"
            }

            folder ('test') {
                project 'eight'                 // short for `include ":seven"`
                                                // and it sets `project.projectDir` to:
                                                // "\${rootProject.projectDir}/test/seven"

                project ('eight', 'nine')      // also here, you can define multiple projects
            }

            project 'ten'                      // short for `include ":eight"`
            project ('ten', 'eleven')             // also here, you can define multiple projects

            kotlinJvmJs('core', 'core-')        // defines three projects which are contained in folder 'core' and are
                                                // additionally prefixed with 'core-' named 'common', 'js' and 'jvm'
                                                // and sets `project.projectDir` accordingly. For instance, for 'jvm':
                                                // "\${rootProject.projectDir}/core/\${rootProject.name}-core-jvm"

            kotlinJvmJs('domain')               // similar to kotlinJvmJs('core', 'core-') above but this time we do
                                                // not specify the additional prefix which means it will use the folder
                                                // name + '-' suffix for the prefix resulting in the following for 'js':
                                                // "\${rootProject.projectDir}/domain/\${rootProject.name}-domain-js"
        }
        """
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("projects")
            .build()
        //assert
        assertProjectOneTwoSixInOutput(result)
        assertProjectInOutput(result, ':seven')
        assertProjectInOutput(result, ':eight')
        assertProjectInOutput(result, ':nine')
        assertProjectInOutput(result, ':ten')
        assertProjectInOutput(result, ':eleven')
        assertJvmJsInOutput(result, ':test-project-core')
        assertJvmJsInOutput(result, ':test-project-domain')
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
       apply plugin: 'ch.tutteli.gradle.settings'

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
         * Shortcut for `include ":\${rootProject.name}-api-dsl-six"`
         * and it sets `project.projectDir` accordingly:
         * "\${rootProject.projectDir}/apis/subfolder/dsl/\${rootProject.name}-api-dsl-six"
         */
        includePrefixedInFolder('apis/subfolder/dsl', 'api-dsl-six')

        /**
         * Shortcut for `include ":seven"`
         * and it sets `project.projectDir` accordingly:
         * "\${rootProject.projectDir}/apis/seven"
         */
        includeCustomInFolder('apis', 'seven')

        /**
         * Shortcut for `include(":eight", ":nine")`
         * and it sets `project.projectDir` accordingly:
         * "\${rootProject.projectDir}/test/eight"    and
         * "\${rootProject.projectDir}/test/nine"
         */
        includeCustomInFolder('test', 'eight', 'nine')
        """
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("projects")
            .build()
        //assert
        assertProjectOneTwoSixInOutput(result)
        assertProjectInOutput(result, ':seven')
        assertProjectInOutput(result, ':eight')
        assertProjectInOutput(result, ':nine')
        assertStatusOk(result)
    }

    private static void createDirs(File tmp) {
        new File(tmp, 'test-project-one').mkdir()
        new File(tmp, 'test-project-two-with-slash').mkdir()
        new File(tmp, 'test/test-project-three').mkdirs()
        new File(tmp, 'apis/test-project-api-four').mkdirs()
        new File(tmp, 'apis/subfolder/test-project-api-five').mkdirs()
        new File(tmp, 'apis/subfolder/dsl/test-project-api-dsl-six').mkdirs()
        new File(tmp, 'apis/seven').mkdir()
        new File(tmp, 'test/eight').mkdir()
        new File(tmp, 'test/nine').mkdir()
        new File(tmp, 'ten').mkdir()
        new File(tmp, 'eleven').mkdir()
        new File(tmp, 'core').mkdir()
        new File(tmp, 'core/test-project-core-common').mkdir()
        new File(tmp, 'core/test-project-core-js').mkdir()
        new File(tmp, 'core/test-project-core-jvm').mkdir()
        new File(tmp, 'domain').mkdir()
        new File(tmp, 'domain/test-project-domain-common').mkdir()
        new File(tmp, 'domain/test-project-domain-js').mkdir()
        new File(tmp, 'domain/test-project-domain-jvm').mkdir()
        new File(tmp, 'gui').mkdir()
        new File(tmp, 'gui/test-project-ui-common').mkdir()
        new File(tmp, 'gui/test-project-ui-android').mkdir()
        new File(tmp, 'gui/test-project-ui-js').mkdir()
        new File(tmp, 'gui/test-project-ui-jvm').mkdir()
        new File(tmp, 'web').mkdir()
        new File(tmp, 'web/test-project-web-common').mkdir()
        new File(tmp, 'web/test-project-web-android').mkdir()
        new File(tmp, 'web/test-project-web-js').mkdir()
        new File(tmp, 'web/test-project-web-jvm').mkdir()
    }

    private static void assertProjectOneTwoSixInOutput(BuildResult result) {
        assertProjectInOutput(result, ':test-project-one')
        assertProjectInOutput(result, ':test-project-two-with-slash')
        assertProjectInOutput(result, ':test-project-three')
        assertProjectInOutput(result, ':test-project-api-four')
        assertProjectInOutput(result, ':test-project-api-five')
        assertProjectInOutput(result, ':test-project-api-dsl-six')
    }

    private static assertStatusOk(BuildResult result) {
        Asserts.assertStatusOk(result, ":projects")
    }
}
