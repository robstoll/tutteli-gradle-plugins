package ch.tutteli.gradle.plugins.project

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


        project.ext.createTestJarTask = { Project aProject ->
            if(aProject == null) throw new IllegalStateException("you need to pass a project to createTestJarTask")
            if(!aProject.hasProperty('sourceSets')) throw illegalStateCannotCreate(TASK_NAME_TEST_JAR)
            aProject.tasks.create(name: TASK_NAME_TEST_JAR, type: Jar) {
                from aProject.sourceSets.test.output
                archiveClassifier.set('tests')
            }
        }
        project.ext.createTestSourcesJarTask = { Project aProject ->
            if(aProject == null) throw new IllegalStateException("you need to pass a project to createTestSourcesJarTask")
            if(!aProject.hasProperty('sourceSets')) throw illegalStateCannotCreate(TASK_NAME_TEST_SOURCES_JAR)
            aProject.tasks.create(name: TASK_NAME_TEST_SOURCES_JAR, type: Jar) {
                from aProject.sourceSets.test.allSource
                archiveClassifier.set('testsources')
            }
        }
    }

    private static IllegalStateException illegalStateCannotCreate(String what) {
        new IllegalStateException("Can only create a " + what + " if there are sourceSets present. Did you forget to apply kotlin?")
    }
}
