[![Apache license](https://img.shields.io/badge/license-Apache%202.0-brightgreen.svg)](http://opensource.org/licenses/Apache2.0)
[![Build Status](https://travis-ci.org/robstoll/tutteli-gradle-plugins.svg?branch=master)](https://travis-ci.org/robstoll/tutteli-gradle-plugins/branches)
[![Coverage](https://codecov.io/github/robstoll/tutteli-gradle-plugins/coverage.svg?branch=master)](https://codecov.io/github/robstoll/tutteli-gradle-plugins?branch=master)

# Tutteli gradle plugin
A set of gradle plugins which provide utility tasks and functions which I often use in my projects.

*You want to use one of them as well?*

Sweet :smile: the following sections will cover a few features.
They are most probably not complete
(and maybe out-dated, bear with me, as far as I know I am the only one using them).

Please [open an issue](https://github.com/robstoll/tutteli-gradle-plugins/issues/new),
if you find a bug or need some help.

The following sections give a brief information what the different plugins offer.

# ch.tutteli.settings
Provides utility functions to include projects (in a multi-project setup).
Is especially useful if you apply the naming convention that all modules start with the name of the `rootProject`.

It supports three styles:
- [Extension Object paired with property/methodMissing](https://github.com/robstoll/tutteli-gradle-plugins/tree/master/tutteli-gradle-settings/src/test/groovy/ch/tutteli/gradle/settings/SettingsUtilPluginIntTest.groovy#L79)
- [Extension Object with method calls](https://github.com/robstoll/tutteli-gradle-plugins/tree/master/tutteli-gradle-settings/src/test/groovy/ch/tutteli/gradle/settings/SettingsUtilPluginIntTest.groovy#L123)
- [simply functions](https://github.com/robstoll/tutteli-gradle-plugins/tree/master/tutteli-gradle-settings/src/test/groovy/ch/tutteli/gradle/settings/SettingsUtilPluginIntTest.groovy#L177)

# ch.tutteli.jacoco
Applies the [junit-platform-gradle-plugin](https://junit.org/junit5/docs/current/user-guide/#running-tests-build-gradle)
as well as the [jacoco-plugin](https://docs.gradle.org/current/userguide/jacoco_plugin.html)
and binds jacoco to the `junitPlatformTest` task.

You need to specify the `classpath` for the junit-platform-gradle-plugin in the `buildscript` section 
and you need to specify the junit engine you want to use. 

Have a look at [build.gradle](https://github.com/robstoll/tutteli-gradle-plugins/tree/master/build.gradle#L51).

# License
All tutteli gradle plugins are published under [Apache 2.0](http://opensource.org/licenses/Apache2.0).
