package ch.tutteli.gradle.plugins.publish

import org.gradle.api.*
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin

class PublishPlugin : Plugin<Project> {

    companion object {
        val LOGGER: Logger = Logging.getLogger(PublishPlugin::class.java)
        const val EXTENSION_NAME = "tutteliPublish"
        private const val PUBLICATION_NAME = "tutteli"
        const val TASK_NAME_PREFIX_AUGMENT_MANIFEST_IN_JAR = "augmentManifestIn-"
        const val TASK_NAME_VALIDATE_PUBLISH = "validateBeforePublish"
        val TASK_GENERATE_POM = "generatePomFileFor${PUBLICATION_NAME.capitalize()}Publication"
        val TASK_GENERATE_GRADLE_METADATA = "generateMetadataFileFor${PUBLICATION_NAME.capitalize()}Publication"
    }

    override fun apply(project: Project) {

        project.plugins.apply(MavenPublishPlugin::class.java)
        project.plugins.apply(SigningPlugin::class.java)

        val extension = project.extensions.create<PublishPluginExtension>(EXTENSION_NAME, project)

        project.afterEvaluate {
            project.version = determineVersion(project) ?: determineVersion(project.rootProject) ?: ""
            project.group = determineGroup(project) ?: determineGroup(project.rootProject) ?: ""
            checkNotNullNorBlank(project.name, "project.name")
            checkNotNullNorBlank(project.version, "project.version or rootProject.version")
            checkNotNullNorBlank(project.group, "project.group or rootProject.group")
            checkNotNullNorBlank(project.description, "project.description")
            checkExtensionPropertyPresentAndNotBlank(extension.githubUser, "githubUser")
            checkExtensionPropertyPresentNotEmpty(extension.licenses, "licenses")

            jarTasks(project, extension) {
                val jarTask = this
                val augmentTaskName = TASK_NAME_PREFIX_AUGMENT_MANIFEST_IN_JAR + jarTask.name
                val augmentTask = project.tasks.register<AugmentManifestInJarTask>(augmentTaskName) {
                    this.jarTask.set(jarTask)
                }
                jarTask.dependsOn(augmentTask)
            }

            var publications = getMavenPublications(project)
            val usesOwnPublications = publications.isNotEmpty()
            if (!usesOwnPublications) {
                // we only create the tutteli publication in case there is not already one
                // (e.g. MPP creates own publications)
                registerTutteliPublication(project, extension)
                publications = getMavenPublications(project)
            }

            val signingExtension = project.the<SigningExtension>()
            val validateBeforePublish = project.tasks.register<ValidateBeforePublishTask>(TASK_NAME_VALIDATE_PUBLISH)

            val pomAugmenter = PomAugmenter(project, extension)
            publications.configureEach {
                val publication = this
                pomAugmenter.augment(this)

                val taskSuffix = "${publication.name.capitalize()}Publication"

                // creates the sign task -- tasks if we would pass more than one publication
                signingExtension.sign(publication).forEach { signTask ->
                    signTask.dependsOn(validateBeforePublish)

                    project.tasks.named("publish${taskSuffix}ToMavenLocal").configure {
                        dependsOn(signTask)
                    }
                }

                // in case we generate a javadocJar (e.g. via tutteli's dokka plugin) then we add it to each publication
                // but only if it is not a ...-relocation publication
                // and only if the project has defined own publications and the new MPP plugin is available. Because
                // we fear that other plugins might add javadoc as magically as we do it here and we only want to
                // do it if MPP is available (doesn't mean there could not be another plugin which still does magic stuff)
                if (!publication.name.endsWith("-relocation") &&
                    usesOwnPublications &&
                    project.plugins.findPlugin("org.jetbrains.kotlin.multiplatform") != null
                ) {
                    project.tasks.findByName("javadocJar")?.let { javadocJar ->
                        publication.artifact(javadocJar)
                    }
                }
            }
        }
    }

    private fun jarTasks(project: Project, extension: PublishPluginExtension, action: Action<Jar>): Unit =
        project.tasks.withType<Jar>()
            .matching { !extension.artifactFilter.isPresent || extension.artifactFilter.get().invoke(it) }
            //TODO 5.0.0 check if we can use configureEach with gradle 8.x
            .all(action)

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
                jarTasks(project, extension) {
                    artifact(this)
                }
            }
        }
    }
}
