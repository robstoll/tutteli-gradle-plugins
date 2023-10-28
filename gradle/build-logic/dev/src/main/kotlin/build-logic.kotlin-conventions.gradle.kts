import gradle.kotlin.dsl.accessors._81cadab438125fe32232621fe97f8fea.testImplementation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // this plugin sets inter alia toolchain and source/targetCompatibility
    // but also applies common plugins such as gradle-convention, build-params
    id("build-logic.java")
}

tasks.configureEach<KotlinCompile> {
    kotlinOptions {
        jvmTarget = buildParameters.defaultJdkVersion
    }
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}
