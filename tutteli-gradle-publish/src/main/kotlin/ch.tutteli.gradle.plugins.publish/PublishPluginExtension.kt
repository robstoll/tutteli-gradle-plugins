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
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.signatory.pgp.PgpSignatoryProvider

open class PublishPluginExtension(private val project: Project) {
    companion object {
        const val DEFAULT_DISTRIBUTION = "repo"
    }

    val githubUser: Property<String> = project.objects.property()
    val component: Property<SoftwareComponent> = project.objects.property()
    val artifactFilter: Property<(Jar) -> Boolean> = project.objects.property()
    val licenses: ListProperty<License> = project.objects.listProperty()
    val developers: ListProperty<Developer> = project.objects.listProperty()

    val signWithGpg: Property<Boolean> = project.objects.property()
    val manifestVendor: Property<String> = project.objects.property()

    init {
        resetLicenses(StandardLicenses.APACHE_2_0, "repo")
//        if (!signWithGpg.present) {
//            signWithGpg.set(!project.version.endsWith('-SNAPSHOT'))
//        }
        signWithGpg.convention(true)

        if (isTutteliProject(project) || isTutteliProject(project.rootProject)) {
            githubUser.set("robstoll")
            manifestVendor.set("tutteli.ch")
            addDeveloper {
                id = "robstoll"
                name = "Robert Stoll"
                email = "rstoll@tutteli.ch"
                url = "https://tutteli.ch"
            }
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
     * Use [PublishPluginExtension.licenses] to specify additional licenses.
     */
    fun resetLicenses(license: String) {
        resetLicenses(license, DEFAULT_DISTRIBUTION)
    }

    /**
     * Resets all previously set licenses and adds the given - can be used to override the default license.
     * Use [PublishPluginExtension.licenses] to specify additional licenses.
     */
    fun resetLicenses(license: String, distribution: String) {
        resetLicenses(StandardLicenses.fromShortName(license), distribution)
    }

    /**
     * Resets all previously set licenses and adds the given - can be used to override the default license.
     * Use [PublishPluginExtension.licenses] to specify additional licenses.
     */
    fun resetLicenses(standardLicense: StandardLicenses) {
        resetLicenses(standardLicense, DEFAULT_DISTRIBUTION)
    }

    /**
     * Resets all previously set licenses and adds the given - can be used to override the default license.
     * Use [PublishPluginExtension.licenses] to specify additional licenses.
     */
    fun resetLicenses(standardLicense: StandardLicenses, distribution: String) {
        resetLicenses(License(standardLicense, distribution))
    }

    /**
     * Resets all previously set licenses and adds the given - can be used to override the default license.
     * Use [PublishPluginExtension.licenses] to specify additional licenses.
     */
    fun resetLicenses(newLicense: License) {
        val licenses = ArrayList<License>(5)
        licenses.add(newLicense)
        this.licenses.set(licenses)
    }

    fun addLicense(additionalLicense: String) {
        addLicense(additionalLicense, DEFAULT_DISTRIBUTION)
    }

    fun addLicense(additionalLicense: String, distribution: String) {
        addLicense(StandardLicenses.fromShortName(additionalLicense), distribution)
    }

    fun addLicense(standardLicense: StandardLicenses) {
        addLicense(standardLicense, DEFAULT_DISTRIBUTION)
    }

    fun addLicense(standardLicense: StandardLicenses, distribution: String) {
        addLicense(License(standardLicense, distribution))
    }

    fun addLicense(license: License) {
        licenses.add(license)
    }

    fun addDeveloper(developer: Action<Developer>) {
        val newDeveloper = project.objects.newInstance(Developer::class.java)
        newDeveloper.organization
        developer.execute(newDeveloper)
        developers.add(newDeveloper)
    }

    fun determineRepoDomainAndPath(): String =
        "github.com/${githubUser.get()}/${project.rootProject.name}"

    fun usePgpJava() {
        val signingExtension = project.the<SigningExtension>()
        signingExtension.signatories = PgpSignatoryProvider()
    }

}
