import org.gradle.api.Project

fun Project.prefixedProject(name: String): Project = project(":${rootProject.name}-$name")
