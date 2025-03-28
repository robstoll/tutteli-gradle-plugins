[![Gradle Plugin Portal](https://img.shields.io/badge/gradle%20plugin-v5.0.3-blue.svg)](https://plugins.gradle.org/u/robstoll)
[![Apache license](https://img.shields.io/badge/license-Apache%202.0-brightgreen.svg)](http://opensource.org/licenses/Apache2.0)
[![Build Status Ubuntu](https://github.com/robstoll/tutteli-gradle-plugins/workflows/Ubuntu/badge.svg?event=push)](https://github.com/robstoll/tutteli-gradle-plugins/actions?query=workflow%3AUbuntu+branch%3Amain)
[![Build Status Windows](https://github.com/robstoll/tutteli-gradle-plugins/workflows/Windows/badge.svg?event=push)](https://github.com/robstoll/tutteli-gradle-plugins/actions?query=workflow%3AWindows+branch%3Amain)
[![Coverage](https://codecov.io/gh/robstoll/tutteli-gradle-plugins/branch/main/graph/badge.svg)](https://codecov.io/github/robstoll/tutteli-gradle-plugins/branch/main)

# Tutteli gradle plugin
A set of gradle plugins which provide utility tasks and functions which I often use in my projects.

*You want to use one of them as well?*

Sweet :smile: the following sections will cover a few features.
They are most probably not complete
(and maybe out-dated, bear with me, as far as I know I am the only one using them).

Please [open an issue](https://github.com/robstoll/tutteli-gradle-plugins/issues/new),
if you find a bug or need some help.

The following sections give brief information what the different plugins offer.

# ch.tutteli.gradle.plugins.dokka [🔗](https://plugins.gradle.org/plugin/ch.tutteli.dokka/5.0.3)

Applies the [dokka-plugin](https://github.com/Kotlin/dokka) and defines a `sourceLink` per `dokkaSourceSet`.
If the project version follows the pattern x.y.z, then an `externalDocumentationLink` per `dokkaSourceSet` is defined in addition.
The url used for the `sourceLink` and the `externalDocumentationLink` is based on a given githubUser.
Last but not least, it automatically configures dokka to look in test folder for *Samples.kt files for samples linked
in KDoc.

# ch.tutteli.gradle.plugins.junitjacoco [🔗](https://plugins.gradle.org/plugin/ch.tutteli.junitjacoco/5.0.3)
Applies the [junit-platform-gradle-plugin](https://junit.org/junit5/docs/current/user-guide/#running-tests-build-gradle)
as well as the [jacoco-plugin](https://docs.gradle.org/current/userguide/jacoco_plugin.html)
and applies some default configuration.

This plugin does not set up a junit engine and you need to define it yourself.
Have a look at [build.gradle](https://github.com/robstoll/tutteli-gradle-plugins/tree/v5.0.3/build.gradle#L61)
for an example.

# ch.tutteli.gradle.plugins.kotlin.module.info [🔗](https://plugins.gradle.org/plugin/ch.tutteli.gradle.plugins.kotlin.module.info/5.0.3)

Intended to be used in a kotlin project where either module-info.java is the single java source file or where >= jdk 11 is used.
It sets up compileJava accordingly and configures JavaCompile tasks to use jdk 11 for `sourceCompatibility`/`targetCompatibility` if not already set or higher.

Per default, it reads the module name (which is used for `--patch-module`) from the defined module-info.java.
You can speed up this process (in case you have many java files) by defining `moduleName` on `project.extra`.

# ch.tutteli.gradle.plugins.publish [🔗](https://plugins.gradle.org/plugin/ch.tutteli.gradle.plugins.publish/5.0.3)

Applies the `maven-publish` and `signing` plugin and 
configures them based on given license(s), a github user and a few other information.
It exposes the `tutteliPublish` extension which lets you specify those information and refine default conventions.
Have a look at the [example in the tests](https://github.com/robstoll/tutteli-gradle-plugins/tree/v5.0.3/tutteli-gradle-publish/src/test/groovy/ch/tutteli/gradle/publish/PublishPluginIntTest.groovy#L41)
for more information.

If not set, it automatically propagates `version` and `group` from `rootProject` to subprojects 
(`group` of subprojects are set to "" when plugin is applied, would default to `rootProject.name`).

If no MavenPublication is defined, then it creates one which:
- automatically uses `project.components.java` if available.
- includes all Jar Tasks into the publication (you can use an `artifactFilter` to exclude some)

Regardless if there was one or several existing MavenPublications or one was created by the plugin.
All Jar Tasks are modified in a way that they include the LICENSE(.txt) file located in the root of the rootProject
and augments the manifest file with information such as Vendor, Kotlin-version used etc.

Last but not least, it augments the pom-file with license, developer and scm information (can be configured via the `tutteliPublish` extension)

The conventions:
- Apache 2.0 is used as default license
- project.group, project.description and project.version is used in publishing
- configures `singing` to `useGpgCmd`

# ch.tutteli.gradle.plugins.spek [🔗](https://plugins.gradle.org/plugin/ch.tutteli.spek/5.0.3)
-> will most likely be removed with 6.0.0 (spek is no longer a reliable test runner IMO regarding maintenance)

Applies the junitjacoco plugin (which itself applies the junit and jacoco plugin, see two sections above) 
and sets up [Spek](http://spekframework.org/) as junit engine.
Requires that a JVM compliant kotlin plugin is applied first.
Moreover, it adds `mavenCentral()` to the repositories and sets up kotlin dependencies:
kotlin-stdlib as implementation and kotlin-reflect as testImplementation dependency -- kotlin-reflect is required by spek.

# License
All tutteli gradle plugins are licensed under [Apache 2.0](http://opensource.org/licenses/Apache2.0).
