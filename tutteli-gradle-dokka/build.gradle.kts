plugins{
    `kotlin-dsl`
}
val pluginId by extra("ch.tutteli.gradle.plugins.dokka")
val pluginClass by extra("ch.tutteli.gradle.plugins.dokka.DokkaPlugin")
val pluginName by extra("Tutteli Dokka Plugin")
val pluginDescription by extra("Applies JetBrain's dokka plugin, configures it by conventions and provides a javadocJar task.")
val pluginTags by extra(listOf("dokka", "kotlin"))

val dokkaVersion: String by rootProject.extra
val kotlinVersion: String by rootProject.extra

dependencies {
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion"){
        exclude("com.jetbrains.kotlin")
    }
    testImplementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
}
