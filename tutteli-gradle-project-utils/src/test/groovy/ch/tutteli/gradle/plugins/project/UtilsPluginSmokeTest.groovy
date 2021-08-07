package ch.tutteli.gradle.plugins.project

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

import static ch.tutteli.gradle.plugins.project.UtilsPlugin.TASK_NAME_TEST_JAR
import static ch.tutteli.gradle.plugins.project.UtilsPlugin.TASK_NAME_TEST_SOURCES_JAR
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertThrows

class UtilsPluginSmokeTest {

    @Test
    void smokeTest_nothignCalled_taskNotCreated() {
        //arrange
        Project project = ProjectBuilder.builder().build()
        //act
        project.plugins.apply(UtilsPlugin)
        //assert
        assertNull(project.tasks.findByName(TASK_NAME_TEST_JAR), TASK_NAME_TEST_JAR)
        assertNull(project.tasks.findByName(TASK_NAME_TEST_SOURCES_JAR), TASK_NAME_TEST_SOURCES_JAR)

        //assert no exception
        project.evaluate()
    }

    @Test
    void smokeTest_createTasks_created() {
        //arrange
        Project project = ProjectBuilder.builder().build()
        //act
        project.plugins.apply(UtilsPlugin)
        project.plugins.apply('java')
        project.ext.createTestJarTask(project)
        project.ext.createTestSourcesJarTask(project)
        //assert
        project.tasks.getByName(TASK_NAME_TEST_JAR)
        project.tasks.getByName(TASK_NAME_TEST_SOURCES_JAR)

        //assert no exception
        project.evaluate()
    }

    @Test
    void smokeTest_createTasksNoProject_Throws() {
        //arrange
        Project project = ProjectBuilder.builder().build()
        //act
        project.plugins.apply(UtilsPlugin)
        project.plugins.apply('java')
        //assert
        assertThrows(IllegalStateException) {
            project.ext.createTestJarTask()
        }
        assertThrows(IllegalStateException) {
            project.ext.createTestSourcesJarTask()
        }
        assertNull(project.tasks.findByName(TASK_NAME_TEST_JAR), TASK_NAME_TEST_JAR)
        assertNull(project.tasks.findByName(TASK_NAME_TEST_SOURCES_JAR), TASK_NAME_TEST_SOURCES_JAR)

        //assert no exception
        project.evaluate()
    }

    @Test
    void smokeTest_createTasksNoSourceSets_Throws() {
        //arrange
        Project project = ProjectBuilder.builder().build()
        //act
        project.plugins.apply(UtilsPlugin)
        //assert
        assertThrows(IllegalStateException) {
            project.ext.createTestJarTask(project)
        }
        assertThrows(IllegalStateException) {
            project.ext.createTestSourcesJarTask(project)
        }
        assertNull(project.tasks.findByName(TASK_NAME_TEST_JAR), TASK_NAME_TEST_JAR)
        assertNull(project.tasks.findByName(TASK_NAME_TEST_SOURCES_JAR), TASK_NAME_TEST_SOURCES_JAR)

        //assert no exception
        project.evaluate()
    }
}
