package ch.tutteli.gradle.test

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

    static void assertProjectInOutput(BuildResult result, String projectName) {
        assertTrue(result.output.contains(projectName), "project $projectName in output: ${result.output}")
    }

    static void assertStatusOk(BuildResult result, String taskName) {
        assertStatusOk(result, [taskName], [], [])
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

    static void assertContainsRegex(String pom, String what, String regex) {
        def matcher = pom =~ regex
        assertTrue(matcher.find(), what + "\n" + pom)
    }

    static void assertThrowsProjectConfigExceptionWithCause(Class<? extends Throwable> cause, String message, Executable executable) {
        def exception = assertThrows(ProjectConfigurationException) {
            executable.execute()
        }
        //assert
        assertEquals(cause, exception.cause.class)
        assertEquals(message, exception.cause.message)
    }
}
