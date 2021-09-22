package ch.tutteli.gradle.plugins.dokka

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

open class DokkaPluginExtension(project: Project) {
    val repoUrl: Property<String> = project.objects.property()
    val githubUser: Property<String> = project.objects.property()
    /**
     * true: kdoc included in docs/kdoc without versioning vs false: gh-pages branch with version/kdoc/
     */
    val modeSimple: Property<Boolean> = project.objects.property()

    init {
        if (isTutteliProject(project) || isTutteliProject(project.rootProject)) {
            githubUser.set("robstoll")
        }
        modeSimple.convention(true)
    }

    private fun isTutteliProject(project: Project): Boolean {
        return (project.group as? String)?.startsWith("ch.tutteli") ?: false
    }
}
