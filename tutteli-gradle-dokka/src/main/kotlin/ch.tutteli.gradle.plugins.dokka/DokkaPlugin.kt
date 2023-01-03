package ch.tutteli.gradle.plugins.dokka

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.gradle.DokkaPlugin as JetbrainsDokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.gradle.kotlin.dsl.*
import java.net.URL

open class DokkaPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        //TODO use apply from gradle-kotlinx with type parameter
        project.pluginManager.apply(JetbrainsDokkaPlugin::class.java)

        val extension = project.extensions.create<DokkaPluginExtension>(EXTENSION_NAME, project)
        val rootProject = project.rootProject;

        val docsDir = extension.modeSimple.map { useSimpleDocs ->
            if (useSimpleDocs) {
                rootProject.projectDir.resolve("docs/kdoc")
            } else {
                rootProject.projectDir.resolve("../${rootProject.name}-gh-pages/${rootProject.version}/kdoc")
            }
        }

        val dokkaTask = project.tasks.register(TASK_NAME_DOKKA) {
            dependsOn(project.tasks.named("dokkaHtml"))
        }
        project.tasks.register<Jar>(TASK_NAME_JAVADOC) {
            archiveClassifier.set("javadoc")
            dependsOn(dokkaTask)
            doFirst {
                from(docsDir)
            }
        }

        project.tasks.withType<DokkaTask>().configureEach {
            // custom output directory
            outputDirectory.set(docsDir)

            dokkaSourceSets.configureEach {
                val src = "src/${name}/kotlin"
                val srcDir = project.file(src)
                // might be we deal with a multi-platform project where the corresponding target has no sources and hence
                // the directory is missing
                if (srcDir.exists()) {
                    sourceLink {
                        localDirectory.set(project.file(srcDir))
                        remoteUrl.set(getUrl(project.rootProject, extension, src))
                        remoteLineSuffix.set("#L")
                    }
                }

                if (isReleaseVersion(rootProject)) {
                    externalDocumentationLink {
                        url.set(extension.githubUser.flatMap { githubUser ->
                            extension.modeSimple.map { useSimpleDocs ->
                                if (useSimpleDocs) {
                                    URL("https://$githubUser.github.io/${rootProject.name}/kdoc/${rootProject.name}/")
                                } else {
                                    URL("https://$githubUser.github.io/${rootProject.name}/${rootProject.version}/doc/${rootProject.name}/")
                                }
                            }
                        })
                    }
                }
            }
        }

        project.afterEvaluate {
            if (!extension.repoUrl.isPresent) {
                extension.repoUrl.convention(extension.githubUser.map { githubUser ->
                    "https://github.com/${githubUser}/${rootProject.name}"
                })
                if (!extension.githubUser.isPresent) {
                    throw IllegalStateException("$EXTENSION_NAME.githubUser needs to be defined")
                }
            }
        }
    }


    private fun getUrl(rootProject: Project, extension: DokkaPluginExtension, src: String): Provider<URL> {
        return extension.repoUrl.map { urlWithPossibleSlash ->
            val urlWithSlash = if (urlWithPossibleSlash.endsWith("/")) {
                urlWithPossibleSlash
            } else {
                "$urlWithPossibleSlash/"
            }
            val gitRef = if (rootProject.version == "unspecified" || hasSnapshotVersion(rootProject)) {
                "main"
            } else {
                "v${rootProject.version}"
            }
            URL("${urlWithSlash}tree/$gitRef/$src")
        }
    }

    private fun hasSnapshotVersion(rootProject: Project) =
        (rootProject.version as String).endsWith("-SNAPSHOT")

    private fun isReleaseVersion(rootProject: Project): Boolean =
        (rootProject.version as String).matches(Regex("^[0-9]+\\.[0-9]+\\.[0-9]+"))

    companion object {
        const val EXTENSION_NAME = "tutteliDokka"
        const val TASK_NAME_DOKKA = "dokka"
        const val TASK_NAME_JAVADOC = "javadocJar"
    }
}

