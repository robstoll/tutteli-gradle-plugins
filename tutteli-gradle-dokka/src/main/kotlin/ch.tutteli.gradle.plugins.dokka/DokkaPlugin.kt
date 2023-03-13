package ch.tutteli.gradle.plugins.dokka

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.gradle.DokkaPlugin as JetbrainsDokkaPlugin
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import org.jetbrains.dokka.gradle.AbstractDokkaParentTask
import org.jetbrains.dokka.gradle.DokkaTask
import java.io.File
import java.net.URL

open class DokkaPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        //TODO use apply from gradle-kotlinx with type parameter
        project.pluginManager.apply(JetbrainsDokkaPlugin::class.java)

        val extension = project.extensions.create<DokkaPluginExtension>(EXTENSION_NAME, project)
        val rootProject = project.rootProject;

        val docsDir = if (project == rootProject) {
            extension.modeSimple.map { usesSimpleDocs ->
                if (usesSimpleDocs) {
                    rootProject.projectDir.resolve("docs/kdoc")
                } else {
                    rootProject.projectDir.resolve("../${rootProject.name}-gh-pages/${rootProject.version}/kdoc")
                }
            }
        } else {
            null
        }

        project.tasks.register<Jar>(TASK_NAME_JAVADOC) {
            archiveClassifier.set("javadoc")
            val dokkaHtml = project.tasks.named<DokkaTask>("dokkaHtml")
            dependsOn(dokkaHtml)
            doFirst {
                from(dokkaHtml.map { it.outputDirectory })
            }
        }

        if (docsDir != null) {
            project.tasks.withType<AbstractDokkaParentTask>().configureEach {
                outputDirectory.set(docsDir)
            }
        }

        // we want to configure DokkaTask as well as DokkaPartialTask
        project.tasks.withType<AbstractDokkaLeafTask>().configureEach {
            // custom output directory
            if (docsDir != null) {
                outputDirectory.set(docsDir)
            }

            dokkaSourceSets.configureEach {
                val sourceSet = this

                val src = "src/${name}/kotlin"
                val srcDir = project.file(src)
                // might be we deal with a multi-platform project where the corresponding target has no sources and
                // hence the directory is missing
                if (srcDir.exists()) {
                    sourceLink {
                        localDirectory.set(srcDir)
                        remoteUrl.set(getUrl(project.rootProject, extension, srcDir))
                        remoteLineSuffix.set("#L")
                    }
                }

                if (isReleaseVersion(rootProject)) {
                    externalDocumentationLink {
                        url.set(extension.githubUser.flatMap { githubUser ->
                            extension.modeSimple.map { usesSimpleDocs ->
                                if (usesSimpleDocs) {
                                    URL("https://$githubUser.github.io/${rootProject.name}/kdoc/${rootProject.name}/")
                                } else {
                                    URL("https://$githubUser.github.io/${rootProject.name}/${rootProject.version}/doc/${rootProject.name}/")
                                }
                            }
                        })
                    }
                }

                if (sourceSet.name.endsWith("Main")) {
                    val testFolder =
                        project.projectDir.resolve("src/${sourceSet.name.substringBeforeLast("Main")}Test/kotlin")

                    if (testFolder.exists()) {
                        val files = project.fileTree(testFolder) {
                            include("**/*Samples.kt")
                        }
                        samples.from(files)
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


    private fun getUrl(rootProject: Project, extension: DokkaPluginExtension, srcDir: File): Provider<URL> {
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
            URL(
                "${urlWithSlash}tree/$gitRef/${
                    srcDir.relativeTo(rootProject.projectDir).toString().replace('\\', '/')
                }"
            )
        }
    }

    private fun hasSnapshotVersion(rootProject: Project) =
        (rootProject.version as? CharSequence)?.endsWith("-SNAPSHOT") ?: false

    private fun isReleaseVersion(rootProject: Project): Boolean =
        (rootProject.version as? CharSequence)?.matches(Regex("^[0-9]+\\.[0-9]+\\.[0-9]+"))
            ?: throw IllegalStateException("please define your version as CharSequence (e.g. String), was ${rootProject.version::class} (${rootProject.version}")

    companion object {
        const val EXTENSION_NAME = "tutteliDokka"
        const val TASK_NAME_JAVADOC = "javadocJar"
    }
}

