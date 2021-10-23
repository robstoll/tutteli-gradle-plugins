package ch.tutteli.gradle.plugins.publish

import org.gradle.api.*
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.jvm.tasks.Jar
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin

import static ch.tutteli.gradle.plugins.publish.Validation.*

class PublishPlugin implements Plugin<Project> {
    private static final Logger LOGGER = Logging.getLogger(PublishPlugin.class)
    static final String EXTENSION_NAME = 'tutteliPublish'
    static final String PUBLICATION_NAME = 'tutteli'
    static final String TASK_NAME_INCLUDE_TIME = 'includeBuildTimeInManifest'
    static final String TASK_NAME_VALIDATE_PUBLISH = 'validateBeforePublish'
    static final String TASK_GENERATE_POM = "generatePomFileFor${PUBLICATION_NAME.capitalize()}Publication"
    static final String TASK_GENERATE_GRADLE_METADATA = "generateMetadataFileFor${PUBLICATION_NAME.capitalize()}Publication"

    @Override
    void apply(Project project) {
        project.plugins.apply(MavenPublishPlugin)
        project.plugins.apply(SigningPlugin)

        if (!project.hasProperty('sourceSets')) throw new IllegalStateException(
            "The project $project.name does not have any sources. We currently require a project to have sources in order to publish it." +
                "\nPlease make sure you do not apply the ch.tutteli.$EXTENSION_NAME plugin before the plugin which provides the sourceSets (e.g. kotlin or java and the like)" +
                "\nPlease open an issue if you would like to publish projects without sources: https://github.com/robstoll/tutteli-gradle-plugins/issues/new"
        )

        def extension = project.extensions.create(EXTENSION_NAME, PublishPluginExtension, project)

        def validateBeforePublish = project.tasks.create(name: TASK_NAME_VALIDATE_PUBLISH, type: ValidateBeforePublishTask)

        def includeBuildTime = project.tasks.create(name: TASK_NAME_INCLUDE_TIME) {
            doLast {
                jarTasks(project, extension).each {
                    augmentManifest(it, project, extension)
                }
            }
        }

        includeBuildTime.dependsOn validateBeforePublish

        project.afterEvaluate {
            jarTasks(project, extension).each {
                it.mustRunAfter(includeBuildTime)
            }
            project.version = determineVersion(project) ?: determineVersion(project.rootProject) ?: ""
            project.group = project.group ?: project.rootProject.group
            requireNotNullNorBlank(project.name, "project.name")
            requireNotNullNorBlank(project.version, "project.version or rootProject.version")
            requireNotNullNorBlank(project.group, "project.group or rootProject.group")
            requireNotNullNorBlank(project.description, "project.description")
            requireExtensionPropertyPresentAndNotBlank(extension.githubUser, "githubUser")
            requireExtensionPropertyPresentNotEmpty(extension.licenses, "licenses")
            requireExtensionPropertyPresentAndNotBlank(extension.envNameGpgPassphrase, "envNameGpgPassphrase")
            requireExtensionPropertyPresentAndNotBlank(extension.envNameGpgKeyId, "envNameGpgKeyId")
            requireExtensionPropertyPresentAndNotBlank(extension.envNameGpgKeyRing, "envNameGpgSecretKeyRingFile")
            requireExtensionPropertyPresentAndNotBlank(extension.envNameGpgSigningKey, "envNameGpgSigningKey")

            def signingExtension = project.extensions.getByType(SigningExtension)
            def publications = getMavenPublications(project)
            def usesOwnPublications = !publications.isEmpty()
            if (!usesOwnPublications) {
                // we only create the tutteli publication in case there is not already one (e.g. MPP creates own publications)
                configurePublishing(project, extension)
                publications = getMavenPublications(project)
            }
            publications
                .forEach { publication ->
                    publication.pom.withXml(pomConfig(project, extension))

                    // was a workaround for signing SNAPSHOTs, looks like this is no longer a problem,
                    // I keep it here in case it re-appears. Can be removed after some time I guess
//                    def version = String.valueOf(project.version)
//                    // change version for signing only, we change it back afterwards
//                    if (version.endsWith("-SNAPSHOT")) {
//                        project.version = version.substring(0, version.lastIndexOf("-SNAPSHOT"))
//                    }
                    signingExtension.sign(publication)
//                    project.version = version

                    def taskSuffix = "${publication.name.capitalize()}Publication"
                    def generatePom = project.tasks.getByName("generatePomFileFor$taskSuffix")
                    generatePom.dependsOn(includeBuildTime)
                    def signTask = project.tasks.getByName("sign$taskSuffix")
                    def pubToMaLo = project.tasks.getByName("publish${taskSuffix}ToMavenLocal")
                    pubToMaLo.dependsOn(signTask)
                }
            if (usesOwnPublications && project.plugins.findPlugin('org.jetbrains.kotlin.multiplatform') != null) {
                // in case we generate a javadocJar (e.g. via tutteli's dokka plugin) then we add it to each publication
                def javadocJar = project.tasks.findByName("javadocJar")
                if (javadocJar != null) {
                    publications
                        .forEach { publication ->
                            publication.artifact(javadocJar)
                        }
                }
            }
        }

    }

    private static List<MavenPublication> getMavenPublications(Project project) {
        project.extensions.getByType(PublishingExtension).publications
            .findAll { it instanceof MavenPublication }
            .collect { it as MavenPublication }
    }

    private static Set<Jar> jarTasks(Project project, PublishPluginExtension extension) {
        return project.tasks.withType(Jar).findAll {
            !extension.artifactFilter.isPresent() || extension.artifactFilter.get()(it)
        }
    }

    private static String determineRepoDomainAndPath(Project project, PublishPluginExtension extension) {
        return "github.com/${extension.githubUser.get()}/$project.rootProject.name"
    }

    private static String determineVersion(Project project) {
        return project.version == "unspecified" ? null : project.version
    }

    private void configurePublishing(Project project, PublishPluginExtension extension) {
        project.publishing {
            publications {
                tutteli(MavenPublication) { MavenPublication publication ->
                    groupId project.group
                    artifactId project.name
                    version project.version

                    if (extension.component.isPresent()) {
                        def component = extension.component.get()
                        publication.from(component)
                        publication.artifacts.forEach {
                            if (it.file.name.endsWith("jar")) {
                                // we remove all jars added by the component as we are going to re-add all jars
                                // further below
                                publication.artifacts.remove(it)
                            }
                        }
                    }
                    jarTasks(project, extension).each {
                        publication.artifact it
                    }
                }
            }
        }
    }

    private static Action<? extends XmlProvider> pomConfig(Project project, PublishPluginExtension extension) {
        String domainAndPath = determineRepoDomainAndPath(project, extension)
        def extensionLicenses = extension.licenses.getOrElse(Collections.emptyList())
        def uniqueLicenses = extensionLicenses.toSet().toSorted()
        if (extensionLicenses.size() != uniqueLicenses.size()) {
            LOGGER.warn("Some licenses were duplicated. Please check if you made a mistake.")
        }
        def pomConfig = {
            description project.description
            url "https://" + domainAndPath
            licenses {
                uniqueLicenses.each { chosenLicense ->
                    requireNotNullNorBlank(chosenLicense.longName, "license.longName")
                    license {
                        name chosenLicense.longName
                        if (chosenLicense.url) url chosenLicense.url
                        if (chosenLicense.distribution) distribution chosenLicense.distribution
                    }
                }
            }
            developers {
                extension.developers.getOrElse(Collections.emptyList()).each { dev ->
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
            scm {
                connection "scm:git:git://${domainAndPath}.git"
                developerConnection "scm:git:ssh://${domainAndPath}.git"
                url "https://$domainAndPath"
            }
        }
        return new Action<? extends XmlProvider>() {
            @Override
            void execute(XmlProvider p) {
                def root = p.asNode()
                root.appendNode('name', project.name)
                root.children().last() + pomConfig
            }
        }
    }

    static String getPropertyOrSystemEnv(Project project, Property<String> propName, Property<String> envName) {
        def value = project.findProperty(propName.get())
        if (!value?.trim()) {
            value = System.getenv(envName.get())
        }
        return value
    }

    private static void augmentManifest(
        Jar task,
        Project project,
        PublishPluginExtension extension
    ) {
        String repoUrl = determineRepoDomainAndPath(project, extension)
        task.manifest {
            attributes(['Implementation-Title'  : project.name,
                        'Implementation-Version': project.version,
                        'Implementation-URL'    : "https://" + repoUrl,
                        'Build-Time'            : new Date().format('yyyy-MM-dd\'T\'HH:mm:ss.SSSZZ')
            ] + getVendorIfAvailable(extension) + getImplementationKotlinVersionIfAvailable(project))
            def licenseTxt = project.file("$project.rootProject.projectDir/LICENSE.txt")
            if (licenseTxt.exists()) task.from(licenseTxt)
            def license = project.file("$project.rootProject.projectDir/LICENSE")
            if (license.exists()) task.from(license)
        }
    }

    private static Map<String, String> getVendorIfAvailable(PublishPluginExtension extension) {
        if (extension.manifestVendor.isPresent()) return ['Implementation-Vendor': extension.manifestVendor.get()]
        else return Collections.emptyMap()
    }

    private static Map<String, String> getImplementationKotlinVersionIfAvailable(Project project) {
        def kotlinVersion = getKotlinVersion(project)
        // we use if instead of ternary operator because type inference fails otherwise
        if (kotlinVersion != null) return ['Implementation-Kotlin-Version': kotlinVersion]
        else return Collections.emptyMap()
    }

    private static String getKotlinVersion(Project project) {
        def plugins = project.plugins
        def kotlinPlugin = plugins.findPlugin('kotlin')
            ?: plugins.findPlugin('kotlin2js')
            ?: plugins.findPlugin('kotlin-platform-jvm')
            ?: plugins.findPlugin('kotlin-platform-js')
            ?: plugins.findPlugin('kotlin-common')
            ?: plugins.findPlugin('org.jetbrains.kotlin.multiplatform')
            ?: plugins.findPlugin('org.jetbrains.kotlin.jvm')
            ?: plugins.findPlugin('org.jetbrains.kotlin.js')
        return kotlinPlugin?.getKotlinPluginVersion()
    }
}
