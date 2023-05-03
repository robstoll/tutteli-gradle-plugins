package ch.tutteli.gradle.plugins.publish

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.full.memberProperties

abstract class AugmentManifestInJarTask : DefaultTask() {

    @get:Input
    abstract val jarTask: Property<Jar>

    @TaskAction
    fun configure() {
        val extension = project.extensions.getByType<PublishPluginExtension>()
        augmentManifest(extension)
    }

    private fun augmentManifest(extension: PublishPluginExtension) {
        val repoUrl = extension.determineRepoDomainAndPath()
        jarTask.get().apply {
            manifest {
                attributes(
                    mapOf(
                        "Implementation-Title" to project.name,

                        "Implementation-Version" to project.version,
                        "Implementation-URL" to "https://$repoUrl",
                        "Build-Time" to ZonedDateTime.now().format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
                    ) + getVendorIfAvailable(extension) + getImplementationKotlinVersionIfAvailable(project)
                )
                listOf("LICENSE.txt", "LICENSE", "LICENSE.md", "LICENSE.rst").forEach { fileName ->
                    val licenseFile = project.file("${project.rootProject.projectDir}/$fileName")
                    if (licenseFile.exists()) this@apply.from(licenseFile)
                }
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
        return version ?: (project.plugins.let<PluginContainer, Plugin<*>?> {
            // TODO 5.0.0 drop once we no longer support the old kotlin plugins and old gradle version
            it.findPlugin("kotlin") ?: it.findPlugin("kotlin2js") ?: it.findPlugin("kotlin-platform-jvm")
            ?: it.findPlugin("kotlin-platform-js") ?: it.findPlugin("kotlin-common")
            ?: it.findPlugin("org.jetbrains.kotlin.multiplatform") ?: it.findPlugin("org.jetbrains.kotlin.jvm")
            ?: it.findPlugin("org.jetbrains.kotlin.js")
        }?.let {
            val value = it::class.memberProperties.find { property ->
                property.name == "kotlinPluginVersion"
            }?.call(it)
            (value as? CharSequence)?.toString()
        })
    }
}
