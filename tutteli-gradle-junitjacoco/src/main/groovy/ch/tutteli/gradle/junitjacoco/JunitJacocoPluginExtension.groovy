package ch.tutteli.gradle.junitjacoco

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

class JunitJacocoPluginExtension {
    private JacocoPluginExtension jacocoPluginExtension
    private JacocoReport jacocoReportTask

    /**
     * @deprecated Use the gradle API instead: test { reports { junitXml { enabled = false }}}
     */
    @Deprecated(/*since = "0.28.0"*/)
    Property<Boolean> enableJunitReport

    JunitJacocoPluginExtension(Project project) {
        this.jacocoPluginExtension = project.extensions.getByType(JacocoPluginExtension)
        this.jacocoReportTask = project.tasks.getByName(JunitJacocoPlugin.JACOCO_TASK_NAME) as JacocoReport
        this.enableJunitReport = project.objects.property(Boolean)
        this.enableJunitReport.set(false)
        project.check.dependsOn jacocoReportTask
        defaultConfig()
    }

    private void defaultConfig() {

        jacoco {
            toolVersion = '0.8.3'
        }

        jacocoReportTask.configure {
            sourceSets project.sourceSets.main
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

    /**
     * @deprecated Use the gradle API instead: jacocoTestReport {...}
     */
    @Deprecated(/*since = "0.28.0"*/)
    void jacocoReport(Action<JacocoReport> configure) {
        configure.execute(jacocoReportTask)
    }
}
