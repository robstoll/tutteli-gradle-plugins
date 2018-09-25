package ch.tutteli.gradle.bintray

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin

import static ch.tutteli.gradle.bintray.Validation.requireNotNullNorEmpty
import static ch.tutteli.gradle.bintray.Validation.requirePresentAndNotEmpty

class BintrayPlugin implements Plugin<Project> {
    private static final Logger LOGGER = Logging.getLogger(BintrayPlugin.class)
    static final String EXTENSION_NAME = 'tutteliBintray'

    @Override
    void apply(Project project) {
        project.plugins.apply(MavenPublishPlugin)
        def extension = project.extensions.create(EXTENSION_NAME, BintrayPluginExtension, project)

        project.afterEvaluate {
            requireNotNullNorEmpty(project.name, "project.name")
            requireNotNullNorEmpty(project.version == "unspecified" ? null : project.version, "project.version")
            requireNotNullNorEmpty(project.group, "project.group")
            requireNotNullNorEmpty(project.description, "project.description")
            requirePresentAndNotEmpty(extension.githubUser, "${EXTENSION_NAME}.githubUser")

            configurePublishing(project, extension)
        }
    }

    private static void configurePublishing(Project project, BintrayPluginExtension extension) {
        project.publishing {
            publications {
                tutteli(MavenPublication) {
                    MavenPublication publication = it
                    if (extension.component.isPresent()) {
                        publication.from(extension.component.get())
                    }
                    def artifacts = extension.artifacts.getOrElse(Collections.emptyList())
                    artifacts.each {
                        publication.artifact it
                    }

                    groupId project.group
                    artifactId project.name
                    version project.version

                    pom.withXml(pomConfig(project, extension))
                }
            }
        }
    }

    private static Action<? extends XmlProvider> pomConfig(Project project, BintrayPluginExtension extension) {
        def repoUrl = "https://github.com/${extension.githubUser.get()}/$project.name"
        def pomConfig = {
            url repoUrl
            licenses {
                if (extension.licenses.isPresent()) {
                    def licenses = extension.licenses.get()
                    def uniqueLicenses = licenses.toSet().toSorted()
                    if (licenses.size() != uniqueLicenses.size()) {
                        LOGGER.warn("Some licenses were duplicated. Please check if you made a mistake.")
                    }
                    uniqueLicenses.each { chosenLicense ->
                        requireNotNullNorEmpty(chosenLicense.longName, "license.longName")
                        license {
                            name chosenLicense.longName
                            if (chosenLicense.url) url chosenLicense.url
                            if (chosenLicense.distribution) distribution chosenLicense.distribution
                        }
                    }
                }
            }
            developers {
                if (extension.developers.isPresent()) {
                    extension.developers.get().each { dev ->
                        developer {
                            id dev.id
                            if(dev.name) name dev.name
                            if(dev.email) email dev.email
                            if(dev.url) url dev.url
                            if(dev.organization) organization dev.organization
                            if(dev.organizationUrl) organizationUrl dev.organizationUrl
                            //never used roles, timezone so far, skip it for now
                        }
                    }
                }
            }
            scm {
                url repoUrl
            }
        }
        return new Action<? extends XmlProvider>() {
            @Override
            void execute(XmlProvider p) {
                def root = p.asNode()
                root.appendNode('description', project.description)
                root.children().last() + pomConfig
            }
        }
    }
}

