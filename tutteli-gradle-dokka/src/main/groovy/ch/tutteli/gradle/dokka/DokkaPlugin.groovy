package ch.tutteli.gradle.dokka


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.gradle.DokkaPlugin as JetbrainsDokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask

class DokkaPlugin implements Plugin<Project> {
    static final String EXTENSION_NAME = 'tutteliDokka'
    static final String JAVADOC_JAR_TASK_NAME = 'javadocJar'

    @Override
    void apply(Project project) {
        project.pluginManager.apply(JetbrainsDokkaPlugin)
        project.extensions.create(EXTENSION_NAME, DokkaPluginExtension, project)
        DokkaTask dokkaTask = project.tasks.getByName('dokka') as DokkaTask
        project.tasks.create(name: JAVADOC_JAR_TASK_NAME, type: Jar, dependsOn: dokkaTask) {
            from dokkaTask.outputDirectory
            classifier = 'javadoc'
        }
    }
}

