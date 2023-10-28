plugins {
    id("build-logic.published-gradle-plugin")
    groovy
}

gradlePlugin {
    plugins {
        register("publish") {
            id = "ch.tutteli.gradle.plugins.publish"
            displayName = "Tutteli Publish Plugin"
            description = "Applies maven-publish and configures it according to tutteli\"s publish conventions."
            tags.set(listOf("publish", "kotlin"))
            implementationClass = "ch.tutteli.gradle.plugins.publish.PublishPlugin"
        }
    }
}


dependencies {
    implementation(libs.maven.model)
    compileOnly(libs.kotlin)
    testImplementation(libs.kotlin)
    compileOnly(libs.dokka.plugin) {
        exclude("com.jetbrains.kotlin")
    }
}
