plugins {
    id("build-logic.kotlin-dsl-gradle-plugin")
}

dependencies {
    api(projects.basics)
    api(projects.dev)

    api(buildLibs.publish)
}
