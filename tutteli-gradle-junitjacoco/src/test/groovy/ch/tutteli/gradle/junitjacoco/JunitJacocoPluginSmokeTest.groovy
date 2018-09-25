package ch.tutteli.gradle.junitjacoco

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

import static ch.tutteli.gradle.junitjacoco.JunitJacocoPlugin.*
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
        assertNotNull(project.extensions.getByName('junitPlatform'), 'junitPlatform')
        def junitPlatformTestTask = project.tasks.getByName(JUNIT_TASK_NAME)

        assertNotNull(project.extensions.getByName('jacoco'), 'jacoco')
        def jacocoReport = project.tasks.getByName(REPORT_TASK_NAME)
        assertTrue(jacocoReport.reports.xml.enabled as Boolean, 'jacoco xml report is enabled by default')
        assertFalse(jacocoReport.reports.csv.enabled as Boolean, 'jacoco csv report is disabled by default')
        assertFalse(jacocoReport.reports.html.enabled as Boolean, 'jacoco html report is disabled by default')

        project.evaluate()
        assertTrue(junitPlatformTestTask.args.size > 1, "$JUNIT_TASK_NAME was evaluated (has args)")
        assertTrue(junitPlatformTestTask.args.indexOf(ARG_REPORTS_DIR) == -1, "$ARG_REPORTS_DIR is absent per default")
    }
}
