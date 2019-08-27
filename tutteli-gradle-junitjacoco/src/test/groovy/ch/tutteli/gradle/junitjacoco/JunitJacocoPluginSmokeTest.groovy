package ch.tutteli.gradle.junitjacoco

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.junit.jupiter.api.Test

import static ch.tutteli.gradle.junitjacoco.JunitJacocoPlugin.getEXTENSION_NAME
import static ch.tutteli.gradle.junitjacoco.JunitJacocoPlugin.getJACOCO_TASK_NAME
import static org.junit.jupiter.api.Assertions.*

class JunitJacocoPluginSmokeTest {

    @Test
    void smokeTest() {
        //arrange
        Project project = ProjectBuilder.builder().build()
        //act
        project.plugins.apply(JunitJacocoPlugin)
        //assert
        assertNotNull(project.extensions.getByName(EXTENSION_NAME), EXTENSION_NAME)

        assertNotNull(project.extensions.getByName('jacoco'), 'jacoco')
        def jacocoReport = project.tasks.getByName(JACOCO_TASK_NAME) as JacocoReport
        assertTrue(jacocoReport.reports.xml.enabled, 'jacoco xml report is enabled by default')
        assertFalse(jacocoReport.reports.csv.enabled, 'jacoco csv report is disabled by default')
        assertFalse(jacocoReport.reports.html.enabled, 'jacoco html report is disabled by default')

        project.evaluate()
        assertFalse(project.test.reports.junitXml.enabled, "junitXml report is disabled per default")
    }
}
