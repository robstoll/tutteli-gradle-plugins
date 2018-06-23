package ch.tutteli.gradle.kotlin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformCommonPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJsPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.*

class KotlinUtilsPluginSmokeTest {

    @Test
    void smokeTest_KotlinPlugin() {
        smokeTest(KotlinPluginWrapper)
    }

    @Test
    void smokeTest_KotlinPlatformJvmPlugin() {
        smokeTest(KotlinPlatformJvmPlugin)
    }

    @Test
    void smokeTest_KotlinPlatformJsPlugin() {
        smokeTest(KotlinPlatformJsPlugin)
    }

    @Test
    void smokeTest_KotlinPlatformCommonPlugin() {
        smokeTest(KotlinPlatformCommonPlugin)
    }

    private static void smokeTest(Class<? extends Plugin> plugin) {
        //arrange
        Project project = ProjectBuilder.builder().build()
        //act
        project.plugins.apply(plugin)
        project.plugins.apply(KotlinUtilsPlugin)
        //assert
    }


    @Test
    void errorIfKotlinNotApplied() {
        //arrange
        Project project = ProjectBuilder.builder().build()
        //pre-assert
        def ex = assertThrows(PluginApplicationException) {
            //act
            project.plugins.apply(KotlinUtilsPlugin)
        }
        //assert
        assertEquals(IllegalStateException, ex.cause.class)
    }
}
