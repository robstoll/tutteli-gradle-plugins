package ch.tutteli.gradle.junitjacoco

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.junit.platform.gradle.plugin.JUnitPlatformPlugin

class JunitJacocoPlugin implements Plugin<Project> {
    private static final Logger LOGGER = Logging.getLogger(JunitJacocoPlugin.class)

    static final String ARG_REPORTS_DIR = '--reports-dir'
    static final String JUNIT_TASK_NAME = 'junitPlatformTest'
    static final String EXTENSION_NAME = 'junitjacoco'
    static final String REPORT_TASK_NAME = 'junitPlatformJacocoReport'

    @Override
    void apply(Project project) {
        project.getPluginManager().apply(JUnitPlatformPlugin)
        project.getPluginManager().apply(JacocoPlugin)
        def junitPlatformTestTask = project.tasks.getByName(JUNIT_TASK_NAME)
        def extension = project.extensions.create(EXTENSION_NAME, JunitJacocoPluginExtension, project, junitPlatformTestTask)

        project.afterEvaluate {
            def keepItEnabled = extension.enableJunitReport.get()
            LOGGER.debug("enable junit report: ${keepItEnabled}")
            if (!keepItEnabled) {
                List args = junitPlatformTestTask.args
                def reportIndex = args.findIndexOf { it == ARG_REPORTS_DIR }
                if (reportIndex != -1) {
                    def keep = (0..(args.size() - 1)) - [reportIndex, reportIndex + 1]
                    junitPlatformTestTask.args = args[keep]
                } else {
                    LOGGER.warn("junit report was already disabled")
                }
            }
        }
    }
}
