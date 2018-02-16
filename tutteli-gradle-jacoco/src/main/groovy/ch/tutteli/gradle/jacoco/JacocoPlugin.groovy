package ch.tutteli.gradle.jacoco

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.testing.jacoco.tasks.JacocoReport

class JacocoPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {

        //TODO check if we can modify buildscript classpath but I doubt it
//        project.buildscript.dependencies {
//            classpath "org.junit.platform:junit-platform-gradle-plugin:1.0.3"
//        }
        project.getPluginManager().apply('jacoco')
        project.getPluginManager().apply('org.junit.platform.gradle.plugin')

        def junitPlatformTestTask = project.tasks.getByName('junitPlatformTest')
        //TODO make configurable, with/without junit report
        def reportIndex = junitPlatformTestTask.args.findIndexOf { it == '--reports-dir' }
        def keep = (0..(junitPlatformTestTask.args.size() - 1)) - [reportIndex, reportIndex + 1]
        junitPlatformTestTask.args = junitPlatformTestTask.args[keep]

        project.jacoco {
            toolVersion = '0.8.0'
            applyTo junitPlatformTestTask
        }

        //TODO make configurable currently one would need to do tasks.getByName('junitPlatformJacocoReport')
        def jacocoReport = project.task(type: JacocoReport, 'junitPlatformJacocoReport',
            {
                sourceDirectories = project.sourceSets.main.allJava.sourceDirectories
                classDirectories = project.files(project.sourceSets.main.output.classesDirs)
                executionData junitPlatformTestTask
                reports {
                    csv.enabled = false
                    csv.destination project.file("${project.jacoco.reportsDir}/junitReport.csv")
                    xml.enabled = false
                    xml.destination project.file("${project.jacoco.reportsDir}/junitReport.xml")
                    html.enabled = false
                    html.destination project.file("${project.jacoco.reportsDir}/junitHtml/")
                }
            })
        project.check.dependsOn jacocoReport
    }
}
