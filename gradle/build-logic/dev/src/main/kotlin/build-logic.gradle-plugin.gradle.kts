plugins {
    id("org.gradle.kotlin.kotlin-dsl") // this is 'kotlin-dsl' without version
    id("java-gradle-plugin")
    id("build-logic.kotlin-conventions")
}


gradlePlugin {
    website.set("https://github.com/robstoll/tutteli-gradle-plugins")
    vcsUrl.set("https://github.com/robstoll/tutteli-gradle-plugins.git")
}
//
//val createClasspathManifest = tasks.register("createClasspathManifest") {
//    val outputDir = file("${buildDir}/${name}")
//
//    inputs.files(sourceSets.main.get().runtimeClasspath)
//    outputs.dir(outputDir)
//
//    doLast {
//        outputDir.mkdirs()
//        file("${outputDir}/plugin-classpath.txt").writeText(sourceSets.main.get().runtimeClasspath.joinToString("\n"))
//    }
//}
//tasks.named("test") {
//    dependsOn(createClasspathManifest)
//}

