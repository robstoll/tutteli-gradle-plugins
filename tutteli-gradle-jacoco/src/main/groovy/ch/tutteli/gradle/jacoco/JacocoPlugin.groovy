package ch.tutteli.gradle.jacoco

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.junit.platform.gradle.plugin.JUnitPlatformPlugin

class JacocoPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        Task junitPlatformTestTask = applyJunitPlatform(project)
        applyJacoco(project, junitPlatformTestTask)
    }

    private static Task applyJunitPlatform(Project project) {
        project.getPluginManager().apply(JUnitPlatformPlugin)

        def junitPlatformTestTask = project.tasks.getByName('junitPlatformTest')
        //TODO make configurable, with/without junit report
        def reportIndex = junitPlatformTestTask.args.findIndexOf { it == '--reports-dir' }
        def keep = (0..(junitPlatformTestTask.args.size() - 1)) - [reportIndex, reportIndex + 1]
        junitPlatformTestTask.args = junitPlatformTestTask.args[keep]
        return junitPlatformTestTask
    }

    private static void applyJacoco(Project project, Task junitPlatformTestTask) {
        project.getPluginManager().apply(org.gradle.testing.jacoco.plugins.JacocoPlugin)

        JacocoPluginExtension jacocoExtension = project.extensions.getByName('jacoco') as JacocoPluginExtension
        jacocoExtension.with {
            toolVersion = '0.8.0'
            applyTo junitPlatformTestTask
        }

        //TODO make configurable currently one would need to do tasks.getByName('junitPlatformJacocoReport')
        JacocoReport jacocoReport = project.task(type: JacocoReport, 'junitPlatformJacocoReport') as JacocoReport
        jacocoReport.with {
            sourceDirectories = project.sourceSets.main.allJava.sourceDirectories
            classDirectories = project.files(project.sourceSets.main.output.classesDirs)
            executionData junitPlatformTestTask
            reports {
                csv.enabled = false
                csv.destination project.file("${project.jacoco.reportsDir}/report.csv")
                xml.enabled = true
                xml.destination project.file("${project.jacoco.reportsDir}/report.xml")
                html.enabled = false
                html.destination project.file("${project.jacoco.reportsDir}/html/")
            }
        }
        project.check.dependsOn jacocoReport
    }

}
