package ch.tutteli.gradle.project

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

class UtilsPlugin implements Plugin<Project> {
    static final String TASK_NAME_TEST_JAR = 'testJar'
    static final String TASK_NAME_TEST_SOURCES_JAR = 'testSourcesJar'

    @Override
    void apply(Project project) {
        def prefixedProjectName = { String name ->
            ":${project.rootProject.name}-$name"
        }

        project.ext.prefixedProject = { String name ->
            project.project(prefixedProjectName(name))
        }
        project.ext.prefixedProjectName = prefixedProjectName


        project.ext.createTestJarTask = {
            if(!project.hasProperty('sourceSets')) throw illegalStateCannotCreate(TASK_NAME_TEST_JAR)
            project.tasks.create(name: TASK_NAME_TEST_JAR, type: Jar) {
                from project.sourceSets.test.output
                classifier = 'tests'
            }
        }
        project.ext.createTestSourcesJarTask = {
            if(!project.hasProperty('sourceSets')) throw illegalStateCannotCreate(TASK_NAME_TEST_SOURCES_JAR)
            project.tasks.create(name: TASK_NAME_TEST_SOURCES_JAR, type: Jar) {
                from project.sourceSets.test.allSource
                classifier = 'testsources'
            }
        }
    }

    private static IllegalStateException illegalStateCannotCreate(String what) {
        new IllegalStateException("Can only create a " + what + " if there is are sourceSets present. Did you forget to apply kotlin?")
    }
}
