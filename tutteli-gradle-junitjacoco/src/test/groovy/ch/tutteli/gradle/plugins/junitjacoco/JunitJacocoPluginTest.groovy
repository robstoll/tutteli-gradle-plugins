package ch.tutteli.gradle.plugins.junitjacoco

import ch.tutteli.gradle.plugins.junitjacoco.JunitJacocoPlugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

import static ch.tutteli.gradle.plugins.junitjacoco.JunitJacocoPlugin.EXTENSION_NAME
import static org.junit.jupiter.api.Assertions.assertEquals

class JunitJacocoPluginTest {

    @Test
    void allowedTestTasksWithoutTests_defaultIsCorrect() {
        //arrange
        Project project = ProjectBuilder.builder().build()
        project.plugins.apply(JunitJacocoPlugin)
        def extension = project.extensions.getByName(EXTENSION_NAME) as JunitJacocoPluginExtension
        //act
        project.evaluate()
        //assert
        assertEquals(["jsBrowserTest"], extension.allowedTestTasksWithoutTests.get())
    }
}
