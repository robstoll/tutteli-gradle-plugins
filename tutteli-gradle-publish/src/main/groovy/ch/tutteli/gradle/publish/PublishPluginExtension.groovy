package ch.tutteli.gradle.publish

import com.jfrog.bintray.gradle.BintrayExtension
import org.apache.maven.model.Developer
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

import static Validation.requireNotNullNorBlank

class PublishPluginExtension {
    private static final String DEFAULT_DISTRIBUTION = 'repo'
    private Project project
    private BintrayExtension bintrayExtension

    final Property<String> githubUser
    final Property<SoftwareComponent> component
    //TODO switch to SetProperty requires gradle 4.5
    final ListProperty<Task> artifacts
    final ListProperty<License> licenses
    final ListProperty<Developer> developers
    final Property<String> propNameBintrayUser
    final Property<String> propNameBintrayApiKey
    final Property<String> propNameBintrayGpgPassphrase
    final Property<String> envNameBintrayUser
    final Property<String> envNameBintrayApiKey
    final Property<String> envNameBintrayGpgPassphrase
    final Property<String> bintrayRepo
    final Property<String> bintrayPkg
    final Property<Boolean> signWithGpg
    final Property<String> manifestVendor

    PublishPluginExtension(Project project) {
        this.project = project
        bintrayExtension = project.extensions.getByType(BintrayExtension)

        githubUser = project.objects.property(String)
        component = project.objects.property(SoftwareComponent)
        artifacts = project.objects.listProperty(Task)
        licenses = project.objects.listProperty(License)
        overrideDefaultLicense(StandardLicenses.APACHE_2_0, 'repo')
        developers = project.objects.listProperty(Developer)
        propNameBintrayUser = project.objects.property(String)
        propNameBintrayUser.set('bintrayUser')
        propNameBintrayApiKey = project.objects.property(String)
        propNameBintrayApiKey.set('bintrayApiKey')
        propNameBintrayGpgPassphrase = project.objects.property(String)
        propNameBintrayGpgPassphrase.set('bintrayGpgPassphrase')
        envNameBintrayUser = project.objects.property(String)
        envNameBintrayUser.set('BINTRAY_USER')
        envNameBintrayApiKey = project.objects.property(String)
        envNameBintrayApiKey.set('BINTRAY_API_KEY')
        envNameBintrayGpgPassphrase = project.objects.property(String)
        envNameBintrayGpgPassphrase.set('BINTRAY_GPG_PASSPHRASE')
        bintrayRepo = project.objects.property(String)
        bintrayPkg = project.objects.property(String)
        signWithGpg = project.objects.property(Boolean)
        signWithGpg.set(true)
        manifestVendor = project.objects.property(String)

        useSourcesJarAsArtifact()
        useJavaComponentIfJavaPluginAvailable()
        useJavadocJarAsArtifactIfAvailable()
    }

    private void useSourcesJarAsArtifact(){
        artifacts.add(project.tasks.getByName(PublishPlugin.TASK_NAME_SOURCES_JAR))
    }

    private void useJavaComponentIfJavaPluginAvailable() {
        def name = project.components.findByName('java')
        if (name != null) {
            component.set(name)
        }
    }

    private void useJavadocJarAsArtifactIfAvailable() {
        def jar = project.tasks.findByName('javadocJar')
        if (jar != null) {
            artifacts.add(jar)
        }
    }

    /**
     * Resets all previously set licenses and adds the given, should only be used to override the default.
     * Use {@link #license(java.lang.String)} to specify additional licenses
     */
    void overrideDefaultLicense(String license) {
        overrideDefaultLicense(license, DEFAULT_DISTRIBUTION)
    }

    /**
     * Resets all previously set licenses and add the given, should only be used to override the default.
     * Use {@link #license(java.lang.String, java.lang.String)} to specify additional licenses
     */
    void overrideDefaultLicense(String license, String distribution) {
        overrideDefaultLicense(StandardLicenses.fromShortName(license), distribution)
    }

    /**
     * Resets all previously set licenses and add the given, should only be used to override the default.
     * Use {@link #license(StandardLicenses)} to specify additional licenses
     */
    void overrideDefaultLicense(StandardLicenses standardLicense) {
        overrideDefaultLicense(standardLicense, DEFAULT_DISTRIBUTION)
    }

    /**
     * Resets all previously set licenses and add the given, should only be used to override the default.
     * Use {@link #license(StandardLicenses, java.lang.String)} to specify additional licenses
     */
    void overrideDefaultLicense(StandardLicenses standardLicense, String distribution) {
        setNewLicense(new LicenseImpl(standardLicense, distribution))
    }

    /**
     * Resets all previously set licenses and add the given, should only be used to override the default.
     * Use {@link #license(org.gradle.api.Action)} to specify additional licenses
     */
    void overrideDefaultLicense(Action<License> license) {
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

    private boolean addNewLicense(License license) {
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

    void bintray(Action<BintrayExtension> bintray) {
        bintray.execute(bintrayExtension)
    }
}
