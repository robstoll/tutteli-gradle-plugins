package ch.tutteli.gradle.publish

import com.jfrog.bintray.gradle.BintrayExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property

import javax.inject.Inject

import static ch.tutteli.gradle.publish.Validation.newIllegalState

class ValidateBeforePublishTask extends DefaultTask {
    Project project
    PublishPluginExtension extension

    def validate() {
        def bintrayExtension = project.extensions.getByType(BintrayExtension)
        if (!bintrayExtension.user?.trim()) throw throwIllegalPropertyNorSystemEnvSet(extension.propNameBintrayUser, extension.envNameBintrayUser)
        if (!bintrayExtension.key?.trim()) throw throwIllegalPropertyNorSystemEnvSet(extension.propNameBintrayApiKey, extension.envNameBintrayApiKey)
        if (bintrayExtension.pkg.version.gpg.sign && !bintrayExtension.pkg.version.gpg.passphrase?.trim()) throw throwIllegalPropertyNorSystemEnvSet(extension.propNameBintrayGpgPassphrase, extension.envNameBintrayGpgPassphrase)
    }

    private static void throwIllegalPropertyNorSystemEnvSet(Property<String> propName, Property<String> envName) {
        throw newIllegalState("property with name ${propName.get()} or System.env variable with name ${envName.get()}")
    }
}
