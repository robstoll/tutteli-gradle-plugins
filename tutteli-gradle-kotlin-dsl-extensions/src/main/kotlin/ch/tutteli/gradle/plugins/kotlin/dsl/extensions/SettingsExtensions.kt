package ch.tutteli.gradle.plugins.kotlin.dsl.extensions
import org.gradle.api.GradleException
import org.gradle.kotlin.dsl.KotlinSettingsScript

fun KotlinSettingsScript.include(subPath: String, projectName: String) {
    val dir = file("${rootProject.projectDir}/$subPath/$projectName")
    if (!dir.exists()) {
        throw GradleException("cannot include project $projectName as its projectDir $dir does not exist")
    }
    include(projectName)
    project(":$projectName").projectDir = dir
}
