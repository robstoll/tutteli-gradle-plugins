package ch.tutteli.gradle.plugins.junitjacoco

import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.kotlin.dsl.listProperty

open class JunitJacocoPluginExtension(project: Project) {
    val allowedTestTasksWithoutTests: ListProperty<String> = project.objects.listProperty()
    val additionalProjectSources: ListProperty<Project> = project.objects.listProperty()

    init {
        allowedTestTasksWithoutTests.convention(listOf("jsBrowserTest"))
        additionalProjectSources.convention(emptyList())
    }
}
