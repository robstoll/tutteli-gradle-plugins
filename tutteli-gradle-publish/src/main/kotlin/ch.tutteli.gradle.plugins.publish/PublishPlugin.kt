package ch.tutteli.gradle.plugins.publish

import org.gradle.api.*
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties

class PublishPlugin : Plugin<Project> {

    companion object {
        val LOGGER: Logger = Logging.getLogger(PublishPlugin::class.java)
        const val EXTENSION_NAME = "tutteliPublish"
        private const val PUBLICATION_NAME = "tutteli"
        const val TASK_NAME_INCLUDE_TIME = "includeBuildTimeInManifest"
        const val TASK_NAME_VALIDATE_PUBLISH = "validateBeforePublish"
        val TASK_GENERATE_POM = "generatePomFileFor${PUBLICATION_NAME.capitalize()}Publication"
        val TASK_GENERATE_GRADLE_METADATA = "generateMetadataFileFor${PUBLICATION_NAME.capitalize()}Publication"
    }

    override fun apply(project: Project) {

        project.plugins.apply(MavenPublishPlugin::class.java)
        project.plugins.apply(SigningPlugin::class.java)

        val extension = project.extensions.create<PublishPluginExtension>(EXTENSION_NAME, project)

        val validateBeforePublish = project.tasks.register<ValidateBeforePublishTask>(TASK_NAME_VALIDATE_PUBLISH)

        val includeBuildTime = project.tasks.register(TASK_NAME_INCLUDE_TIME) {
            doLast {
                jarTasks(project, extension).forEach {
                    augmentManifest(it, project, extension)
                }
            }
        }

        includeBuildTime.configure {
            dependsOn(validateBeforePublish)
        }

        project.afterEvaluate {
            jarTasks(project, extension).forEach {
                it.mustRunAfter(includeBuildTime)
            }
            project.version = determineVersion(project) ?: determineVersion(project.rootProject) ?: ""
            project.group = determineGroup(project) ?: determineGroup(project.rootProject) ?: ""
            checkNotNullNorBlank(project.name, "project.name")
            checkNotNullNorBlank(project.version, "project.version or rootProject.version")
            checkNotNullNorBlank(project.group, "project.group or rootProject.group")
            checkNotNullNorBlank(project.description, "project.description")
            checkExtensionPropertyPresentAndNotBlank(extension.githubUser, "githubUser")
            checkExtensionPropertyPresentNotEmpty(extension.licenses, "licenses")
            checkExtensionPropertyPresentAndNotBlank(extension.envNameGpgPassphrase, "envNameGpgPassphrase")
            checkExtensionPropertyPresentAndNotBlank(extension.envNameGpgKeyId, "envNameGpgKeyId")
            checkExtensionPropertyPresentAndNotBlank(extension.envNameGpgKeyRing, "envNameGpgKeyRing")
            checkExtensionPropertyPresentAndNotBlank(extension.envNameGpgSigningKey, "envNameGpgSigningKey")

            val signingExtension = project.extensions.getByType<SigningExtension>()
            var publications = getMavenPublications(project)
            val usesOwnPublications = publications.isNotEmpty()
            if (!usesOwnPublications) {
                // we only create the tutteli publication in case there is not already one (e.g. MPP creates own publications)
                configurePublishing(project, extension)
                publications = getMavenPublications(project)
            }
            publications.forEach { publication ->
                configurePom(publication, project, extension)


                // was a workaround for signing SNAPSHOTs, looks like this is no longer a problem,
                // I keep it here in case it re-appears. Can be removed after some time I guess
//                    def version = String.valueOf(project.version)
//                    // change version for signing only, we change it back afterwards
//                    if (version.endsWith("-SNAPSHOT")) {
//                        project.version = version.substring(0, version.lastIndexOf("-SNAPSHOT"))
//                    }
                signingExtension.sign(publication)
//                    project.version = version

                val taskSuffix = "${publication.name.capitalize()}Publication"
                project.tasks.named("generatePomFileFor$taskSuffix").configure {
                    dependsOn(includeBuildTime)
                }
                val signTask = project.tasks.named("sign$taskSuffix")
                project.tasks.named("publish${taskSuffix}ToMavenLocal").configure {
                    dependsOn(signTask)
                }
            }
            if (usesOwnPublications && project.plugins.findPlugin("org.jetbrains.kotlin.multiplatform") != null) {
                // in case we generate a javadocJar (e.g. via tutteli's dokka plugin) then we add it to each publication
                val javadocJar = project.tasks.findByName("javadocJar")
                if (javadocJar != null) {
                    publications
                        // don't add javadocJar to a "relocation" publication
                        .filterNot { it.name.endsWith("-relocation") }
                        .forEach { publication ->
                            publication.artifact(javadocJar)
                        }
                }
            }
        }
    }


    private fun jarTasks(project: Project, extension: PublishPluginExtension): Set<Jar> =
        project.tasks.withType<Jar>().filter {
            !extension.artifactFilter.isPresent || extension.artifactFilter.get().invoke(it)
        }.toSet()


    private fun getMavenPublications(project: Project): NamedDomainObjectCollection<MavenPublication> =
        project.extensions.getByType<PublishingExtension>().publications.withType<MavenPublication>()

    private fun determineRepoDomainAndPath(project: Project, extension: PublishPluginExtension): String =
        "github.com/${extension.githubUser.get()}/${project.rootProject.name}"

    private fun determineVersion(project: Project): String? =
        if (project.version == "unspecified") null else project.versionAsString

    private val Project.versionAsString: String?
        get() = (project.version as? CharSequence)?.toString()

    private fun determineGroup(project: Project): String? {
        val group = (project.group as? CharSequence)?.toString()
        return if (group.isNullOrBlank()) null else group
    }

    private fun configurePublishing(project: Project, extension: PublishPluginExtension) {
        project.extensions.getByType<PublishingExtension>().publications {
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
                jarTasks(project, extension).forEach {
                    artifact(it)
                }
            }
        }
    }

    private fun configurePom(publication: MavenPublication, project: Project, extension: PublishPluginExtension) {
        val domainAndPath = determineRepoDomainAndPath(project, extension)
        val extensionLicenses = extension.licenses.getOrElse(emptyList())
        val uniqueLicenses = extensionLicenses.toSortedSet()
        if (extensionLicenses.size != uniqueLicenses.size) {
            LOGGER.warn("Some licenses were duplicated. Please check if you made a mistake.")
        }

        publication.pom {
            name.set(project.name)
            description.set(project.description)
            url.set("https://$domainAndPath")

            licenses {
                uniqueLicenses.forEach { chosenLicense ->
                    license {
                        name.set(chosenLicense.longName)
                        url.set(chosenLicense.url)
                        distribution.set(chosenLicense.distribution)
                    }
                }
            }
            developers {
                extension.developers.getOrElse(emptyList()).forEach { dev ->
                    developer {
                        id.set(dev.id)
                        if (dev.name.isNullOrBlank().not()) name.set(dev.name)
                        if (dev.email.isNullOrBlank().not()) email.set(dev.email)
                        if (dev.url.isNullOrBlank().not()) url.set(dev.url)
                        if (dev.organization.isNullOrBlank().not()) organization.set(dev.organization)
                        if (dev.organizationUrl.isNullOrBlank().not()) organizationUrl.set(dev.organizationUrl)
                        //never used roles, timezone so far, skip it for now
                    }
                }
            }
            scm {
                connection.set("scm:git:git://${domainAndPath}.git")
                developerConnection.set("scm:git:ssh://${domainAndPath}.git")
                url.set("https://$domainAndPath")
            }
        }
    }


    private fun augmentManifest(task: Jar, project: Project, extension: PublishPluginExtension) {
        val repoUrl = determineRepoDomainAndPath(project, extension)
        task.manifest {
            attributes(
                mapOf(
                    "Implementation-Title" to project.name,

                    "Implementation-Version" to project.version,
                    "Implementation-URL" to "https://$repoUrl",
                    "Build-Time" to ZonedDateTime.now().format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
                )
                    + getVendorIfAvailable(extension) + getImplementationKotlinVersionIfAvailable(project)
            )
            listOf("LICENSE.txt", "LICENSE", "LICENSE.md", "LICENSE.rst").forEach { fileName ->
                val licenseFile = project.file("${project.rootProject.projectDir}/$fileName")
                if (licenseFile.exists()) task.from(licenseFile)
            }
        }
    }

    private fun getVendorIfAvailable(extension: PublishPluginExtension): Map<String, String> =
        if (extension.manifestVendor.isPresent) mapOf("Implementation-Vendor" to extension.manifestVendor.get())
        else emptyMap()

    private fun getImplementationKotlinVersionIfAvailable(project: Project): Map<String, String> {
        val kotlinVersion = getKotlinVersion(project)
        // we use if instead of ternary operator because type inference fails otherwise
        return if (kotlinVersion != null) mapOf("Implementation-Kotlin-Version" to kotlinVersion)
        else emptyMap()
    }

    private fun getKotlinVersion(project: Project): String? {
        val version = try {
            project.getKotlinPluginVersion()
        } catch (e: NoClassDefFoundError) {
            // KotlinPluginWrapperKt (source where extension method getKotlinPluginVersion is defined) might not exist
            // if no kotlin plugin was applied or an old one or an old gradle version is used where the extension
            // method on Project does not exist yet
            null
        }
        return version
            ?: (project.plugins.let<PluginContainer, Plugin<*>?> {
                // TODO drop once we no longer support the old kotlin plugins and old gradle version
                it.findPlugin("kotlin")
                    ?: it.findPlugin("kotlin2js")
                    ?: it.findPlugin("kotlin-platform-jvm")
                    ?: it.findPlugin("kotlin-platform-js")
                    ?: it.findPlugin("kotlin-common")
                    ?: it.findPlugin("org.jetbrains.kotlin.multiplatform")
                    ?: it.findPlugin("org.jetbrains.kotlin.jvm")
                    ?: it.findPlugin("org.jetbrains.kotlin.js")
            }?.let {
                val value = it::class.memberProperties.find { property ->
                    property.name == "kotlinPluginVersion"
                }?.call(it)
                (value as? CharSequence)?.toString()
            })
    }
}
