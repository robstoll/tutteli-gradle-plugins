package ch.tutteli.gradle.plugins.dokka

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.property

sealed interface DokkaDestination
object Docs : DokkaDestination
object GhPages : DokkaDestination

open class DokkaPluginExtension(project: Project) {
    val repoUrl: Property<String> = project.objects.property()
    val githubUser: Property<String> = project.objects.property()

    /**
     * true: kdoc included in docs/kdoc without versioning vs false: gh-pages branch with version/kdoc/
     */
    val writeTo: Property<DokkaDestination> = project.objects.property()

    init {
        if (isTutteliProject(project) || isTutteliProject(project.rootProject)) {
            githubUser.set("robstoll")
        }

        val rootExtension = if (project != project.rootProject) {
            project.rootProject.extensions.findByType<DokkaPluginExtension>()
        } else null

        if (rootExtension != null) {
            takeOverValueFromRoot(rootExtension.repoUrl, repoUrl)
            takeOverValueFromRoot(rootExtension.githubUser, githubUser)
        }
        writeTo.convention(Docs)
    }

    private fun <T> takeOverValueFromRoot(rootProperty: Property<T>, property: Property<T>) {
        if (rootProperty.isPresent) {
            property.set(rootProperty)
        }
    }

    private fun isTutteliProject(project: Project): Boolean {
        return (project.group as? CharSequence)?.startsWith("ch.tutteli") ?: false
    }
}
