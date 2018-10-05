package ch.tutteli.gradle.kotlin

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

import static ch.tutteli.gradle.kotlin.KotlinUtilsPlugin.EXTENSION_NAME
import static org.junit.jupiter.api.Assertions.*

class KotlinUtilsPluginSmokeTest {

    @Test
    void smokeTest() {
        //arrange
        Project project = ProjectBuilder.builder().build()
        //act
        project.plugins.apply(KotlinUtilsPlugin)
        def extension = project.extensions.getByName(EXTENSION_NAME)
        extension.kotlinVersion = '1.2.50'
        //assert
        assertNotNull(project.extensions.getByName(EXTENSION_NAME), EXTENSION_NAME)

        //assert no exception
        project.evaluate()
    }

    @Test
    void errorIfKotlinVersionNotDefined() {
        //arrange
        Project project = ProjectBuilder.builder().build()
        project.plugins.apply(KotlinUtilsPlugin)
        assertThrowsIllegalStateKotlinVersionNotDefined { project.ext.kotlinStdlib() }
        assertThrowsIllegalStateKotlinVersionNotDefined { project.ext.kotlinStdlibJs() }
        assertThrowsIllegalStateKotlinVersionNotDefined { project.ext.kotlinStdlibCommon() }
        assertThrowsIllegalStateKotlinVersionNotDefined { project.ext.kotlinReflect() }
    }

    @Test
    void checkIfExcludingWorks() {
        Project project = ProjectBuilder.builder().build()
        project.plugins.apply('java')
        project.plugins.apply(KotlinUtilsPlugin)
        project.repositories {
            maven { url "http://dl.bintray.com/robstoll/tutteli-jars" }
        }
        project.dependencies {
            testCompile "ch.tutteli.atrium:atrium-cc-en_GB-robstoll:0.7.0", project.excluding {
                kotlin()
                kbox()
                atriumVerbs()
            }
        }
    }

    private static void assertThrowsIllegalStateKotlinVersionNotDefined(Executable executable) {
        //act
        def ex = assertThrows(IllegalStateException, executable)
        //assert
        assertEquals(KotlinUtilsPlugin.ERR_KOTLIN_VERSION, ex.message)
    }
}
