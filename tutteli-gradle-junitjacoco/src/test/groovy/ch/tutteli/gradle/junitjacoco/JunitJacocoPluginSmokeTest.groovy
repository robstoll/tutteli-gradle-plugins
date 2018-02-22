package ch.tutteli.gradle.junitjacoco

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.*

class JunitJacocoPluginSmokeTest {

    @Test
    void smokeTest() {
        //arrange
        Project project = ProjectBuilder.builder().build()
        //act
        project.plugins.apply(JunitJacocoPlugin)
        //assert
        def junitjacoco = project.extensions.getByName('junitjacoco')
        assertNotNull(junitjacoco, 'junitjacoco')
        assertNotNull(project.extensions.getByName('junitPlatform'), 'junitPlatform')
        assertNotNull(project.tasks.getByName('junitPlatformTest'), 'junitPlatformTest')
        assertNotNull(project.extensions.getByName('jacoco'), 'jacoco')
        def jacocoReport = project.tasks.getByName('junitPlatformJacocoReport')
        assertNotNull(jacocoReport, 'junitPlatformJacocoReport')
        assertTrue(jacocoReport.reports.xml.enabled as Boolean, 'jacoco xml report is enabled by default')
        assertFalse(jacocoReport.reports.csv.enabled as Boolean, 'jacoco csv report is disabled by default')
        assertFalse(jacocoReport.reports.html.enabled as Boolean, 'jacoco html report is disabled by default')
    }
}
