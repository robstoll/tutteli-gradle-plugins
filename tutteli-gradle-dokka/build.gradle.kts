plugins {
    id("build-logic.published-gradle-plugin")
    groovy
}

gradlePlugin {
    plugins {
        register("dokka") {
            id = "ch.tutteli.gradle.plugins.dokka"
            displayName = "Tutteli Dokka Plugin"
            description = "Applies JetBrain's dokka plugin, configures it by conventions"
            tags.set(listOf("dokka", "kotlin"))
            implementationClass = "ch.tutteli.gradle.plugins.dokka.DokkaPlugin"
        }
    }
}

dependencies {
    implementation(libs.dokka.plugin) {
        exclude("com.jetbrains.kotlin")
    }
    //TODO 6.0.0 activate, see libs.version.toml for more info
//    implementation(libs.kbox)
    testImplementation(libs.kotlin)
}
