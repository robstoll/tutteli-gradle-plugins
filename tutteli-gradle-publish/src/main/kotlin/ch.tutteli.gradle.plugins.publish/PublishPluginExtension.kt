package ch.tutteli.gradle.plugins.publish

import org.apache.maven.model.Developer
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property

open class PublishPluginExtension(private val project: Project) {
    companion object {
        const val DEFAULT_DISTRIBUTION = "repo"
    }

    val githubUser: Property<String> = project.objects.property()
    val component: Property<SoftwareComponent> = project.objects.property()
    val artifactFilter: Property<(Jar) -> Boolean> = project.objects.property()
    val licenses: ListProperty<License> = project.objects.listProperty()
    val developers: ListProperty<Developer> = project.objects.listProperty()
    val propNameGpgKeyId: Property<String> = project.objects.property()
    val propNameGpgKeyRing: Property<String> = project.objects.property()
    val propNameGpgPassphrase: Property<String> = project.objects.property()
    val envNameGpgPassphrase: Property<String> = project.objects.property()
    val envNameGpgKeyId: Property<String> = project.objects.property()
    val envNameGpgKeyRing: Property<String> = project.objects.property()
    val envNameGpgSigningKey: Property<String> = project.objects.property()
    val signWithGpg: Property<Boolean> = project.objects.property()
    val manifestVendor: Property<String> = project.objects.property()

    init {
        resetLicenses(StandardLicenses.APACHE_2_0, "repo")
        propNameGpgPassphrase.convention("gpgPassphrase")
        propNameGpgKeyId.convention("gpgKeyId")
        propNameGpgKeyRing.convention("gpgKeyRing")
        envNameGpgPassphrase.convention("GPG_PASSPHRASE")
        envNameGpgKeyId.convention("GPG_KEY_ID")
        envNameGpgKeyRing.convention("GPG_KEY_RING")
        envNameGpgSigningKey.convention("GPG_SIGNING_KEY")
//        if (!signWithGpg.present) {
//            signWithGpg.set(!project.version.endsWith('-SNAPSHOT'))
//        }
        signWithGpg.set(true)

        if (isTutteliProject(project) || isTutteliProject(project.rootProject)) {
            githubUser.set("robstoll")
            manifestVendor.set("tutteli.ch")
            val dev = Developer()
            dev.id = "robstoll"
            dev.name = "Robert Stoll"
            dev.email = "rstoll@tutteli.ch"
            dev.url = "https://tutteli.ch"
            developers.add(dev)
        }

        // reset group of sub-projects
        if (project.rootProject != project && project.group == project.rootProject.name) {
            project.group = ""
        }
        useJavaComponentIfJavaPluginAvailable()
    }

    private fun useJavaComponentIfJavaPluginAvailable() {
        val component = project.components.findByName("java")
        if (component != null) {
            this.component.set(component)
        }
    }

    private fun isTutteliProject(project: Project): Boolean =
        (project.group as? CharSequence)?.startsWith("ch.tutteli") ?: false


    /**
     * Resets all previously set licenses and adds the given - can be used to override the default license.
     * Use {@link #license(java.lang.String)} to specify additional licenses.
     */
    fun resetLicenses(license: String) {
        resetLicenses(license, DEFAULT_DISTRIBUTION)
    }

    /**
     * Resets all previously set licenses and adds the given - can be used to override the default license.
     * Use {@link #license(java.lang.String, java.lang.String)} to specify additional licenses.
     */
    fun resetLicenses(license: String, distribution: String) {
        resetLicenses(StandardLicenses.fromShortName(license), distribution)
    }

    /**
     * Resets all previously set licenses and adds the given - can be used to override the default license.
     * Use {@link #license(StandardLicense)} to specify additional licenses.
     */
    fun resetLicenses(standardLicense: StandardLicenses) {
        resetLicenses(standardLicense, DEFAULT_DISTRIBUTION)
    }

    /**
     * Resets all previously set licenses and adds the given - can be used to override the default license.
     * Use {@link #license(StandardLicense, java.lang.String)} to specify additional licenses.
     */
    fun resetLicenses(standardLicense: StandardLicenses, distribution: String) {
        resetLicenses(License(standardLicense, distribution))
    }

    /**
     * Resets all previously set licenses and adds the given - can be used to override the default license.
     * Use {@link #license(org.gradle.api.Action)} to specify additional licenses.
     */
    fun resetLicenses(newLicense: License) {
        val licenses = ArrayList<License>(5)
        licenses.add(newLicense)
        this.licenses.set(licenses)
    }

    fun license(additionalLicense: String) {
        license(additionalLicense, DEFAULT_DISTRIBUTION)
    }

    fun license(additionalLicense: String, distribution: String) {
        license(StandardLicenses.fromShortName(additionalLicense), distribution)
    }

    fun license(standardLicense: StandardLicenses) {
        license(standardLicense, DEFAULT_DISTRIBUTION)
    }

    fun license(standardLicense: StandardLicenses, distribution: String) {
        license(License(standardLicense, distribution))
    }

    fun license(license: License) {
        licenses.add(license)
    }

    fun developer(developer: Action<Developer>) {
        val newDeveloper = project.objects.newInstance(Developer::class.java)
        developer.execute(newDeveloper)
        developers.add(newDeveloper)
    }

    fun determineRepoDomainAndPath(): String =
        "github.com/${githubUser.get()}/${project.rootProject.name}"

}
