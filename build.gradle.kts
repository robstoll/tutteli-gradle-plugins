import com.gradle.publish.PluginBundleExtension

val kotlinVersion by extra("1.5.21")
val dokkaVersion by extra("1.8.10")

val junitJupiterVersion by extra("5.9.2")
val junitPlatformVersion by extra("1.9.1")

val spekVersion by extra("2.0.15")
val mockitoVersion by extra("5.2.0")
val jsonPathAssertVersion by extra("2.7.0")
val mavenModelVersion by extra("3.8.7")
val jacocoToolVersion by extra("0.8.8")

buildscript {
    val version = "4.7.0"
    val previousVersion = "4.5.1"

    rootProject.version = version
    rootProject.group = "ch.tutteli"

    val repo = "${rootProject.projectDir}/repo"
    rootProject.extra.set("repo", repo)

    rootProject.extra.set("error", false)

    if (rootProject.extra.get("error") == false) {
        fun includeLocalRepo(pluginName: String, version: String) {
            val repoDir = file(repo)
            val propName = "plugin_${pluginName}_exists"
            rootProject.extra.set(propName, false)
            if (repoDir.exists()) {
                rootProject.extra.set(
                    propName,
                    file("${repoDir.absolutePath}/ch/tutteli/${rootProject.name}-$pluginName/$version").exists()
                )
                if (rootProject.extra.get(propName) == true) {
                    rootProject.buildscript {
                        repositories {
                            // for local development
                            maven(url = uri(repo))
                        }
                        dependencies {
                            classpath("ch.tutteli:tutteli-gradle-$pluginName:$version")
                        }
                    }
                } else {
                    throw IllegalStateException("local repo exists but not the corresponding version of $pluginName, delete the repo manually")
                }
            }
        }

        apply(from = "${rootProject.projectDir}/gradle/scripts/localRepo.gradle")
        includeLocalRepo("junitjacoco", version)
        includeLocalRepo("project-utils", version)
    }

    fun setUpDependency(pluginName: String) {
        if (rootProject.extra.get("error") == false || rootProject.extra.get("plugin_${pluginName}_exists") == false) {
            dependencies {
                classpath("ch.tutteli:tutteli-gradle-$pluginName:$previousVersion")
            }
        }
    }
    setUpDependency("junitjacoco")
    setUpDependency("project-utils")
}




plugins {
    // necessary in order that we can use sourceSets / implementation and such within a dependencies block
    id("java-library")
    //TODO 5.0.0 update to 1.1.0
    id("com.gradle.plugin-publish") version "0.21.0" apply false
}

configurations {
    register("renovate")
}

//TODO 5.0.0 remove if renovate is still able to update the dependencies
//dependencies {
//    // helps renovate to recognise versions which it should update
//    dependencies {
//        add("renovate", "org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
//        add("renovate", "org.junit.platform:junit-platform-console:$junitPlatformVersion")
//        add("renovate", "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
//        add("renovate", "org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")
//        add("renovate", "ch.tutteli.spek:tutteli-spek-extensions:$spekVersion")
//        add("renovate", "org.jacoco:jacoco-maven-plugin:$jacocoToolVersion")
//        add("renovate", "org.apache.maven:maven-model:$mavenModelVersion")
//    }
//}

subprojects {
    apply(plugin = "groovy")
    version = rootProject.version
    group = rootProject.group

    with(the<JavaPluginExtension>()) {
        // TODO change to JDK11 with 5.0.0
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    repositories {
        mavenCentral()
    }
}

val pluginProjects = subprojects - project(":test-utils")
configure(pluginProjects) {
    apply(plugin = "java-gradle-plugin")
    apply(plugin = "com.gradle.plugin-publish")
}

configure(pluginProjects) {
    apply(plugin = "ch.tutteli.gradle.plugins.project.utils")
    apply(plugin = "ch.tutteli.gradle.plugins.junitjacoco")

    tasks.named<JacocoReport>("jacocoTestReport").configure {
        reports.html.required.set(true)
    }

    tasks.withType<Test> {
        reports {
            html.outputLocation.set(file("${buildDir}/reports/junit"))
        }
    }
}

configure(pluginProjects) {
    val subproject = this

    val createClasspathManifest = tasks.register("createClasspathManifest") {
        val outputDir = file("${buildDir}/${name}")

        inputs.files(sourceSets.main.get().runtimeClasspath)
        outputs.dir(outputDir)

        doLast {
            outputDir.mkdirs()
            file("${outputDir}/plugin-classpath.txt").writeText(sourceSets.main.get().runtimeClasspath.joinToString("\n"))
        }
    }
    tasks.named("test") {
        dependsOn(createClasspathManifest)
    }

    dependencies {
        testImplementation(project(":test-utils")) {
            exclude(group = "org.codehaus.groovy")
        }
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")

        testRuntimeOnly(files(createClasspathManifest)) //required for tests
        testImplementation("org.mockito:mockito-core:$mockitoVersion")
        testImplementation("com.jayway.jsonpath:json-path-assert:$jsonPathAssertVersion")
    }

    afterEvaluate {

        with(the<GradlePluginDevelopmentExtension>()) {
            plugins {
                register("tutteliPlugin") {
                    val pluginId: String by subproject.extra
                    val pluginClass: String by subproject.extra
                    id = pluginId
                    implementationClass = pluginClass
                }
            }
        }

        with(the<PluginBundleExtension>()) {
            website = "https://github.com/robstoll/tutteli-gradle-plugins"
            vcsUrl = "https://github.com/robstoll/tutteli-gradle-plugins.git"

            plugins {
                named("tutteliPlugin") {
                    val pluginId: String by subproject.extra
                    val pluginName: String by subproject.extra
                    val pluginDescription: String by subproject.extra
                    val pluginTags: List<String> by subproject.extra
                    id = pluginId
                    displayName = pluginName
                    description = pluginDescription
                    tags = pluginTags
                }
            }

            mavenCoordinates {
                groupId = subproject.group as String
            }
        }
    }

    apply(plugin = "maven-publish")
    tasks.register<Copy>("repo") {
        val outputJar = "${rootProject.extra.get("repo")}/ch/tutteli/${project.name}/${project.version}/"
        outputs.dir(outputJar)
        from(tasks.named("jar"))
        from(tasks.matching { it.name == "generatePomFileForPluginMavenPublication" })
        rename("pom-default.xml", "${project.name}-${project.version}.pom")
        into(outputJar)
    }

}
tasks.register<Delete>("removeRepo") {
    delete(rootProject.extra.get("repo"))
}

listOf("spek").forEach { projectName ->
    val subproject = project(":tutteli-gradle-$projectName")
    val generateHardCodedDependencies = subproject.tasks.register("generateHardCodedDependencies") {
        group = "build"
        doLast {
            val folder =
                File("${subproject.projectDir}/src/main/groovy/ch/tutteli/gradle/plugins/$projectName/generated")
            mkdir(folder)
            File(folder, "Dependencies.groovy").writeText(
                """
                package ch.tutteli.gradle.plugins.${projectName}.generated
                class Dependencies {
                   public static final jacoco_toolsVersion = "$jacocoToolVersion"
                   public static final junit_jupiter_version = "$junitJupiterVersion"
                   public static final junit_platform_version = "$junitPlatformVersion"
                   public static final spek_version = "$spekVersion"
                }
                """.trimIndent()
            )
        }
    }
    subproject.tasks.named("compileGroovy").configure {
        dependsOn(generateHardCodedDependencies)
    }
}


/*
Release & deploy
----------------
(assumes you have an alias named gr pointing to ./gradlew)
1. gr removeRepo
2. Update master:
    a) point to the tag
        1) update version of the badges in README (except for codecov)
        2) search for `tree/master` in README and replace it with `tree/vX.Y.Z`
    b) change `version` in build.gradle to X.Y.Z (remove -SNAPSHOT)
    c) search for old version and replace by new with the *exception* of previous_version in build.gradle
    c) commit & push (modified README.md and build.gradle.kts)
    d) git tag vX.Y.Z
    e) git push origin vX.Y.Z
3. publish plugins
   a) gr publishPlugins
4. create release on github


Prepare next dev cycle
-----------------------
1. change version in build.gradle to X.Y.Z-SNAPSHOT
2. change previous_version in build.gradle
3. point to master
   a) search for `tree/vX.Y.Z` in README and replace it with `tree/master`
4. commit & push changes

*/
