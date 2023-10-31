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

val generated = project.files("src/main/generated/kotlin/ch/tutteli/gradle/plugins/junitjacoco/generated")

kotlin {
    sourceSets {
        main {
            kotlin.srcDir(generated)
        }
    }
}

val generateDependencyVersions = tasks.register("generateHardCodedDependencies") {
    group = "build"
    description = "dependency version as code"

    doLast {
        mkdir(generated.asPath)
        File(generated.asPath, "Dependencies.kt").writeText(
            """
            package ch.tutteli.gradle.plugins.junitjacoco.generated

            object Dependencies{
                const val jacocoToolsVersion = "${libs.versions.jacocoTool.get()}"
            }
        """.trimIndent()
        )
    }
}
project.files(generated).builtBy(generateDependencyVersions)
tasks.withType<KotlinCompile>().configureEach {
    dependsOn(generateDependencyVersions)
}
afterEvaluate {
    tasks.named<Jar>("sourcesJar").configure {
        dependsOn(generateDependencyVersions)
    }
}
