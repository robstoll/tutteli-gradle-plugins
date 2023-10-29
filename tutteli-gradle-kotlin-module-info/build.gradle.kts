plugins {
    id("build-logic.published-gradle-plugin")
    groovy
}

gradlePlugin {
    plugins {
        register("module-info") {
            id = "ch.tutteli.gradle.plugins.kotlin.module.info"
            displayName = "Tutteli Kotlin module-info.java Plugin"
            description = "Provides a way to use module-info.java in kotlin projects."
            tags.set(listOf("kotlin", "module-info", "jigsaw"))
            implementationClass = "ch.tutteli.gradle.plugins.kotlin.module.info.ModuleInfoPlugin"
        }
    }
}
