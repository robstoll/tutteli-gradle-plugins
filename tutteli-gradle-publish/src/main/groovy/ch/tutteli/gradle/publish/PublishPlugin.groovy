package ch.tutteli.gradle.publish

import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.BintrayPlugin
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.bundling.Jar

import static ch.tutteli.gradle.publish.Validation.*

class PublishPlugin implements Plugin<Project> {
    private static final Logger LOGGER = Logging.getLogger(PublishPlugin.class)
    static final String EXTENSION_NAME = 'tutteliPublish'
    static final String TASK_NAME_INCLUDE_TIME = 'includeBuildTimeInManifest'
    static final String TASK_NAME_PUBLISH_TO_BINTRAY = 'publishToBintray'
    static final String TASK_NAME_SOURCES_JAR = 'sourcesJar'
    static final String TASK_NAME_VALIDATE = 'validateBeforePublishToBintray'

    @Override
    void apply(Project project) {
        project.plugins.apply(MavenPublishPlugin)
        project.plugins.apply(BintrayPlugin)

        if (!project.hasProperty('sourceSets')) throw new IllegalStateException(
            "The project $project.name does not have any sources. We currently require a project to have sources in order to publish it." +
                "\nPlease make sure you do not apply the ch.tutteli.$EXTENSION_NAME plugin before the plugin which provides the sourceSets (e.g. kotlin or java and the like)" +
                "\nPlease open an issue if you would like to publish projects without sources: https://github.com/robstoll/tutteli-gradle-plugins/issues/new"
        )

        project.tasks.create(name: TASK_NAME_SOURCES_JAR, type: Jar) {
            from project.sourceSets.main.allSource
            classifier = 'sources'
        }

        def extension = project.extensions.create(EXTENSION_NAME, PublishPluginExtension, project)

        def validateBeforePublish = project.tasks.create(name: TASK_NAME_VALIDATE, type: ValidateBeforePublishTask)
        validateBeforePublish.project = project
        validateBeforePublish.extension = extension

        def includeBuildTime = project.tasks.create(name: TASK_NAME_INCLUDE_TIME) {
            doLast {
                extension.artifacts.getOrElse(Collections.emptyList())
                    .findAll { it instanceof org.gradle.jvm.tasks.Jar }
                    .collect { it as org.gradle.jvm.tasks.Jar }
                    .each { augmentManifest(it, project, extension) }
            }
        }

        project.tasks.create(name: TASK_NAME_PUBLISH_TO_BINTRAY) {
            def bintrayUpload = project.tasks.getByName('bintrayUpload')
            dependsOn validateBeforePublish
            dependsOn includeBuildTime
            dependsOn bintrayUpload

            includeBuildTime.mustRunAfter(validateBeforePublish)
            bintrayUpload.mustRunAfter(includeBuildTime)
        }

        project.afterEvaluate {
            project.version = determineVersion(project) ?: determineVersion(project.rootProject) ?: ""
            project.group = project.group ?: project.rootProject.group
            requireNotNullNorBlank(project.name, "project.name")
            requireNotNullNorBlank(project.version, "project.version or rootProject.version")
            requireNotNullNorBlank(project.group, "project.group or rootProject.group")
            requireNotNullNorBlank(project.description, "project.description")
            requireComponentOrArtifactsPresent(extension)
            requireExtensionPropertyPresentAndNotBlank(extension.githubUser, "githubUser")
            requireExtensionPropertyPresentNotEmpty(extension.licenses, "licenses")

            def bintrayExtension = project.extensions.getByType(BintrayExtension)
            requireExtensionPropertyPresentAndNotBlank(extension.envNameBintrayUser, "envNameBintrayUser")
            requireExtensionPropertyPresentAndNotBlank(extension.envNameBintrayApiKey, "envNameBintrayApiKey")
            requireExtensionPropertyPresentAndNotBlank(extension.envNameBintrayGpgPassphrase, "envNameBintrayGpgPassphrase")
            requireSetOnBintrayExtensionOrProperty(bintrayExtension.pkg.repo, extension.bintrayRepo, "bintrayRepo")

            configurePublishing(project, extension)
            configureBintray(project, extension, bintrayExtension)
        }
    }

    private static String determineRepoUrl(Project project, PublishPluginExtension extension) {
        return "https://github.com/${extension.githubUser.get()}/$project.rootProject.name"
    }

    private static String determineVersion(Project project) {
        return project.version == "unspecified" ? null : project.version
    }


    private static void requireComponentOrArtifactsPresent(PublishPluginExtension extension) {
        if (!extension.component.isPresent() && extension.artifacts.map { it.isEmpty() }.getOrElse(true)) {
            throw newIllegalState("either ${EXTENSION_NAME}.component or ${EXTENSION_NAME}.artifacts")
        }
    }

    private static void configurePublishing(Project project, PublishPluginExtension extension) {
        def aId = determineArtifactId(project)
        project.publishing {
            publications {
                tutteli(MavenPublication) {
                    groupId project.group
                    artifactId aId
                    version project.version

                    MavenPublication publication = it
                    if (extension.component.isPresent()) {
                        def component = extension.component.get()
                        publication.from(component)

                        // Not the best solution only works for component java
                        if (component == project.components.findByName('java')) {
                            def jarPub = publication.artifacts.find {
                                it.file.name.endsWith(project.name + "-" + project.version + ".jar")
                            }
                            publication.artifacts.remove(jarPub)
                            def jarTask = project.tasks.getByName('jar')
                            if (!extension.artifacts.get().contains(jarTask)) {
                                extension.artifacts.add(jarTask)
                            }
                        }

                    }
                    def artifacts = extension.artifacts.getOrElse(Collections.emptyList())
                    artifacts.each {
                        if (it instanceof org.gradle.jvm.tasks.Jar) {
                            it.baseName = aId
                        }
                        publication.artifact it
                    }
                    pom.withXml(pomConfig(project, extension))
                }
            }
        }
    }

    private static String determineArtifactId(Project project) {
        def name = project.name
        return name.endsWith("-jvm") ?
            name.substring(0, name.lastIndexOf("-jvm")) :
            name
    }

    private static Action<? extends XmlProvider> pomConfig(Project project, PublishPluginExtension extension) {
        String repoUrl = determineRepoUrl(project, extension)
        def extensionLicenses = extension.licenses.get()
        def uniqueLicenses = extensionLicenses.toSet().toSorted()
        if (extensionLicenses.size() != uniqueLicenses.size()) {
            LOGGER.warn("Some licenses were duplicated. Please check if you made a mistake.")
        }
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
        BintrayExtension bintrayExtension
    ) {
        def uniqueShortNames = extension.licenses.get().collect { it.shortName }.toSet().toSorted() as String[]
        def repoUrl = determineRepoUrl(project, extension)

        bintrayExtension.with {
            user = user ?: getPropertyOrSystemEnv(project, extension.propNameBintrayUser, extension.envNameBintrayUser)
            key = key ?: getPropertyOrSystemEnv(project, extension.propNameBintrayApiKey, extension.envNameBintrayApiKey)
            publications = ['tutteli'] as String[]

            pkg.with {
                repo = repo ?: extension.bintrayRepo.get()
                def pkgName = name ?: extension.bintrayPkg.getOrElse(project.rootProject.name)
                name = pkgName
                userOrg = userOrg ?: extension.bintrayOrganisation.getOrElse(null)
                licenses = licenses ?: uniqueShortNames
                vcsUrl = vcsUrl ?: repoUrl
                version.with {
                    name = name ?: project.version as String
                    desc = desc ?: "${determineArtifactId(project)} $project.version"
                    released = released ?: new Date().format('yyyy-MM-dd\'T\'HH:mm:ss.SSSZZ')
                    vcsTag = vcsTag ?: "v$project.version"
                    gpg.with {
                        sign = extension.signWithGpg.get()
                        if (sign) {
                            passphrase = passphrase ?: getPropertyOrSystemEnv(project, extension.propNameBintrayGpgPassphrase, extension.envNameBintrayGpgPassphrase)
                        }
                    }
                }
            }
        }
    }

    private static String getPropertyOrSystemEnv(Project project, Property<String> propName, Property<String> envName) {
        def value = project.findProperty(propName.get())
        if (!value?.trim()) {
            value = System.getenv(envName.get())
        }
        return value
    }

    private static void augmentManifest(
        org.gradle.jvm.tasks.Jar task,
        Project project,
        PublishPluginExtension extension
    ) {
        String repoUrl = determineRepoUrl(project, extension)
        task.manifest {
            attributes(['Implementation-Title'  : determineArtifactId(project),
                        'Implementation-Version': project.version,
                        'Implementation-URL'    : repoUrl,
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
        //we use if instead of ternary operator because type inference fails otherwise
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
        return kotlinPlugin?.getKotlinPluginVersion()
    }
}
