[![Gradle Plugin Portal](https://img.shields.io/badge/gradle%20plugin-v2.0.0-blue.svg)](https://plugins.gradle.org/u/robstoll)
[![Apache license](https://img.shields.io/badge/license-Apache%202.0-brightgreen.svg)](http://opensource.org/licenses/Apache2.0)
[![Build Status Ubuntu](https://github.com/robstoll/tutteli-gradle-plugins/workflows/Ubuntu/badge.svg?event=push)](https://github.com/robstoll/tutteli-gradle-plugins/actions?query=workflow%3AUbuntu+branch%3Amaster)
[![Build Status Windows](https://github.com/robstoll/tutteli-gradle-plugins/workflows/Windows/badge.svg?event=push)](https://github.com/robstoll/tutteli-gradle-plugins/actions?query=workflow%3AWindows+branch%3Amaster)
[![Coverage](https://codecov.io/gh/robstoll/tutteli-gradle-plugins/branch/master/graph/badge.svg)](https://codecov.io/github/robstoll/tutteli-gradle-plugins/branch/master)

# Tutteli gradle plugin
A set of gradle plugins which provide utility tasks and functions which I often use in my projects.

*You want to use one of them as well?*

Sweet :smile: the following sections will cover a few features.
They are most probably not complete
(and maybe out-dated, bear with me, as far as I know I am the only one using them).

Please [open an issue](https://github.com/robstoll/tutteli-gradle-plugins/issues/new),
if you find a bug or need some help.

The following sections give brief information what the different plugins offer.

# ch.tutteli.gradle.plugins.project.utils [🔗](https://plugins.gradle.org/plugin/ch.tutteli.project.utils/2.0.0)
This plugin is the complement of the settings plugin and you will typically use it together. 
Yet, you apply it in your `build.gradle` instead of the `settings.gradle` and accordingly this plugin adds utility functions to `Project`.

Currently, it provides the following functions:
- `prefixedProject(name)` which is a shortcut for `project("${rootProject.name}-$name")`.
   You find an example in the [build.gradle of the spek plugin](https://github.com/robstoll/tutteli-gradle-plugins/tree/v2.0.0/tutteli-gradle-spek/build.gradle#L20).
- `createTestJarTask` creates a task named `testJar` which creates a jar containing your test binaries
- `createTestSourcesJarTask` creates a task named `testSourcesJar` which creates a jar containing your test sources

<!--
# ch.tutteli.gradle.plugins.dokka [🔗](https://plugins.gradle.org/plugin/ch.tutteli.dokka/2.0.0)

**Currently** no longer maintained

Applies the [dokka-plugin](https://github.com/Kotlin/dokka) and creates a `javadocJar` task which can be used for publishing.
Moreover it applies a [default configuration to dokka](https://github.com/robstoll/tutteli-gradle-plugins/tree/v2.0.0/tutteli-gradle-dokka/src/main/groovy/ch/tutteli/gradle/dokka/DokkaPluginExtension.groovy#L22)
and allows to add an `externalDocumentationLink` based on the given `githubUser` with the `ghPages` flag.
It exposes the `tutteliDokka` extension where you can define i.a. the `githubUser`.
 
See [DokkaPluginIntTest](https://github.com/robstoll/tutteli-gradle-plugins/tree/v2.0.0/tutteli-gradle-dokka/src/test/groovy/ch/tutteli/gradle/dokka/DokkaPluginIntTest.groovy#L112)
for an example.
-->

# ch.tutteli.gradle.plugins.junitjacoco [🔗](https://plugins.gradle.org/plugin/ch.tutteli.junitjacoco/2.0.0)
Applies the [junit-platform-gradle-plugin](https://junit.org/junit5/docs/current/user-guide/#running-tests-build-gradle)
as well as the [jacoco-plugin](https://docs.gradle.org/current/userguide/jacoco_plugin.html)
and binds jacoco to the `junitPlatformTest` task.

This plugin does not set up a junit engine and you need to define it yourself. 
Have a look at [build.gradle](https://github.com/robstoll/tutteli-gradle-plugins/tree/v2.0.0/build.gradle#L61)
for an example.
In case you should use Spek as your engine, then you might want to have a look at the `spek` plugin below.

# ch.tutteli.gradle.plugins.kotlin.module.info [🔗](https://plugins.gradle.org/plugin/ch.tutteli.kotlin.module.info/2.0.0)

In case the used jdk for gradle is JDK9 or newer and the user has defined `module-info.java` under `src/module` 
then it compiles it add adds it to the kotlin target classes.
This way the kotlin compiler verifies `requires` and the like and the `module-info.class` gets included in the jar when it is built.

# ch.tutteli.gradle.plugins.kotlin.utils [🔗](https://plugins.gradle.org/plugin/ch.tutteli.kotlin.utils/2.0.0)
Provides some utility functions to declare dependencies on kotlin projects, to configure projects as well as utility functions to exclude kotlin.
Requires that `kotlinutils.kotlinVersion` (property on the extension) is configured.

Following a list of functions it supports:
- declare dependencies on libs: `kotlinStdlib()`, `kotlinStdlibJs()`, `kotlinStdlibCommon()`, `kotlinReflect()`, `kotlinTestJs()`, `kotlinTestCommon()`, , `kotlinTestAnotationsCommon()`  
- exclude dependencies: `excludeKotlin`, `excludeKbox`, `excludeAtriumVerbs` (see example)
- configure projects: `configureCommonProjects`, `configureJsProjects`, `configureJvmProjects`
- `getCommonProjects()`, `getJsProjects()`, `getJvmProjects()`, `getProjectNameWithoutSuffix(project)`   

Moreover, it turns warnings into errors if one of the env variables `CI` or `WARN_AS_ERROR` is set to `true`.

You find an example in [KotlinUtilsPluginIntTest](https://github.com/robstoll/tutteli-gradle-plugins/tree/v2.0.0/tutteli-gradle-kotlin-utils/src/test/groovy/ch/tutteli/gradle/kotlin/KotlinUtilsPluginIntTest.groovy#L45).

# ch.tutteli.gradle.plugins.publish [🔗](https://plugins.gradle.org/plugin/ch.tutteli.publish/2.0.0)

Applies the `maven-publish` and `signing` plugin and 
configures them based on given license(s), a github user and a few other information.
It exposes the `tutteliPublish` extension which lets you specify those information and refine default conventions.
Have a look at the [example in the tests](https://github.com/robstoll/tutteli-gradle-plugins/tree/v2.0.0/tutteli-gradle-publish/src/test/groovy/ch/tutteli/gradle/publish/PublishPluginIntTest.groovy#L41)
for more information.

If not set, it automatically propagates `version` and `group` from `rootProject` to subprojects 
(`group` of subprojects are set to "" when plugin is applied, would default to `rootProject.name`).

If no MavenPublication is defined, then it creates one which:
- automatically uses `project.components.java` if available -- apply the `java` or `kotlin` plugin (or similar) first.
- includes all Jar Tasks into the publication

Regardless if there was one or several existing MavenPublications or one was created by the plugin.
All Jar Tasks are modified in a way that they include the LICENSE(.txt) file located in the root of the rootProject
and augments the manifest file with information such as Build-Time, Kotlin-version used etc.

Last but not least, it augments the pom-file with license, developer and scm information (can be configured via tge `tutteliPublish` extension)

The conventions:
- Apache 2.0 is used as default license
- project.group, project.description and project.version is used in publishing
- gpg passphrase/keyRing and keyId can either be provided by a property (if you do, gradle.properties make sense) or by System.env with the following names:

    |       prop      |         env        |
    |-----------------|--------------------|
    | gpgPassphrase   | GPG_PASSPHRASE     |
    | gpgKeyRing      | GPG_KEY_RING       | 
    | gpgKeyId        | GPG_KEY_ID         | 
- The private gpg key can also be provided via GPG_SIGNING_KEY instead of pointing to a file via gpgKeyRing    

# ch.tutteli.gradle.plugins.spek [🔗](https://plugins.gradle.org/plugin/ch.tutteli.spek/2.0.0)
Applies the junitjacoco plugin (which itself applies the junit and jacoco plugin, see two sections above) 
and sets up [Spek](http://spekframework.org/) as junit engine.
Requires that a JVM compliant kotlin plugin is applied first.
Moreover, it adds `mavenCentral()` to the repositories and sets up kotlin dependencies:
kotlin-stdlib as implementation and kotlin-reflect as testImplementation dependency -- kotlin-reflect is required by spek.

# License
All tutteli gradle plugins are licensed under [Apache 2.0](http://opensource.org/licenses/Apache2.0).
