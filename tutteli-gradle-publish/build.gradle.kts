plugins {
    `kotlin-dsl`
}
val pluginId by extra("ch.tutteli.gradle.plugins.publish")
val pluginClass by extra("ch.tutteli.gradle.plugins.publish.PublishPlugin")
val pluginName by extra("Tutteli Publish Plugin")
val pluginDescription by extra("Applies maven-publish as well as io.github.gradle-nexus.publish-plugin and configures them according to tutteli\"s publish conventions.")
val pluginTags by extra(listOf("publish", "kotlin"))

repositories {
    gradlePluginPortal()
}

val mavenModelVersion: String by rootProject.extra
val kotlinVersion: String by rootProject.extra

dependencies {
    implementation("org.apache.maven:maven-model:$mavenModelVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
}
