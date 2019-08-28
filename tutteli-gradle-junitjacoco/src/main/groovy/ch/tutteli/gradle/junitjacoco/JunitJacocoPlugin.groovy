package ch.tutteli.gradle.junitjacoco

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoPlugin

class JunitJacocoPlugin implements Plugin<Project> {
    private static final Logger LOGGER = Logging.getLogger(JunitJacocoPlugin.class)

    static final String EXTENSION_NAME = 'junitjacoco'
    static final String TEST_TASK_NAME = 'test'
    static final String JACOCO_TASK_NAME = 'jacocoTestReport'

    @Override
    void apply(Project project) {
        project.pluginManager.apply(JavaPlugin)
        project.pluginManager.apply(JacocoPlugin)
        def extension = project.extensions.create(EXTENSION_NAME, JunitJacocoPluginExtension, project)
        project.tasks.getByName(TEST_TASK_NAME) { Test test ->
            test.useJUnitPlatform()
        }

        project.afterEvaluate {
            def keepItEnabled = extension.enableJunitReport.get()
            LOGGER.debug("enable junit report: ${keepItEnabled}")
            if (!keepItEnabled) {
                project.tasks.getByName(TEST_TASK_NAME) { Test test ->
                    test.reports.junitXml.enabled = false
                }
            }
        }
    }
}
