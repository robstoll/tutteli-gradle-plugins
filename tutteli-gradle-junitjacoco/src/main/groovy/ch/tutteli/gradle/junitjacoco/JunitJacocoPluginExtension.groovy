package ch.tutteli.gradle.junitjacoco

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.junit.platform.gradle.plugin.JUnitPlatformExtension

class JunitJacocoPluginExtension {
    private Task junitPlatformTestTask
    private JacocoPluginExtension jacocoPluginExtension
    private JacocoReport jacocoReportTask
    private JUnitPlatformExtension junitPlatformExtension

    Property<Boolean> enableJunitReport

    JunitJacocoPluginExtension(Project project, Task junitPlatformTestTask) {
        this.junitPlatformTestTask = junitPlatformTestTask
        this.jacocoPluginExtension = project.extensions.getByType(JacocoPluginExtension)
        this.jacocoReportTask = project.task(type: JacocoReport, JunitJacocoPlugin.REPORT_TASK_NAME) as JacocoReport
        this.junitPlatformExtension = project.extensions.getByType(JUnitPlatformExtension)
        this.enableJunitReport = project.objects.property(Boolean)
        this.enableJunitReport.set(false)
        project.check.dependsOn jacocoReportTask
        defaultConfig()
    }

    private void defaultConfig() {

        //necessary that it is accessible within the closure without the need of a public getter
        def junitTask = junitPlatformTestTask
        jacoco {
            toolVersion = '0.8.2'
            applyTo junitTask
        }

        jacocoReport {
            sourceDirectories = project.sourceSets.main.allJava.sourceDirectories
            classDirectories = project.files(project.sourceSets.main.output.classesDirs)
            executionData junitTask
            reports {
                csv.enabled = false
                csv.destination project.file("${project.jacoco.reportsDir}/report.csv")
                xml.enabled = true
                xml.destination project.file("${project.jacoco.reportsDir}/report.xml")
                html.enabled = false
                html.destination project.file("${project.jacoco.reportsDir}/html/")
            }
        }
    }

    void jacoco(Action<JacocoPluginExtension> configure) {
        configure.execute(jacocoPluginExtension)
    }

    void jacocoReport(Action<JacocoReport> configure) {
        configure.execute(jacocoReportTask)
    }

    void junitPlatform(Action<JUnitPlatformExtension> configure) {
        configure.execute(junitPlatformExtension)
    }
}
