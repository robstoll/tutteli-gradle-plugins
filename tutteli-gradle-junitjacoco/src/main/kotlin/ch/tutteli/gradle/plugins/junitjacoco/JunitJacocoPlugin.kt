package ch.tutteli.gradle.plugins.junitjacoco

import ch.tutteli.gradle.plugins.junitjacoco.generated.Dependencies
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.*
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType

class JunitJacocoPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        //TODO use apply from gradle-kotlinx with type parameter
        project.pluginManager.apply(JavaPlugin::class.java)
        project.pluginManager.apply(JacocoPlugin::class.java)

        val extension = project.extensions.create<JunitJacocoPluginExtension>(EXTENSION_NAME, project)

        project.tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            reports.junitXml.required.set(false)
        }

        defaultConfig(project, extension)
        configureTestTasks(project, extension)
    }

    private fun defaultConfig(target: Project, extension: JunitJacocoPluginExtension) {
        @Suppress("UNCHECKED_CAST")
        val jacocoReportTask = target.tasks.named(JACOCO_TASK_NAME) as TaskProvider<JacocoReport>
        target.tasks.named("check") {
            dependsOn(jacocoReportTask)
        }
        val jacocoPluginExtension = target.extensions.getByType(JacocoPluginExtension::class.java)
        jacocoPluginExtension.toolVersion = Dependencies.jacocoToolsVersion

        jacocoReportTask.configure {

            if (target.plugins.findPlugin("org.jetbrains.kotlin.multiplatform") != null) {

                configureProjectSources(target)

                executionData.setFrom(target.files("${target.buildDir}/jacoco/jvmTest.exec"))
                dependsOn(target.tasks.named("jvmTest"))
            }

            extension.additionalProjectSources.get().forEach { otherProject ->
                configureProjectSources(otherProject)
            }

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

    private fun JacocoReport.configureProjectSources(project: Project) {
        sourceDirectories.from(project.files(coverageSourceDirs))
        classDirectories.from(project.layout.buildDirectory.map { it.dir("classes/kotlin/jvm/main").asFileTree })
    }

    private fun configureTestTasks(project: Project, extension: JunitJacocoPluginExtension) {

        project.tasks.withType<AbstractTestTask>().configureEach {
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
                            determineMemoizeTestFile(project, testTask).writeText(result.resultType.toString())
                        }
                    }
                }
            })
        }
        project.afterEvaluate {
            // TODO 5.0.0 check if really still needed (maybe fixed in newer gradle versions) and if so, then find a
            // way that we can still use configureEach instead of all in order that we can use configuration-cache
            project.tasks.withType<AbstractTestTask>().all {
                val testTask = this
                if (testTask.doesNotFailIfFailedBefore) {
                    testTask.finalizedBy(failIfTestFailedLastTimeTask(project, testTask))
                }
            }
        }
    }

    private fun determineMemoizeTestFile(project: Project, testTask: AbstractTestTask) =
        project.file("${project.buildDir}/test-results/memoize-previous-state-${testTask.name}.txt")


    private fun failIfTestFailedLastTimeTask(
        project: Project,
        testTask: AbstractTestTask
    ): TaskProvider<Task> = project.tasks.register("fail-if-${testTask.name}-failed-last-time") {
        doLast {
            if (testTask.didWork.not()) {
                val memoizeTestFile = determineMemoizeTestFile(project, testTask)
                if (memoizeTestFile.exists() && memoizeTestFile.readText() == TestResult.ResultType.FAILURE.toString()) {
                    val allTests = project.tasks.named<TestReport>("allTests")
                    val reportFile = if (allTests.isPresent && allTests.get().didWork) {
                        allTests.get().destinationDirectory.file("index.html").get().asFile
                    } else {
                        testTask.reports.html.entryPoint
                    }
                    val projectPrefix = if (project == project.rootProject) "" else ":${project.name}"
                    throw GradleException(
                        "test failed in last run, execute ${projectPrefix}clean${testTask.name.capitalize()} to force its execution\n" +
                            "See the following report for more information:\nfile://${reportFile.absolutePath}"
                    )
                }
            }
        }
    }

    private val AbstractTestTask.doesNotFailIfFailedBefore get(): Boolean = name != "test"


    companion object {
        const val JACOCO_TASK_NAME = "jacocoTestReport"
        const val EXTENSION_NAME = "junitjacoco"
        private val coverageSourceDirs = arrayOf(
            "src/commonMain",
            "src/jvmMain",
            "src/main"
        )

    }
}
