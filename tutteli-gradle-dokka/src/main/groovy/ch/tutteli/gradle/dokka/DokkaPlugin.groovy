package ch.tutteli.gradle.dokka


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.dokka.gradle.DokkaPlugin as JetbrainsDokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.LinkMapping

class DokkaPlugin implements Plugin<Project> {
    static final String EXTENSION_NAME = 'tutteliDokka'
    static final String TASK_NAME_JAVADOC = 'javadocJar'
    protected static final String ERR_REPO_URL_OR_GITHUB_USER = "${EXTENSION_NAME}.repoUrl or ${EXTENSION_NAME}.githubUser has to be defined"
    protected static final String ERR_GH_PAGES_WITHOUT_USER =
        "You need to define ${EXTENSION_NAME}.githubUser if you want to use ${EXTENSION_NAME}.ghPages"

    @Override
    void apply(Project project) {
        project.pluginManager.apply(JetbrainsDokkaPlugin)

        DokkaTask dokkaTask = project.tasks.getByName('dokka') as DokkaTask
        project.tasks.create(name: TASK_NAME_JAVADOC, type: Jar, dependsOn: dokkaTask) {
            classifier = 'javadoc'
            doFirst {
                from dokkaTask.outputDirectory
            }
        }

        project.buildscript.repositories {
            maven { url 'https://dl.bintray.com/kotlin/dokka' }
        }

        def extension = project.extensions.create(EXTENSION_NAME, DokkaPluginExtension, project)
        project.afterEvaluate {
            LinkMapping mapping = dokkaTask.linkMappings.find { it.url == DokkaPluginExtension.DEFAULT_REPO_URL }
            if (mapping != null) {
                mapping.url = getUrl(project.rootProject, extension)
            }

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

    private static String getUrl(Project rootProject, DokkaPluginExtension extension) {
        if (!extension.repoUrl.isPresent() && !extension.githubUser.isPresent()) throw new IllegalStateException(ERR_REPO_URL_OR_GITHUB_USER)
        if (!extension.repoUrl.isPresent()) {
            extension.repoUrl.set("https://github.com/${extension.githubUser.get()}/$rootProject.name".toString())
        }
        def urlWithPossibleSlash = extension.repoUrl.get()
        def urlWithSlash = urlWithPossibleSlash.endsWith("/") ? urlWithPossibleSlash : urlWithPossibleSlash + "/"
        def gitRef = rootProject.version.endsWith("-SNAPSHOT") ? 'master' : 'v' + rootProject.version
        return "${urlWithSlash}tree/$gitRef"
    }
}

