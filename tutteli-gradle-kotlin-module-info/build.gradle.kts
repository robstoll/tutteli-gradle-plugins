plugins {
    `kotlin-dsl`
}
val plugin_id by extra("ch.tutteli.gradle.plugins.kotlin.module.info")
val plugin_class by extra("ch.tutteli.gradle.plugins.kotlin.module.info.ModuleInfoPlugin")
val plugin_name by extra("Tutteli Kotlin module-info.java Plugin")
val plugin_description by extra("Provides a way to use module-info.java in kotlin projects.")
val plugin_tags by extra(listOf("kotlin", "module-info", "jigsaw"))
