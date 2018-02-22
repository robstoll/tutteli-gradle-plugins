package ch.tutteli.gradle.jacoco

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertNotNull

class JacocoPluginSmokeTest {

    @Test
    void smokeTest(){
        //arrange
        Project project = ProjectBuilder.builder().build()
        //act
        project.plugins.apply(JacocoPlugin)
        //assert
        assertNotNull(project.tasks.getByName('junitPlatformTest'))
        assertNotNull(project.extensions.getByName('jacoco'))
        assertNotNull(project.tasks.getByName('junitPlatformJacocoReport'))
    }
}
