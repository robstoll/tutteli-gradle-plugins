package ch.tutteli.gradle.plugins.junitjacoco

import ch.tutteli.gradle.plugins.junitjacoco.generated.Dependencies
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.*
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.kotlin.dsl.create

class JunitJacocoPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        //TODO use apply from gradle-kotlinx with type parameter
        project.pluginManager.apply(JavaPlugin::class.java)
        project.pluginManager.apply(JacocoPlugin::class.java)

        // Add the 'greeting' extension object
        val extension = project.extensions.create<JunitJacocoPluginExtension>(EXTENSION_NAME, project)

        project.tasks.withType(Test::class.java) {
            useJUnitPlatform()
            reports.junitXml.required.set(false)
        }
        @Suppress("UNCHECKED_CAST")
        val jacocoReportTask = project.tasks.named(JACOCO_TASK_NAME) as TaskProvider<JacocoReport>
        project.tasks.named("check") {
            dependsOn(jacocoReportTask)
        }
        defaultConfig(project, jacocoReportTask)
        configureTestTasks(project, extension)
    }

    private fun defaultConfig(target: Project, jacocoReportTask: TaskProvider<JacocoReport>) {
        val jacocoPluginExtension = target.extensions.getByType(JacocoPluginExtension::class.java)
        jacocoPluginExtension.toolVersion = Dependencies.jacocoToolsVersion

        jacocoReportTask.configure {
            reports {

                csv.required.set(false)
                xml.required.set(true)
                html.required.set(false)

                val reportDir = target.file(jacocoPluginExtension.reportsDirectory)
                csv.outputLocation.set(reportDir.resolve("report.csv"))
                xml.outputLocation.set(reportDir.resolve("report.xml"))
                html.outputLocation.set(reportDir.resolve("html/"))
            }
        }
    }


    private fun configureTestTasks(project: Project, extension: JunitJacocoPluginExtension) {
        fun memoizeTestFile(testTask: AbstractTestTask) =
            project.file("${project.buildDir}/test-results/memoize-previous-state-${testTask.name}.txt")

        project.tasks.withType(AbstractTestTask::class.java) {
            testLogging {
                events(
                    TestLogEvent.FAILED,
                    TestLogEvent.SKIPPED,
                    TestLogEvent.STANDARD_OUT,
                    TestLogEvent.STANDARD_ERROR
                )
                exceptionFormat = TestExceptionFormat.FULL
                showExceptions = true
                showCauses = true
                showStackTraces = true
            }
            val testTask = this
            addTestListener(object : TestListener {
                override fun beforeSuite(suite: TestDescriptor) {}
                override fun beforeTest(testDescriptor: TestDescriptor) {}
                override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}
                override fun afterSuite(suite: TestDescriptor, result: TestResult) {
                    if (suite.parent == null) {
                        if (
                            result.testCount == 0L &&
                            extension.allowedTestTasksWithoutTests.get().contains(testTask.name).not()
                        ) {
                            throw GradleException("No tests executed, most likely the discovery failed.")
                        }
                        println("${testTask.name} Result: ${result.resultType} (${result.successfulTestCount} succeeded, ${result.failedTestCount} failed, ${result.skippedTestCount} skipped)")
                        if (testTask.doesNotFailIfFailedBefore) {
                            memoizeTestFile(testTask).writeText(result.resultType.toString())
                        }
                    }
                }
            })
        }
        project.afterEvaluate {
            project.tasks.withType(AbstractTestTask::class.java).forEach { testTask ->
                if (testTask.doesNotFailIfFailedBefore) {
                    val failIfTestFailedLastTime =
                        project.tasks.register("fail-if-${testTask.name}-failed-last-time") {
                            doLast {
                                if (!testTask.didWork) {
                                    val memoizeTestFile = memoizeTestFile(testTask)
                                    if (memoizeTestFile.exists() && memoizeTestFile.readText() == TestResult.ResultType.FAILURE.toString()) {
                                        val allTests = project.tasks.named("allTests")
                                        val reportFile = if (allTests.isPresent && allTests.get().didWork) {
                                            (allTests.get() as TestReport).destinationDir.resolve("index.html")
                                        } else {
                                            testTask.reports.html.entryPoint
                                        }
                                        throw GradleException(
                                            "test failed in last run, execute clean${testTask.name.capitalize()} to force its execution\n" +
                                                "See the following report for more information:\nfile://${reportFile.absolutePath}"
                                        )
                                    }
                                }
                            }
                        }
                    testTask.finalizedBy(failIfTestFailedLastTime)
                }
            }
        }
    }

    private val AbstractTestTask.doesNotFailIfFailedBefore
        get(): Boolean =
            name != "test"


    companion object {
        const val JACOCO_TASK_NAME = "jacocoTestReport"
        const val EXTENSION_NAME = "junitjacoco"
    }
}
