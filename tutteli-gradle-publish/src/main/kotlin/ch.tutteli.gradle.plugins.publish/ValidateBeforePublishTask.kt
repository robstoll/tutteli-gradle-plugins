package ch.tutteli.gradle.plugins.publish

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.signing.SigningExtension


open class ValidateBeforePublishTask : DefaultTask() {

    @TaskAction
    fun validate() {
        val extension = project.extensions.getByType<PublishPluginExtension>()

        if (extension.signWithGpg.get()) {
            configureSigningForSigningPlugin(extension)

            val signingPassword = getProjectExtraThrowIfNullOrBlank(
                "signing.password", extension.propNameGpgPassphrase,
                extension.envNameGpgPassphrase
            )


            val envNameGpgSigningKey = extension.envNameGpgSigningKey.get()
            val signingKey = System.getenv(envNameGpgSigningKey)

            // if we don't use in memory, then we have to provide a file
            if (signingKey.isNullOrBlank()) {
                getProjectExtraThrowIfNullOrBlank(
                    "signing.keyId",
                    extension.propNameGpgKeyId,
                    extension.envNameGpgKeyId
                )
                getProjectExtraThrowIfNullOrBlank(
                    "signing.secretKeyRingFile",
                    extension.propNameGpgKeyRing,
                    extension.envNameGpgKeyRing
                )
            } else {
                if ((project.extra.get("signing.keyId") as? CharSequence).isNullOrBlank()) {
                    throwIllegalState("you are not allowed to specify an in memory GPG singing key (via $envNameGpgSigningKey) as well as signing.keyId on project.extra")
                }
                if ((project.extra.get("signing.secretKeyRingFile") as? CharSequence).isNullOrBlank()) {
                    throwIllegalState("you are not allowed to specify an in memory GPG singing key (via $envNameGpgSigningKey) as well as signing.secretKeyRingFile on project.extra")
                }
                val signingExtension = project.extensions.getByType<SigningExtension>()
                signingExtension.useInMemoryPgpKeys(signingKey, signingPassword)
            }
        }
    }

    private fun configureSigningForSigningPlugin(extension: PublishPluginExtension) {
        project.extra.set(
            "signing.password",
            getPropertyOrSystemEnv(project, extension.propNameGpgPassphrase, extension.envNameGpgPassphrase)
        )
        project.extra.set(
            "signing.keyId",
            getPropertyOrSystemEnv(project, extension.propNameGpgKeyId, extension.envNameGpgKeyId)
        )
        project.extra.set(
            "signing.secretKeyRingFile",
            getPropertyOrSystemEnv(project, extension.propNameGpgKeyRing, extension.envNameGpgKeyRing)
        )
    }

    private fun getPropertyOrSystemEnv(
        project: Project,
        propName: Property<String>,
        envName: Property<String>
    ): String? {
        val property = project.findProperty(propName.get())
        val value = (property as? CharSequence)?.toString()
        return if (value.isNullOrBlank()) System.getenv(envName.get()) else value
    }

    private fun getProjectExtraThrowIfNullOrBlank(
        projectExtra: String,
        propName: Property<String>,
        envName: Property<String>
    ): String {
        val property = project.extra.get(projectExtra)
        val value = (property as? CharSequence)?.toString()
        if (value.isNullOrBlank()) throwIllegalState(
            "property with name ${propName.get()} or System.env variable with name ${envName.get()}"
        )
        return value
    }

}
