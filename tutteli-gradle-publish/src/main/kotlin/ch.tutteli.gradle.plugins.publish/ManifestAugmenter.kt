package ch.tutteli.gradle.plugins.publish

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.PluginContainer
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import kotlin.reflect.full.memberProperties

class ManifestAugmenter(
    private val project: Project,
    private val extension: PublishPluginExtension
) {

    fun augment(jarTask: Jar) {
        val repoUrl = extension.determineRepoDomainAndPath()
        jarTask.manifest {
            attributes(
                mapOf(
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version,
                    "Implementation-URL" to "https://$repoUrl",
                ) + getVendorIfAvailable(extension) + getImplementationKotlinVersionIfAvailable(project)
            )
            listOf("LICENSE.txt", "LICENSE", "LICENSE.md", "LICENSE.rst").forEach { fileName ->
                val licenseFile = project.file("${project.rootProject.projectDir}/$fileName")
                if (licenseFile.exists()) jarTask.from(licenseFile)
            }
        }
    }

    private fun getVendorIfAvailable(extension: PublishPluginExtension): Map<String, String> =
        if (extension.manifestVendor.isPresent) mapOf("Implementation-Vendor" to extension.manifestVendor.get())
        else emptyMap()

    private fun getImplementationKotlinVersionIfAvailable(project: Project): Map<String, String> {
        val kotlinVersion = getKotlinVersion(project)
        return if (kotlinVersion != null) mapOf("Implementation-Kotlin-Version" to kotlinVersion)
        else emptyMap()
    }

    private fun getKotlinVersion(project: Project): String? {
        val version = try {
            project.getKotlinPluginVersion()
        } catch (e: NoClassDefFoundError) {
            // KotlinPluginWrapperKt (source where extension method getKotlinPluginVersion is defined) might not exist
            // if no kotlin plugin was applied or an old one or an old gradle version is used where the extension
            // method on Project does not exist yet
            null
        }

        return version
        //TODO 6.0.0 drop once we no longer support gradle < 8 ?
            ?: (project.plugins.let<PluginContainer, Plugin<*>?> {
                it.findPlugin("org.jetbrains.kotlin.multiplatform") ?: it.findPlugin("org.jetbrains.kotlin.jvm")
                ?: it.findPlugin("org.jetbrains.kotlin.js")
            }?.let {
                val value = it::class.memberProperties.find { property ->
                    property.name == "kotlinPluginVersion"
                }?.call(it)
                (value as? CharSequence)?.toString()
            })
    }
}
