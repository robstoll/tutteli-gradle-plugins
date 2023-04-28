package ch.tutteli.gradle.plugins.spek


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.junit.jupiter.api.Test

import static ch.tutteli.gradle.plugins.spek.SpekPlugin.EXTENSION_NAME
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
    @Test
    void smokeTest_KotlinMultiPlatformPlugin() {
        smokeTest(KotlinMultiplatformPluginWrapper)
    }

    private static void smokeTest(Class<? extends Plugin> plugin) {
        //arrange
        Project project = ProjectBuilder.builder().build()
        //act
        project.plugins.apply(plugin)
        project.plugins.apply(SpekPlugin)
        //assert
        assertNotNull(project.extensions.getByName(EXTENSION_NAME), EXTENSION_NAME)
        assertNotNull(project.plugins.findPlugin("ch.tutteli.gradle.plugins.junitjacoco"))
    }
}
