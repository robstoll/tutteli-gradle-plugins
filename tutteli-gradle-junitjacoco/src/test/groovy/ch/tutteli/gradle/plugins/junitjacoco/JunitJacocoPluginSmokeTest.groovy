package ch.tutteli.gradle.plugins.junitjacoco

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testing.jacoco.tasks.JacocoReport
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
        assertNotNull(project.extensions.getByName(JunitJacocoPlugin.EXTENSION_NAME), JunitJacocoPlugin.EXTENSION_NAME)

        assertNotNull(project.extensions.getByName('jacoco'), 'jacoco')
        def jacocoReport = project.tasks.getByName(JunitJacocoPlugin.JACOCO_TASK_NAME) as JacocoReport
        assertTrue(jacocoReport.reports.xml.required.get(), 'jacoco xml report should be enabled by default')
        assertFalse(jacocoReport.reports.csv.required.get(), 'jacoco csv report should be disabled by default')
        assertFalse(jacocoReport.reports.html.required.get(), 'jacoco html report should be disabled by default')
    }

    @Test
    void smokeTestWithJavaApplied(){
        //arrange
        Project project = ProjectBuilder.builder().build()
        //act
        project.plugins.apply(JavaPlugin)
        project.plugins.apply(JunitJacocoPlugin)

        project.evaluate()
        assertFalse(project.test.reports.junitXml.required.get(), "junitXml report should be disabled per default")

    }
}
