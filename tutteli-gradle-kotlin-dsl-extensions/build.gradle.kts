plugins {
    `kotlin-dsl`
}

//TODO rename variables, do no longer use unerscore once we use Kotlin everywhere
val plugin_id by extra("ch.tutteli.gradle.plugins.kotlin.dsl.extensions")
val plugin_class by extra("ch.tutteli.gradle.plugins.kotlin.dsl.extensions.KotlinDslExtensions")
val plugin_name by extra("Tutteli Kotlin DSL extensions Plugin")
val plugin_description by extra("Helps you to deal with gradle when using Kotlin")
val plugin_tags by extra(listOf("gradle", "kotlin-dsl"))
