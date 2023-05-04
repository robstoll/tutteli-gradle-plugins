package ch.tutteli.gradle.plugins.test


import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.function.Executable

import static org.junit.jupiter.api.Assertions.*

class Asserts {
    static final String NL_INDENT = "\r?\n\\s+"

    static void assertJvmJsInOutput(BuildResult result, String prefix) {
        assertProjectInOutput(result, prefix + '-common')
        assertProjectInOutput(result, prefix + '-js')
        assertProjectInOutput(result, prefix + '-jvm')
    }

    static void assertJvmJsAndroidInOutput(BuildResult result, String prefix) {
        assertJvmJsInOutput(result, prefix)
        assertProjectInOutput(result, prefix + '-android')
    }

    static void assertProjectInOutput(BuildResult result, String projectName) {
        assertTrue(result.output.contains(projectName), "project $projectName in output: ${result.output}")
    }

    static void assertStatusOk(BuildResult result, String taskName) {
        assertStatusOk(result, [taskName], [], [])
    }

    static void assertTaskRunSuccessfully(BuildResult result, String taskName) {
        def task = result.task(taskName)
        assertNotNull(task, "looks like $taskName did not run\n${result.output}")
        assertEquals(TaskOutcome.SUCCESS, task.outcome, "task $taskName did not run successfully, outcome was $task.outcome\n${result.output}")
    }

    static void assertTaskNotInvolved(BuildResult result, String taskName) {
        assertNull(result.task(taskName), "$taskName should not run but did\n${result.output}")
    }

    @Override
    void setMetaClass(MetaClass metaClass) {
        super.setMetaClass(metaClass)
    }

    static void assertStatusOk(
        BuildResult result,
        List<String> success,
        List<String> skipped,
        List<String> upToDate
    ) {
        assertEquals(success, result.taskPaths(TaskOutcome.SUCCESS), "SUCCESS was different")
        assertEquals(skipped, result.taskPaths(TaskOutcome.SKIPPED), "SKIPPED was different")
        assertEquals(upToDate, result.taskPaths(TaskOutcome.UP_TO_DATE), "UP_TO_DATE was different")
        def failed = result.taskPaths(TaskOutcome.FAILED)
        assertTrue(failed.empty, 'FAILED is empty but was not: ' + failed)
    }

    static void assertContainsRegex(String content, String what, String regex) {
        def matcher = content =~ regex
        assertTrue(
            matcher.find(),
            what + " should be in content\nRegex: ${regex.replace("\r", "\\r").replace("\n", "\\n")}\n" + content
        )
    }

    static void assertContainsNotRegex(String content, String what, String regex) {
        def matcher = content =~ regex
        assertFalse(matcher.find(), what + " should not be in content.\n" + content)
    }

    static void assertThrowsProjectConfigExceptionWithCause(Class<? extends Throwable> cause, String message, Executable executable) {
        def exception = assertThrows(ProjectConfigurationException) {
            executable.execute()
        }
        //assert
        assertEquals(cause, exception.cause.class)
        assertEquals(message, exception.cause.message)
    }

    static void assertTaskExists(Project project, String taskName) {
        def task = project.tasks.findByName(taskName)
        if (task == null) {
            fail("could not find task $taskName, following where defined:\n${project.tasks.getNames().join("\n")}")
        }
    }
}
