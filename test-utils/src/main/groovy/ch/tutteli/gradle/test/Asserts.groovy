package ch.tutteli.gradle.test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class Asserts {
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
}
