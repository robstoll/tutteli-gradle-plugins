plugins {
    `kotlin-dsl`
}
val pluginId by extra("ch.tutteli.gradle.plugins.kotlin.module.info")
val pluginClass by extra("ch.tutteli.gradle.plugins.kotlin.module.info.ModuleInfoPlugin")
val pluginName by extra("Tutteli Kotlin module-info.java Plugin")
val pluginDescription by extra("Provides a way to use module-info.java in kotlin projects.")
val pluginTags by extra(listOf("kotlin", "module-info", "jigsaw"))
