val pluginId by extra("ch.tutteli.gradle.plugins.spek")
val pluginClass by extra("ch.tutteli.gradle.plugins.spek.SpekPlugin")
val pluginName by extra("Tutteli Spek Plugin")
val pluginDescription by extra("Applies the junitjacoco plugin and defines Spek as engine. Applies the kotlin plugin and sets up compile and test dependencies.")
val pluginTags by extra(listOf("spek", "junit", "kotlin"))
val junitJupiterVersion: String by rootProject.extra

val kotlinVersion : String by rootProject.extra

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")

    /**
     * Short for `implementation project(":${rootProject.name}-junitjacoco")`
     */
    implementation(prefixedProject("junitjacoco"))
}
