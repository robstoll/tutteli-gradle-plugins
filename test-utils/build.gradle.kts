val junitJupiterVersion: String by rootProject.extra

dependencies {
    implementation(gradleApi())
    implementation(gradleTestKit())
    implementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
}
