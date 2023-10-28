plugins {
    id("build-logic.kotlin-jvm")
    groovy
}

dependencies {
    api(gradleApi())
    api(gradleTestKit())
    api(libs.junit.jupiter.api)
    api(libs.mockito)
    api(libs.json.path.assert)
    api(libs.atrium)
}
