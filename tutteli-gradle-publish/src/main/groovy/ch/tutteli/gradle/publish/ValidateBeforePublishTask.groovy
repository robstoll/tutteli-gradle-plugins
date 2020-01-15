package ch.tutteli.gradle.publish

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.plugins.signing.SigningExtension

import static ch.tutteli.gradle.publish.Validation.newIllegalState
import static ch.tutteli.gradle.publish.Validation.throwIllegalPropertyNorSystemEnvSet

class ValidateBeforePublishTask extends DefaultTask {
    @TaskAction
    def validate() {
        def extension = project.extensions.getByName(PublishPlugin.EXTENSION_NAME)

        if (extension.signWithGpg.get()) {
            configureSigning(extension)

            def signingPassword = (project.ext."signing.password")?.trim()
            if (!signingPassword) throw throwIllegalPropertyNorSystemEnvSet(extension.propNameGpgPassphrase, extension.envNameGpgPassphrase)

            def envNameGpgSigningKey = extension.envNameGpgSigningKey.get()
            def signingKey = System.getenv(envNameGpgSigningKey)

            // if we don't use in memory, then we have to provide a file
            if (!signingKey?.trim()) {
                if (!(project.ext."signing.keyId")?.trim()) throw throwIllegalPropertyNorSystemEnvSet(extension.propNameGpgKeyId, extension.envNameGpgKeyId)
                if (!(project.ext."signing.secretKeyRingFile")?.trim()) throw throwIllegalPropertyNorSystemEnvSet(extension.propNameGpgKeyRing, extension.envNameGpgKeyRing)
            } else {
                if ((project.ext."signing.keyId")?.trim()) {
                    throw newIllegalState("you are not allowed to specify an in memory GPG singing key (via $envNameGpgSigningKey) as well as project.ext.signing.keyId")
                }
                if ((project.ext."signing.secretKeyRingFile")?.trim()) {
                    throw newIllegalState("you are not allowed to specify an in memory GPG singing key (via $envNameGpgSigningKey) as well as project.ext.signing.secretKeyRingFile")
                }
                def signingExtension = project.extensions.getByType(SigningExtension)
                signingExtension.configure {
                    useInMemoryPgpKeys(signingKey, signingPassword)
                }
            }
        }
    }

    private void configureSigning(PublishPluginExtension extension) {
        project.ext."signing.password" = PublishPlugin.getPropertyOrSystemEnv(project, extension.propNameGpgPassphrase, extension.envNameGpgPassphrase)
        project.ext."signing.keyId" = PublishPlugin.getPropertyOrSystemEnv(project, extension.propNameGpgKeyId, extension.envNameGpgKeyId)
        project.ext."signing.secretKeyRingFile" = PublishPlugin.getPropertyOrSystemEnv(project, extension.propNameGpgKeyRing, extension.envNameGpgKeyRing)
    }
}
