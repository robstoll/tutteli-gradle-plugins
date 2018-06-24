package ch.tutteli.gradle.spek

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJsPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.junit.jupiter.api.Test

import static ch.tutteli.gradle.spek.SpekPlugin.EXTENSION_NAME
import static org.junit.jupiter.api.Assertions.*

class SpekPluginSmokeTest {

    @Test
    void smokeTest_KotlinPlugin() {
        smokeTest(KotlinPluginWrapper)
    }

    @Test
    void smokeTest_KotlinPlatformJvmPlugin() {
        smokeTest(KotlinPlatformJvmPlugin)
    }

    private static void smokeTest(Class<? extends Plugin> plugin) {
        //arrange
        Project project = ProjectBuilder.builder().build()
        //act
        project.plugins.apply(plugin)
        project.plugins.apply(SpekPlugin)
        //assert
        assertNotNull(project.extensions.getByName(EXTENSION_NAME), EXTENSION_NAME)
        assertNotNull(project.extensions.getByName('junitjacoco'), 'junitjacoco')
    }


    @Test
    void errorIfKotlinNotApplied() {
        //arrange
        Project project = ProjectBuilder.builder().build()
        //pre-assert
        def ex = assertThrows(PluginApplicationException) {
            //act
            project.plugins.apply(SpekPlugin)
        }
        //assert
        assertEquals(IllegalStateException, ex.cause.class)
        assertEquals(SpekPlugin.ERR_KOTLIN_PLUGIN, ex.cause.message)

    }
}
