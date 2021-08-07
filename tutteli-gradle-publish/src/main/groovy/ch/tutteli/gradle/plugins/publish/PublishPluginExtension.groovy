package ch.tutteli.gradle.plugins.publish

import org.apache.maven.model.Developer
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

import static Validation.requireNotNullNorBlank

class PublishPluginExtension {
    private static final String DEFAULT_DISTRIBUTION = 'repo'
    private Project project

    final Property<String> githubUser
    final Property<SoftwareComponent> component
    final Property<Closure<Boolean>> artifactFilter
    final ListProperty<License> licenses
    final ListProperty<Developer> developers
    final Property<String> propNameGpgKeyId
    final Property<String> propNameGpgKeyRing
    final Property<String> propNameGpgPassphrase
    final Property<String> envNameGpgPassphrase
    final Property<String> envNameGpgKeyId
    final Property<String> envNameGpgKeyRing
    final Property<String> envNameGpgSigningKey
    final Property<Boolean> signWithGpg
    final Property<String> manifestVendor

    PublishPluginExtension(Project project) {
        this.project = project
        githubUser = project.objects.property(String)
        component = project.objects.property(SoftwareComponent)
        artifactFilter = project.objects.<Closure<Boolean>>property(Closure)
        licenses = project.objects.listProperty(License)
        resetLicenses(StandardLicenses.APACHE_2_0, 'repo')
        developers = project.objects.listProperty(Developer)
        propNameGpgPassphrase = project.objects.property(String)
        propNameGpgPassphrase.set('gpgPassphrase')
        propNameGpgKeyId = project.objects.property(String)
        propNameGpgKeyId.set('gpgKeyId')
        propNameGpgKeyRing = project.objects.property(String)
        propNameGpgKeyRing.set('gpgKeyRing')

        envNameGpgPassphrase = project.objects.property(String)
        envNameGpgPassphrase.set('GPG_PASSPHRASE')
        envNameGpgKeyId = project.objects.property(String)
        envNameGpgKeyId.set('GPG_KEY_ID')
        envNameGpgKeyRing = project.objects.property(String)
        envNameGpgKeyRing.set('GPG_KEY_RING')
        envNameGpgSigningKey = project.objects.property(String)
        envNameGpgSigningKey.set('GPG_SIGNING_KEY')

        signWithGpg = project.objects.property(Boolean)
        signWithGpg.set(true)
//        if (!signWithGpg.present) {
//            signWithGpg.set(!project.version.endsWith('-SNAPSHOT'))
//        }
        manifestVendor = project.objects.property(String)

        if (isTutteliProject(project) || isTutteliProject(project.rootProject)) {
            githubUser.set('robstoll')
            manifestVendor.set('tutteli.ch')
            def dev = new Developer()
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

    private void useJavaComponentIfJavaPluginAvailable() {
        def component = project.components.findByName('java')
        if (component != null) {
            this.component.set(component)
        }
    }

    private static boolean isTutteliProject(Project project) {
        return project.group?.startsWith("ch.tutteli")
    }

    /**
     * Resets all previously set licenses and adds the given - can be used to override the default license.
     * Use {@link #license(java.lang.String)} to specify additional licenses.
     */
    void resetLicenses(String license) {
        resetLicenses(license, DEFAULT_DISTRIBUTION)
    }

    /**
     * Resets all previously set licenses and adds the given - can be used to override the default license.
     * Use {@link #license(java.lang.String, java.lang.String)} to specify additional licenses.
     */
    void resetLicenses(String license, String distribution) {
        resetLicenses(StandardLicenses.fromShortName(license), distribution)
    }

    /**
     * Resets all previously set licenses and adds the given - can be used to override the default license.
     * Use {@link #license(StandardLicenses)} to specify additional licenses.
     */
    void resetLicenses(StandardLicenses standardLicense) {
        resetLicenses(standardLicense, DEFAULT_DISTRIBUTION)
    }

    /**
     * Resets all previously set licenses and adds the given - can be used to override the default license.
     * Use {@link #license(StandardLicenses, java.lang.String)} to specify additional licenses.
     */
    void resetLicenses(StandardLicenses standardLicense, String distribution) {
        setNewLicense(new LicenseImpl(standardLicense, distribution))
    }

    /**
     * Resets all previously set licenses and adds the given - can be used to override the default license.
     * Use {@link #license(org.gradle.api.Action)} to specify additional licenses.
     */
    void resetLicenses(Action<License> license) {
        setNewLicense(applyClosureToNewLicense(license))
    }

    private setNewLicense(License newLicense) {
        def licenses = new ArrayList<License>(5)
        licenses.add(newLicense)
        this.licenses.set(licenses)
    }

    void license(String additionalLicense) {
        license(additionalLicense, DEFAULT_DISTRIBUTION)
    }

    void license(String additionalLicense, String distribution) {
        license(StandardLicenses.fromShortName(additionalLicense), distribution)
    }

    void license(StandardLicenses standardLicense) {
        license(standardLicense, DEFAULT_DISTRIBUTION)
    }

    void license(StandardLicenses standardLicense, String distribution) {
        addNewLicense(new LicenseImpl(standardLicense, distribution))
    }

    void license(Action<License> license) {
        addNewLicense(applyClosureToNewLicense(license))
    }

    private void addNewLicense(License license) {
        licenses.add(license)
    }

    private License applyClosureToNewLicense(Action<License> license) {
        def newLicense = project.objects.newInstance(LicenseImpl as Class<License>)
        newLicense.distribution = 'repo'
        license.execute(newLicense)
        requireNotNullNorBlank(newLicense.shortName, "${PublishPlugin.EXTENSION_NAME}.license.shortName")
        requireNotNullNorBlank(newLicense.longName, "${PublishPlugin.EXTENSION_NAME}.license.longName")
        requireNotNullNorBlank(newLicense.url, "${PublishPlugin.EXTENSION_NAME}.license.url")
        requireNotNullNorBlank(newLicense.distribution, "${PublishPlugin.EXTENSION_NAME}.license.distribution")
        newLicense
    }

    void developer(Action<Developer> developer) {
        def newDeveloper = project.objects.newInstance(Developer)
        developer.execute(newDeveloper)
        developers.add(newDeveloper)
    }
}
