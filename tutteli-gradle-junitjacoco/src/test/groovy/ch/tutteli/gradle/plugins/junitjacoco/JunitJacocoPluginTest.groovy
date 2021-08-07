package ch.tutteli.gradle.plugins.junitjacoco

import ch.tutteli.gradle.plugins.junitjacoco.JunitJacocoPlugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

import static ch.tutteli.gradle.plugins.junitjacoco.JunitJacocoPlugin.EXTENSION_NAME
import static org.junit.jupiter.api.Assertions.assertTrue

class JunitJacocoPluginTest {

    @Test
    void enableJunitReport_true_JunitReportEnabledInGradle() {
        //arrange
        Project project = ProjectBuilder.builder().build()
        project.plugins.apply(JunitJacocoPlugin)
        def extension = project.extensions.getByName(EXTENSION_NAME)
        extension.enableJunitReport = true
        //act
        project.evaluate()
        //assert
        assertTrue(project.test.reports.junitXml.enabled)
    }
}
