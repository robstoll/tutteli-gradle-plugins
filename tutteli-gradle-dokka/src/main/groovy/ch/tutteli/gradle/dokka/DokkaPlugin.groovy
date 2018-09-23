package ch.tutteli.gradle.dokka


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.gradle.DokkaPlugin as JetbrainsDokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask

class DokkaPlugin implements Plugin<Project> {
    static final String EXTENSION_NAME = 'tutteliDokka'
    static final String JAVADOC_JAR_TASK_NAME = 'javadocJar'
    protected static final String ERR_GH_PAGES_WITHOUT_USER =
        "You need to define tutteliDokka.githubUser if you want to use tutteliDokka.ghPages"

    @Override
    void apply(Project project) {
        project.pluginManager.apply(JetbrainsDokkaPlugin)

        DokkaTask dokkaTask = project.tasks.getByName('dokka') as DokkaTask
        project.tasks.create(name: JAVADOC_JAR_TASK_NAME, type: Jar, dependsOn: dokkaTask) {
            from dokkaTask.outputDirectory
            classifier = 'javadoc'
        }

        def extension = project.extensions.create(EXTENSION_NAME, DokkaPluginExtension, project)
        project.afterEvaluate {
            if (extension.ghPages.getOrElse(false)) {
                if (!extension.githubUser.isPresent()) throw new IllegalStateException(ERR_GH_PAGES_WITHOUT_USER)
                def rootProject = project.rootProject
                if (!rootProject.version.endsWith("-SNAPSHOT")) {
                    dokkaTask.configure {
                        externalDocumentationLink {
                            url = new URL("https://${extension.githubUser.get()}.github.io/$rootProject.name/$rootProject.version/doc/")
                        }
                    }
                }
            }
        }
    }
}

