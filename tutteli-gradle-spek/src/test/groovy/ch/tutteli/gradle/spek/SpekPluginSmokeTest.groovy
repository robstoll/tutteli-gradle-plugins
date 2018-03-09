package ch.tutteli.gradle.spek

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

import static ch.tutteli.gradle.spek.SpekPlugin.EXTENSION_NAME
import static org.junit.jupiter.api.Assertions.*

class SpekPluginSmokeTest {

    @Test
    void smokeTest() {
        //arrange
        Project project = ProjectBuilder.builder().build()
        //act
        project.plugins.apply(SpekPlugin)
        //assert
        assertNotNull(project.extensions.getByName(EXTENSION_NAME), EXTENSION_NAME)
        assertNotNull(project.extensions.getByName('junitjacoco'), 'junitjacoco')
    }
}
