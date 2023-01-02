plugins {
    `kotlin-dsl`
}
val pluginId by extra( "ch.tutteli.gradle.plugins.project.utils")
val pluginClass by extra( "ch.tutteli.gradle.plugins.project.UtilsPlugin")
val pluginName by extra( "Tutteli Project Utilities")
val pluginDescription by extra( "Adds some utility functions to project.ext such as prefixedProject.")
val pluginTags by extra( listOf("project", "utility"))
