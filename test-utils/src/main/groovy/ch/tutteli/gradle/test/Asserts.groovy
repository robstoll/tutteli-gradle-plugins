package ch.tutteli.gradle.test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class Asserts {
    static void assertProjectInOutput(BuildResult result, String projectName) {
        assertTrue(result.output.contains(projectName), "project $projectName in output: ${result.output}")
    }

    static void assertStatusOk(BuildResult result) {
        assertEquals([':projects'], result.taskPaths(TaskOutcome.SUCCESS))
        assertTrue(result.taskPaths(TaskOutcome.SKIPPED).empty, 'SKIPPED is empty')
        assertTrue(result.taskPaths(TaskOutcome.UP_TO_DATE).empty, 'UP_TO_DATE is empty')
        assertTrue(result.taskPaths(TaskOutcome.FAILED).empty, 'FAILED is empty')
    }
}
