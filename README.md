[![Gradle Plugin Portal](https://img.shields.io/badge/gradle%20plugin-v0.10.0-blue.svg)](https://plugins.gradle.org/u/robstoll)
[![Apache license](https://img.shields.io/badge/license-Apache%202.0-brightgreen.svg)](http://opensource.org/licenses/Apache2.0)
[![Build Status Travis](https://travis-ci.org/robstoll/tutteli-gradle-plugins.svg?branch=master)](https://travis-ci.org/robstoll/tutteli-gradle-plugins/branches)
[![Build Status AppVeyor](https://ci.appveyor.com/api/projects/status/bv5e7rhsjko5mqy4/branch/master/?svg=true)](https://ci.appveyor.com/project/robstoll/tutteli-gradle-plugins/branch/master)
[![Coverage](https://codecov.io/gh/robstoll/tutteli-gradle-plugins/branch/master/graph/badge.svg)](https://codecov.io/github/robstoll/tutteli-gradle-plugins/branch/master)

# Tutteli gradle plugin
A set of gradle plugins which provide utility tasks and functions which I often use in my projects.

*You want to use one of them as well?*

Sweet :smile: the following sections will cover a few features.
They are most probably not complete
(and maybe out-dated, bear with me, as far as I know I am the only one using them).

Please [open an issue](https://github.com/robstoll/tutteli-gradle-plugins/issues/new),
if you find a bug or need some help.

The following sections give a brief information what the different plugins offer.

# ch.tutteli.settings [🔗](https://plugins.gradle.org/plugin/ch.tutteli.settings/0.10.0)
Provides utility functions to include projects (in a project setup where you have multiple subprojects).
Is especially useful if you apply the naming convention that all modules start with the name of the `rootProject`.

It supports three styles:
- [Extension Object paired with property/methodMissing](https://github.com/robstoll/tutteli-gradle-plugins/tree/master/tutteli-gradle-settings/src/test/groovy/ch/tutteli/gradle/settings/SettingsUtilPluginIntTest.groovy#L29)
- [Extension Object with method calls](https://github.com/robstoll/tutteli-gradle-plugins/tree/master/tutteli-gradle-settings/src/test/groovy/ch/tutteli/gradle/settings/SettingsUtilPluginIntTest.groovy#L94)
- [simply functions](https://github.com/robstoll/tutteli-gradle-plugins/tree/master/tutteli-gradle-settings/src/test/groovy/ch/tutteli/gradle/settings/SettingsUtilPluginIntTest.groovy#L175)

It also provides the helper function `kotlinJvmJs` to ease the inclusion of kotlin multi-platform projects.

# ch.tutteli.project.utils [🔗](https://plugins.gradle.org/plugin/ch.tutteli.project.utils/0.10.0)
This plugin is the complement of the settings plugin and you will typically use it together. 
Yet, you apply it in your `build.gradle` instead of the `settings.gradle` and accordingly this plugin adds utility functions to `Project`.

Currently, it provides just one function named `prefixedProject(name)` which is a shortcut for `project("${rootProject.name}-$name")`.
You find an example in the [build.gradle of the spek plugin](https://github.com/robstoll/tutteli-gradle-plugins/tree/master/tutteli-gradle-spek/build.gradle#L20).

# ch.tutteli.dokka [🔗](https://plugins.gradle.org/plugin/ch.tutteli.dokka/0.10.0)
Applies the [dokka-plugin](https://github.com/Kotlin/dokka) and creates a `javadocJar` task which can be used for publishing.
Moreover it applies a [default configuration to dokka](https://github.com/robstoll/tutteli-gradle-plugins/tree/master/tutteli-gradle-dokka/src/main/groovy/ch/tutteli/gradle/dokka/DokkaPluginExtension.groovy#L22)
and allows to add an `externalDocumentationLink` based on the given `githubUser` with the `ghPages` flag. 
See [DokkaPluginIntTest](https://github.com/robstoll/tutteli-gradle-plugins/tree/master/tutteli-gradle-dokka/src/test/groovy/ch/tutteli/gradle/dokka/DokkaPluginIntTest.groovy#L112)
for an example.

# ch.tutteli.junitjacoco [🔗](https://plugins.gradle.org/plugin/ch.tutteli.junitjacoco/0.10.0)
Applies the [junit-platform-gradle-plugin](https://junit.org/junit5/docs/current/user-guide/#running-tests-build-gradle)
as well as the [jacoco-plugin](https://docs.gradle.org/current/userguide/jacoco_plugin.html)
and binds jacoco to the `junitPlatformTest` task.

This plugin does not set up a junit engine and you need to define it yourself. 
Have a look at [build.gradle](https://github.com/robstoll/tutteli-gradle-plugins/tree/master/build.gradle#L61)
for an example.
In case you should use Spek as your engine, then you might want to have a look at the `spek` plugin below.

# ch.tutteli.kotlin.utils [🔗](https://plugins.gradle.org/plugin/ch.tutteli.kotlin.utils/0.10.0)
Provides some utility functions to declare dependencies on kotlin projects, configure projects as well as utility functions to exclude kotlin.
Requires that `kotlinutils.kotlinVersion` (property on the extension) is configured.

Following a list of functions it supports:
- declare dependencies on libs: `kotlinStdlib()`, `kotlinStdlibJs()`, `kotlinStdlibCommon()`, `kotlinReflect()`  
- exclude dependencies: `withoutKotlin`, `withoutKbox`
- configure projects: `configureCommonProjects`, `configureJsProjects`   

You find an example in [KotlinUtilsPluginIntTest](https://github.com/robstoll/tutteli-gradle-plugins/tree/master/tutteli-gradle-kotlin-utils/src/test/groovy/ch/tutteli/gradle/kotlin/KotlinUtilsPluginIntTest.groovy#L31).

# ch.tutteli.spek [🔗](https://plugins.gradle.org/plugin/ch.tutteli.spek/0.10.0)
Applies the junitjacoco plugin (which itself applies the junit and jacoco plugin, see two sections above) 
and sets up [Spek](http://spekframework.org/) as junit engine.
Requires that a JVM compliant kotlin plugin is applied first.
Moreover, it adds `mavenCentral()` to the repositories and sets up kotlin dependencies:
kotlin-stdlib as compile and kotlin-reflect as testCompile dependency -- kotlin-reflect is required by spek.

# License
All tutteli gradle plugins are licensed under [Apache 2.0](http://opensource.org/licenses/Apache2.0).
