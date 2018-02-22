package ch.tutteli.gradle.junitjacoco

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.junit.platform.gradle.plugin.JUnitPlatformPlugin

class JunitJacocoPlugin implements Plugin<Project> {
    private static final Logger LOGGER = Logging.getLogger(JunitJacocoPlugin.class)

    @Override
    void apply(Project project) {
        project.getPluginManager().apply(JUnitPlatformPlugin)
        project.getPluginManager().apply(JacocoPlugin)
        def junitPlatformTestTask = project.tasks.getByName('junitPlatformTest')
        def extension = project.extensions.create('junitjacoco', JunitJacocoPluginExtension, project, junitPlatformTestTask)

        project.afterEvaluate {
            def keepItEnabled = extension.enableJunitReport.get()
            LOGGER.debug("enable junit report: ${keepItEnabled}")
            if (!keepItEnabled) {
                List args = junitPlatformTestTask.args
                def reportIndex = args.findIndexOf { it == '--reports-dir' }
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
