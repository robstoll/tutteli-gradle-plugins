package ch.tutteli.gradle.junitjacoco

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

import static ch.tutteli.gradle.junitjacoco.JunitJacocoPlugin.ARG_REPORTS_DIR
import static ch.tutteli.gradle.junitjacoco.JunitJacocoPlugin.JUNIT_TASK_NAME
import static ch.tutteli.gradle.junitjacoco.JunitJacocoPlugin.EXTENSION_NAME
import static org.junit.jupiter.api.Assertions.*

class JunitJacocoPluginTest {

    @Test
    void enableJunitReport_true_ReportsDirIsPartOfTheArguments() {
        //arrange
        Project project = ProjectBuilder.builder().build()
        project.plugins.apply(JunitJacocoPlugin)
        def extension = project.extensions.getByName(EXTENSION_NAME)
        extension.enableJunitReport = true
        //act
        project.evaluate()
        //assert
        def junitPlatformTestTask = project.tasks.getByName(JUNIT_TASK_NAME)
        assertTrue(junitPlatformTestTask.args.size > 1, "$JUNIT_TASK_NAME was evaluated (has args)")
        assertTrue(junitPlatformTestTask.args.indexOf(ARG_REPORTS_DIR) != -1, "$ARG_REPORTS_DIR is present")
    }
}
