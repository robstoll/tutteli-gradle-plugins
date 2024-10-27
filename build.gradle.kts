buildscript {
    val version = "5.0.2"
    val previousVersion = "5.0.0"

    rootProject.version = version
    rootProject.group = "ch.tutteli"

    dependencies {
        classpath("ch.tutteli:tutteli-gradle-junitjacoco:$previousVersion")
    }
}

plugins {
    id("build-logic.root-build")
    // necessary in order that we can use sourceSets / implementation and such within a dependencies block
    id("java-library")
}

subprojects {
    group = rootProject.group
    version = rootProject.version
}

val pluginProjects = subprojects - project(":test-utils")

configure(pluginProjects) {

    apply(plugin = "ch.tutteli.gradle.plugins.junitjacoco")

    tasks.named<JacocoReport>("jacocoTestReport").configure {
        reports.html.required.set(true)
    }

    tasks.withType<Test>().configureEach {
        reports {
            html.outputLocation.set(file("${buildDir}/reports/junit"))
        }
    }
}


/*
Release & deploy
----------------
(assumes you have an alias named gr pointing to ./gradlew)
1. Update master:
    a) point to the tag
        1) update version of the badges in README (except for codecov)
        2) search for `tree/master` in README and replace it with `tree/vX.Y.Z`
    b) change `version` in build.gradle.kts to X.Y.Z (remove -SNAPSHOT)
    c) search for old version and replace by new with the *exception* of previous_version in build.gradle.kts
    c) commit & push (modified README.md and build.gradle.kts)
    d) git tag vX.Y.Z
    e) git push origin vX.Y.Z
2. publish plugins
   a) gr publishPlugins
3. create release on github


Prepare next dev cycle
-----------------------
1. change version in build.gradle to X.Y.Z-SNAPSHOT
2. change previous_version in build.gradle
3. point to master
   a) search for `tree/vX.Y.Z` in README and replace it with `tree/master`
4. commit & push changes

*/
