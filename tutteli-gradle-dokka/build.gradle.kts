plugins{
    `kotlin-dsl`
}
val plugin_id by extra("ch.tutteli.gradle.plugins.dokka")
val plugin_class by extra("ch.tutteli.gradle.plugins.dokka.DokkaPlugin")
val plugin_name by extra("Tutteli Dokka Plugin")
val plugin_description by extra("Applies JetBrain's dokka plugin, configures it by conventions and provides a javadocJar task.")
val plugin_tags by extra(listOf("dokka", "kotlin"))

val dokka_version: String by rootProject.extra
val kotlin_version: String by rootProject.extra

dependencies {
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:$dokka_version"){
        exclude("com.jetbrains.kotlin")
    }
    testImplementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
}
