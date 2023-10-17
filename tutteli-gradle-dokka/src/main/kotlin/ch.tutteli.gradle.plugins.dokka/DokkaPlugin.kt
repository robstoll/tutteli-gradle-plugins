package ch.tutteli.gradle.plugins.dokka

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import java.io.File
import java.net.URL
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import org.jetbrains.dokka.gradle.DokkaPlugin as JetbrainsDokkaPlugin

open class DokkaPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        //TODO use apply from gradle-kotlinx with type parameter
        project.pluginManager.apply(JetbrainsDokkaPlugin::class.java)

        val extension = project.extensions.create<DokkaPluginExtension>(EXTENSION_NAME, project)
        val rootProject = project.rootProject;

        val docDirName = "kdoc"
        val docsDir = if (project == rootProject) {
            extension.modeSimple.flatMap { modeSimple ->
                extension.writeToDocs.map { writeToDocs ->
                    if (modeSimple && writeToDocs) {
                        rootProject.layout.projectDirectory.dir("docs/$docDirName")
                    } else {
                        rootProject.layout.projectDirectory.dir("../${rootProject.name}-gh-pages/${rootProject.version}/$docDirName")
                    }
                }
            }
        } else {
            null
        }

        if (docsDir != null) {
            project.tasks.withType<AbstractDokkaTask>().configureEach {
                try {
                    outputDirectory.set(docsDir)
                } catch (e: NoSuchMethodError) {
                    // maybe a dokka 1.8.10 user where dokka used a Property<File> instead of DirectoryProperty
                    @Suppress("UNCHECKED_CAST")
                    val outputDir = (this::class.memberProperties
                        .first { it.name == "outputDirectory" }
                        as KProperty1<AbstractDokkaTask, Property<File>>).get(this)

                    outputDir.set(docsDir.get().asFile)
                }
            }
        }

        // we want to configure DokkaTask as well as DokkaPartialTask
        project.tasks.withType<AbstractDokkaLeafTask>().configureEach {

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
                            rootProject.the<DokkaPluginExtension>().modeSimple.flatMap { usesSimpleDocs ->
                                extension.writeToDocs.map { writeToDocs ->
                                    if (usesSimpleDocs && writeToDocs) {
                                        URL("https://$githubUser.github.io/${rootProject.name}/$docDirName/${rootProject.name}/")
                                    } else {
                                        URL("https://$githubUser.github.io/${rootProject.name}/${rootProject.version}/$docDirName/")
                                    }
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

