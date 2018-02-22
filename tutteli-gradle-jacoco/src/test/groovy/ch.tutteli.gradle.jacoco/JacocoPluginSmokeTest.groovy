package ch.tutteli.gradle.jacoco

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue

class JacocoPluginSmokeTest {

    @Test
    void smokeTest(){
        //arrange
        Project project = ProjectBuilder.builder().build()
        //act
        project.plugins.apply(JacocoPlugin)
        //assert
        assertNotNull(project.tasks.getByName('junitPlatformTest'), 'junitPlatformTest')
        assertNotNull(project.extensions.getByName('jacoco'), 'jacoco')
        def jacocoReport = project.tasks.getByName('junitPlatformJacocoReport')
        assertNotNull(jacocoReport, 'junitPlatformJacocoReport')
        assertTrue(jacocoReport.reports.xml.enabled as Boolean, 'jacoco xml report is enabled')
    }
}
