package ch.tutteli.gradle.plugins.publish

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import java.util.*

class PublishPlugin : Plugin<Project> {

    companion object {
        val LOGGER: Logger = Logging.getLogger(PublishPlugin::class.java)
        const val EXTENSION_NAME = "tutteliPublish"
        private const val PUBLICATION_NAME = "tutteli"
        const val TASK_NAME_VALIDATE_PUBLISH = "validateBeforePublish"
        const val TASK_GENERATE_POM = "generatePomFileForTutteliPublication"
        const val TASK_GENERATE_GRADLE_METADATA = "generateMetadataFileForTutteliPublication"
    }

    override fun apply(project: Project) {

        project.plugins.apply(MavenPublishPlugin::class.java)
        project.plugins.apply(SigningPlugin::class.java)

        val extension = project.extensions.create<PublishPluginExtension>(EXTENSION_NAME, project)
        val manifestAugmenter = ManifestAugmenter(project, extension)
        val pomAugmenter = PomAugmenter(project, extension)

        project.tasks.withType<AbstractArchiveTask>().configureEach {
            // Ensure builds are reproducible, see https://docs.gradle.org/current/userguide/working_with_files.html#sec:reproducible_archives
            // as well as https://github.com/gradle/gradle/issues/10900
            isPreserveFileTimestamps = false
            isReproducibleFileOrder = true
            dirMode = "775".toInt(8)
            fileMode = "664".toInt(8)
        }

        project.tasks.withType<Jar>()
            .matching { jar -> extension.artifactFilter.map { it(jar) }.getOrElse(true) }
            .configureEach {
                val jar = this
                doFirst {
                    manifestAugmenter.augment(jar)
                }
            }

        project.afterEvaluate {
            project.version = determineVersion(project) ?: determineVersion(project.rootProject) ?: ""
            project.group = determineGroup(project) ?: determineGroup(project.rootProject) ?: ""
            checkNotNullNorBlank(project.name, "project.name")
            checkNotNullNorBlank(project.version, "project.version or rootProject.version")
            checkNotNullNorBlank(project.group, "project.group or rootProject.group")
            checkNotNullNorBlank(project.description, "project.description")
            checkExtensionPropertyPresentAndNotBlank(extension.githubUser, "githubUser")
            checkExtensionPropertyPresentNotEmpty(extension.licenses, "licenses")

            val validateBeforePublish = project.tasks.register<ValidateBeforePublishTask>(TASK_NAME_VALIDATE_PUBLISH)
            val signingExtension = project.the<SigningExtension>()

            val publications = getMavenPublications(project)

            val needToCreateOwnPublication = publications.isEmpty()

            if (needToCreateOwnPublication) {
                // we only create the tutteli publication in case there is not already one
                // (e.g. MPP creates own publications)
                registerTutteliPublication(project, extension)
            }

            addJavadocJarBasedOnDokkaIfPresent(project, publications, needToCreateOwnPublication)

            publications.configureEach {
                val publication = this
                pomAugmenter.augment(publication)

                // creates the sign task -- tasks if we would pass more than one publication
                signingExtension.sign(publication).forEach { signTask ->
                    signTask.dependsOn(validateBeforePublish)
                }
            }
        }
    }

    private fun addJavadocJarBasedOnDokkaIfPresent(
        project: Project,
        publications: NamedDomainObjectCollection<MavenPublication>,
        needToCreateOwnPublication: Boolean
    ) {
        val isKotlinMultiPlatform = project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")

        project.plugins.findPlugin("org.jetbrains.dokka")?.run {
            publications.forEach { publication ->

                // we only add a javadoc-jar if we don't deal with a relocation publication
                if (!publication.name.endsWith("-relocation")) {
                    val javadocJar = if (isKotlinMultiPlatform) {
                        val publicationNameCapitalized = publication.name.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        }
                        val customDokkaHtml =
                            project.tasks.register<DokkaTask>("dokkaHtml${publicationNameCapitalized}") {
                                outputDirectory.set(project.layout.buildDirectory.map { it.dir("dokka/${publication.name}") })
                                dokkaSourceSets.matching {
                                    // we only want to include the corresponding platform + common
                                    it.name.startsWith(publication.name).not()
                                        && it.name.startsWith("common").not()
                                }.configureEach {
                                    suppress.set(true)
                                }
                            }

                        project.tasks.register<Jar>("javadocJar$publicationNameCapitalized") {
                            archiveClassifier.set("javadoc")
                            // otherwise it looks like kotlin overwrites the javadoc.jar it creates within the
                            // build directory as it only append the js/jvm archiveAppendix during publication
                            archiveAppendix.set(publication.name)
                            dependsOn(customDokkaHtml)
                            doFirst {
                                from(customDokkaHtml)
                            }
                        }
                    } else {
                        project.tasks.register<Jar>("javadocJar") {
                            archiveClassifier.set("javadoc")
                            val dokkaHtml = project.tasks.named<DokkaTask>("dokkaHtml")
                            dependsOn(dokkaHtml)
                            doFirst {
                                from(dokkaHtml)
                            }
                        }
                    }
                    if (needToCreateOwnPublication.not()) {
                        // if we create an own publication, then we add automatically all jars as artifact and
                        // we don't need to add it here again (otherwise we add the same artifact twice)
                        publication.artifact(javadocJar)
                    }
                }
            }
        }
    }

    private fun getMavenPublications(project: Project): NamedDomainObjectCollection<MavenPublication> =
        project.the<PublishingExtension>().publications.withType<MavenPublication>()

    private fun determineVersion(project: Project): String? =
        if (project.version == "unspecified") null else project.versionAsString

    private val Project.versionAsString: String?
        get() = (project.version as? CharSequence)?.toString()

    private fun determineGroup(project: Project): String? {
        val group = (project.group as? CharSequence)?.toString()
        return if (group.isNullOrBlank()) null else group
    }

    private fun registerTutteliPublication(project: Project, extension: PublishPluginExtension) {
        project.the<PublishingExtension>().publications {
            register<MavenPublication>(PUBLICATION_NAME) {
                groupId = (project.group as? CharSequence)?.toString()
                    ?: throw IllegalStateException("project.group needs to be a string as it is used as groupId for the maven publication coordinates")
                artifactId = project.name
                version = project.versionAsString

                if (extension.component.isPresent) {
                    val component = extension.component.get()
                    from(component)
                    artifacts.forEach {
                        if (it.file.name.endsWith("jar")) {
                            // we remove all jars added by the component as we are going to re-add all jars
                            // further below
                            artifacts.remove(it)
                        }
                    }
                }
                project.tasks.withType<Jar>()
                    .matching { jar ->
                        extension.artifactFilter.map { it(jar) }.getOrElse(true)
                    }
                    // we need to use all here as configureEach will not add jar and kotlinSourcesJar to the publication
                    // no idea why though (maybe because we are within an project.afterEvaluate?
                    .all {
                        artifact(this)
                    }
            }
        }
    }
}
