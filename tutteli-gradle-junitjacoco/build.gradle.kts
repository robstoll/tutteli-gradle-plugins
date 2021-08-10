plugins{
    `kotlin-dsl`
}
val plugin_id by extra("ch.tutteli.gradle.plugins.junitjacoco")
val plugin_class by extra("ch.tutteli.gradle.plugins.junitjacoco.JunitJacocoPlugin")
val plugin_name by extra("Tutteli Jacoco Plugin")
val plugin_description by extra("Sets up JaCoCo for the JUnit 5 Platform")
val plugin_tags by extra(listOf("jacoco", "junit"))

val junit_jupiter_version: String by rootProject.extra

dependencies {
    implementation("org.junit.jupiter:junit-jupiter-api:$junit_jupiter_version")
}

val dep = tasks.register("generateHardCodedDependencies") {
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
                const val jacocoToolsVersion = "0.8.7"
            }
        """.trimIndent()
        )
    }
}
tasks.filterIsInstance<org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile>().forEach {
    it.dependsOn(dep)
}
afterEvaluate {
    tasks.named("publishPluginJar").configure {
        dependsOn(dep)
    }
}
