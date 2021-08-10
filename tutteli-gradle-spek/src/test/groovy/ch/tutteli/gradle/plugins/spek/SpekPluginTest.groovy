package ch.tutteli.gradle.plugins.spek

import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.file.FileCollection
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.junit.jupiter.api.Test

import static ch.tutteli.gradle.plugins.spek.SpekPlugin.EXTENSION_NAME
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

class SpekPluginTest {

    @Test
    void version_1_throwsIllegalStateException() {
        //arrange
        Project project = ProjectBuilder.builder().build()
        project.plugins.apply(KotlinPluginWrapper)
        project.plugins.apply(SpekPlugin)
        def extension = project.extensions.getByName(EXTENSION_NAME)
        extension.version = '1.1.2'
        //act
        def exception = assertThrows(ProjectConfigurationException){
            project.evaluate()
        }
        assertEquals("spek 1 is no longer supported by this plugin.", exception.cause.message)
    }
}
