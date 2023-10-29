import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("build-logic.published-gradle-plugin")
    groovy
}

gradlePlugin {
    plugins {
        register("junitjacoco") {
            id = "ch.tutteli.gradle.plugins.junitjacoco"
            displayName = "Tutteli Junit-Jacoco Plugin"
            description = "Sets up JaCoCo for the JUnit 5 Platform"
            tags.set(listOf("jacoco", "junit", "junit5"))
            implementationClass = "ch.tutteli.gradle.plugins.junitjacoco.JunitJacocoPlugin"
        }
    }
}


dependencies {
    implementation(libs.junit.jupiter.api)
}

val generateDependencyVersions = tasks.register("generateHardCodedDependencies") {
    group = "build"
    description = "dependency version as code"

    val generated = project.projectDir.resolve("src/main/kotlin/ch/tutteli/gradle/plugins/junitjacoco/generated")
    outputs.dir(generated)
    doLast {
        mkdir(generated)
        generated.resolve("Dependencies.kt").writeText(
            """
            package ch.tutteli.gradle.plugins.junitjacoco.generated

            object Dependencies{
                const val jacocoToolsVersion = "${libs.versions.jacocoTool.get()}"
            }
        """.trimIndent()
        )
    }
}
tasks.withType<KotlinCompile>().configureEach {
    dependsOn(generateDependencyVersions)
}

afterEvaluate {
    tasks.named("publishPluginJar").configure {
        dependsOn(generateDependencyVersions)
    }
}
