import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("build-logic.published-gradle-plugin")
    groovy
}

gradlePlugin {
    plugins {
        register("junitjacoco") {

            id = "ch.tutteli.gradle.plugins.spek"
            displayName = "Tutteli Spek Plugin"
            description =
                "Applies the junitjacoco plugin and defines Spek as engine. Applies the kotlin plugin and sets up compile and test dependencies."
            tags.set(listOf("spek", "junit", "kotlin"))
            implementationClass = "ch.tutteli.gradle.plugins.spek.SpekPlugin"
        }
    }
}

dependencies {
    compileOnly(libs.kotlin)
    testImplementation(libs.kotlin)
    testImplementation(libs.junit.jupiter.params)
    implementation(prefixedProject("junitjacoco"))
}


val generated = project.files("src/main/generated/groovy/ch/tutteli/gradle/plugins/spek/generated")

val generateHardCodedDependencies = tasks.register("generateHardCodedDependencies") {
    group = "build"

    doLast {
        mkdir(generated.asPath)
        File(generated.asPath, "Dependencies.groovy").writeText(
            """
                package ch.tutteli.gradle.plugins.spek.generated
                class Dependencies {
                   public static final jacoco_toolsVersion = "${libs.versions.jacocoTool.get()}"
                   public static final junit_jupiter_version = "${libs.versions.junitJupiter.get()}"
                   public static final junit_platform_version = "${libs.versions.junitPlatform.get()}"
                   public static final spek_version = "${libs.versions.spek.get()}"
                }
                """.trimIndent()
        )
    }
}
project.files(generated).builtBy(generateHardCodedDependencies)
tasks.withType<GroovyCompile>().configureEach {
    dependsOn(generateHardCodedDependencies)
}
afterEvaluate {
    tasks.named<Jar>("sourcesJar").configure {
        dependsOn(generateHardCodedDependencies)
    }
}
sourceSets {
    main {
        groovy {
            srcDirs("src/main/generated/groovy")
        }
    }
}
