package ch.tutteli.gradle.publish

import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.BintrayPlugin
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.jvm.tasks.Jar

import static Validation.*

class PublishPlugin implements Plugin<Project> {
    private static final Logger LOGGER = Logging.getLogger(PublishPlugin.class)
    static final String EXTENSION_NAME = 'publish'
    static final String SOURCES_JAR = 'sourcesJar'

    @Override
    void apply(Project project) {
        project.plugins.apply(MavenPublishPlugin)
        project.plugins.apply(BintrayPlugin)

        project.tasks.create(name: SOURCES_JAR, type: Jar) {
            from project.sourceSets.main.allSource
            classifier = 'sources'
        }

        def extension = project.extensions.create(EXTENSION_NAME, PublishPluginExtension, project)

        project.afterEvaluate {
            requireNotNullNorBlank(project.name, "project.name")
            requireNotNullNorBlank(project.version == "unspecified" ? null : project.version, "project.version")
            requireNotNullNorBlank(project.group, "project.group")
            requireNotNullNorBlank(project.description, "project.description")
            requireComponentOrArtifactsPresent(extension)
            requireExtensionPropertyPresentAndNotBlank(extension.githubUser, "githubUser")
            requireExtensionPropertyPresentNotEmpty(extension.licenses, "licenses")

            def bintrayExtension = project.extensions.getByType(BintrayExtension)
            requireExtensionPropertyPresentAndNotBlank(extension.envNameBintrayUser, "envNameBintrayUser")
            requireExtensionPropertyPresentAndNotBlank(extension.envNameBintrayApiKey, "envNameBintrayApiKey")
            requireExtensionPropertyPresentAndNotBlank(extension.envNameBintrayGpgPassphrase, "envNameBintrayGpgPassphrase")
            requireSetOnBintrayExtensionOrProperty(bintrayExtension.pkg.repo, extension.bintrayRepo, "bintrayRepo")
            requireSetOnBintrayExtensionOrProperty(bintrayExtension.pkg.name, extension.bintrayPkg, "bintrayPkg")

            def repoUrl = "https://github.com/${extension.githubUser.get()}/$project.name"
            def licenses = extension.licenses.get()
            def uniqueLicenses = licenses.toSet().toSorted()
            if (licenses.size() != uniqueLicenses.size()) {
                LOGGER.warn("Some licenses were duplicated. Please check if you made a mistake.")
            }

            configurePublishing(project, extension, repoUrl, uniqueLicenses)
            configureBintray(project, extension, bintrayExtension, repoUrl, uniqueLicenses)
        }
    }

    private static void requireComponentOrArtifactsPresent(PublishPluginExtension extension) {
        if (!extension.component.isPresent() && extension.artifacts.map { it.isEmpty() }.getOrElse(true)) {
            throw newIllegalState("either ${EXTENSION_NAME}.component or ${EXTENSION_NAME}.artifacts")
        }
    }

    private static void configurePublishing(
        Project project,
        PublishPluginExtension extension,
        String repoUrl,
        List<License> uniqueLicenses
    ) {

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

                    pom.withXml(pomConfig(project, extension, repoUrl, uniqueLicenses))
                }
            }
        }
    }

    private static Action<? extends XmlProvider> pomConfig(
        Project project,
        PublishPluginExtension extension,
        String repoUrl,
        List<License> uniqueLicenses
    ) {
        def pomConfig = {
            url repoUrl
            licenses {
                if (extension.licenses.isPresent()) {
                    uniqueLicenses.each { chosenLicense ->
                        requireNotNullNorBlank(chosenLicense.longName, "license.longName")
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
                            if (dev.name) name dev.name
                            if (dev.email) email dev.email
                            if (dev.url) url dev.url
                            if (dev.organization) organization dev.organization
                            if (dev.organizationUrl) organizationUrl dev.organizationUrl
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

    private static void configureBintray(
        Project project,
        PublishPluginExtension extension,
        BintrayExtension bintrayExtension,
        String repoUrl,
        List<License> uniqueLicenses
    ) {
        bintrayExtension.with {
            user = user ?: getSystemEnvAndErrorIfBlank(extension.envNameBintrayUser.get())
            key = key ?: getSystemEnvAndErrorIfBlank(extension.envNameBintrayApiKey.get())
            publications = ['tutteli'] as String[]

            pkg.with {
                repo = repo ?: extension.bintrayRepo.get()
                def pkgName = name ?: extension.bintrayPkg.getOrElse(project.name)
                name = pkgName
                licenses = licenses ?: uniqueLicenses.collect { it.shortName } as String[]
                vcsUrl = vcsUrl ?: repoUrl
                version.with {
                    name = name ?: project.name
                    desc = desc ?: "$pkgName $project.version"
                    released = released ?: new Date().toTimestamp().toString()
                    vcsTag = vcsTag ?: "v$project.version"
                    gpg.with {
                        def signIt = sign ?: extension.signWithGpg.getOrElse(true)
                        sign = signIt
                        if (signIt) {
                            passphrase = passphrase ?: getSystemEnvAndErrorIfBlank(extension.envNameBintrayGpgPassphrase.get())
                        }
                    }
                }
            }
        }
    }

    private static String getSystemEnvAndErrorIfBlank(String envName) {
        def value = System.getenv(envName)
        if (!value?.trim()) throw newIllegalState("System.env variable with name $envName")
        return value
    }
}

