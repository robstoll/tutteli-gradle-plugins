import org.gradle.kotlin.dsl.support.expectedKotlinDslPluginsVersion

plugins {
    `kotlin-dsl`
}

allprojects {
    group = "ch.tutteli.gradle.build-logic.convention"
}

dependencies {
    // We use precompiled script plugins (== plugins written as src/kotlin/build-logic.*.gradle.kts files,
    // and we need to declare dependency on org.gradle.kotlin.kotlin-dsl:org.gradle.kotlin.kotlin-dsl.gradle.plugin
    // in order to be able to specify tasks.validatePlugins
    // See https://github.com/gradle/gradle/issues/17016 regarding expectedKotlinDslPluginsVersion
    implementation("org.gradle.kotlin.kotlin-dsl:org.gradle.kotlin.kotlin-dsl.gradle.plugin:$expectedKotlinDslPluginsVersion")
}
