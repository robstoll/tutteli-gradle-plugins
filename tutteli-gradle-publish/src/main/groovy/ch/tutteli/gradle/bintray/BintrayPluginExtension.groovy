package ch.tutteli.gradle.bintray

import org.apache.maven.model.Developer
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

import static ch.tutteli.gradle.bintray.BintrayPlugin.EXTENSION_NAME
import static ch.tutteli.gradle.bintray.Validation.requireNotNullNorEmpty

class BintrayPluginExtension {
    private static final String DEFAULT_DISTRIBUTION = 'repo'

    final Property<String> githubUser
    final Property<SoftwareComponent> component
    //TODO switch to SetProperty requires gradle 4.5
    final ListProperty<Task> artifacts
    final ListProperty<License> licenses
    final ListProperty<Developer> developers
    private Project project


    BintrayPluginExtension(Project project) {
        this.project = project
        githubUser = project.objects.property(String)
        component = project.objects.property(SoftwareComponent)
        artifacts = project.objects.listProperty(Task)
        licenses = project.objects.listProperty(License)
        overrideDefaultLicense(StandardLicenses.APACHE_2_0, 'repo')
        developers = project.objects.listProperty(Developer)

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
     * Use {@link #license(ch.tutteli.gradle.bintray.StandardLicenses)} to specify additional licenses
     */
    void overrideDefaultLicense(StandardLicenses standardLicense) {
        overrideDefaultLicense(standardLicense, DEFAULT_DISTRIBUTION)
    }

    /**
     * Resets all previously set licenses and add the given, should only be used to override the default.
     * Use {@link #license(ch.tutteli.gradle.bintray.StandardLicenses, java.lang.String)} to specify additional licenses
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
        requireNotNullNorEmpty(newLicense.shortName, "${EXTENSION_NAME}.license.shortName")
        requireNotNullNorEmpty(newLicense.longName, "${EXTENSION_NAME}.license.longName")
        requireNotNullNorEmpty(newLicense.url, "${EXTENSION_NAME}.license.url")
        requireNotNullNorEmpty(newLicense.distribution, "${EXTENSION_NAME}.license.distribution")
        newLicense
    }

    void developer(Action<Developer> developer) {
        def newDeveloper = project.objects.newInstance(Developer)
        developer.execute(newDeveloper)
        developers.add(newDeveloper)
    }

}
