package ch.tutteli.gradle.plugins.publish

import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication


class PomAugmenter(
    private val project: Project,
    private val extension: PublishPluginExtension
) {

    fun augment(publication: MavenPublication) {
        val domainAndPath = extension.determineRepoDomainAndPath()
        val extensionLicenses = extension.licenses.getOrElse(emptyList())
        val uniqueLicenses = extensionLicenses.toSortedSet()
        if (extensionLicenses.size != uniqueLicenses.size) {
            PublishPlugin.LOGGER.warn("Some licenses were duplicated. Please check if you made a mistake.")
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
}
