import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins{
    `kotlin-dsl`
}
val pluginId by extra("ch.tutteli.gradle.plugins.junitjacoco")
val pluginClass by extra("ch.tutteli.gradle.plugins.junitjacoco.JunitJacocoPlugin")
val pluginName by extra("Tutteli Jacoco Plugin")
val pluginDescription by extra("Sets up JaCoCo for the JUnit 5 Platform")
val pluginTags by extra(listOf("jacoco", "junit"))

val junitJupiterVersion: String by rootProject.extra
val jacocoToolVersion: String by rootProject.extra

dependencies {
    implementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
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
                const val jacocoToolsVersion = "$jacocoToolVersion"
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
