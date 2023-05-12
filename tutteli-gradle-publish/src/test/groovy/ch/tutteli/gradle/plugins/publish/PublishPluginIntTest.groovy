package ch.tutteli.gradle.plugins.publish

import ch.tutteli.gradle.plugins.test.Asserts
import ch.tutteli.gradle.plugins.test.SettingsExtension
import ch.tutteli.gradle.plugins.test.SettingsExtensionObject
import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

import static ch.tutteli.gradle.plugins.test.Asserts.NL_INDENT
import static ch.tutteli.gradle.plugins.test.Asserts.assertContainsNotRegex
import static ch.tutteli.gradle.plugins.test.Asserts.assertContainsRegex
import static org.junit.jupiter.api.Assertions.*
import static org.junit.jupiter.api.Assumptions.assumeFalse
import static org.junit.jupiter.api.Assumptions.assumeFalse

@ExtendWith(SettingsExtension)
class PublishPluginIntTest {
    //TODO remove once we drop support for old MPP plugins
    def static final OLD_KOTLIN_VERSION = '1.3.71'
    def static final KOTLIN_VERSION = '1.5.21'
    def static final ATRIUM_VERSION = '0.14.0'

    @Test
    void smokeTestJava(SettingsExtensionObject settingsSetup) throws IOException {
        def version = '1.0.0'
        checkSmokeTestJava("smoke1", settingsSetup, version, null)
    }

    @Test
    void smokeTestJava_gradle6x(SettingsExtensionObject settingsSetup) throws IOException {
        def javaVersion = JavaVersion.toVersion(System.getProperty("java.version"))
        assumeFalse(javaVersion >= JavaVersion.VERSION_15)
        def version = '1.0.0'
        checkSmokeTestJava("smoke1", settingsSetup, version, "6.9.4")
    }


    @Test
    void smokeTestJavaWithSnapshot(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        def version = '1.0.0-SNAPSHOT'
        checkSmokeTestJava("smoke2", settingsSetup, version, null)
    }

    private static void checkSmokeTestJava(String projectName, SettingsExtensionObject settingsSetup, String version, String gradleVersion) {
        settingsSetup.settings << "rootProject.name='$projectName'"
        def githubUser = 'robstoll'
        def gpgPassphrase = 'bla'
        def gpgKeyId = 'A5875B96'
        def gpgKeyRing = 'keyring.gpg'
        def groupId = 'com.example'

        settingsSetup.gpgKeyRing << PublishPluginIntTest.class.getResourceAsStream('/test-tutteli-gradle-plugin.gpg')

        settingsSetup.buildGradle << """
        buildscript {
            dependencies {
                classpath files($settingsSetup.pluginClasspath)
            }
        }
        buildscript {
            ext {
                // required since we don't set the System.env variables.
                myGpgPassphrase = '$gpgPassphrase'
                myGpgKeyRing = '$gpgKeyRing'
                myGpgKeyId = '$gpgKeyId'
            }
        }

        apply plugin: 'java'
        apply plugin: 'ch.tutteli.gradle.plugins.publish'

        project.with {
            group = '$groupId'
            version = '$version'
            description = 'test project'
        }

        tutteliPublish {
            //minimal setup required for publish, all other things are only needed if not the default is used
            githubUser = '$githubUser'

            //different ways to override the default license
            resetLicenses 'EUPL-1.2'             // default distribution is 'repo'
            resetLicenses 'EUPL-1.2', 'manually'
            resetLicenses ch.tutteli.gradle.plugins.publish.StandardLicenses.EUPL_1_2
            resetLicenses ch.tutteli.gradle.plugins.publish.StandardLicenses.EUPL_1_2, 'manually'
            resetLicenses new ch.tutteli.gradle.plugins.publish.License(
                'Lic-1.2',
                'License 1.2',
                'https://license.com',
                'manually'
            )
            resetLicenses new ch.tutteli.gradle.plugins.publish.License(
                 'Lic-1.2',
                'License 1.2',
                'https://license.com',
                'repo'
            )

            // different ways to add additional licenses
            license 'Apache-2.0'
            license 'Apache-2.0', 'manually'
            license ch.tutteli.gradle.plugins.publish.StandardLicenses.APACHE_2_0
            license ch.tutteli.gradle.plugins.publish.StandardLicenses.APACHE_2_0, 'somethingElse'
            license(new ch.tutteli.gradle.plugins.publish.License(
                'Lic-1.2',
                'License 1.2',
                'https://license.com',
                'repo'
            ))

            // you can add multiple developers if required
            developer {
                id = 'robstoll'
                name = 'Robert Stoll'
                email = 'rstoll@tutteli.ch'
                url = 'tutteli.ch'
            }
            developer {
                id = 'robstoll_tutteli'
                name = 'Robert Stoll'
                email = 'rstoll@tutteli.ch'
                organization = 'tutteli'
                organizationUrl = 'tutteli.ch'
            }

            // will add Implementation-Vendor to all manifest files.
            manifestVendor = 'tutteli.ch'

            // you can customise the property and env variable names if they differ from the convention
            propNameGpgPassphrase = 'myGpgPassphrase'           // default is gpgPassphrase
            propNameGpgKeyId      = 'myGpgKeyId'                // default is gpgKeyId
            propNameGpgKeyRing     = 'myGpgKeyRing'             // default is gpgKeyRing
            envNameGpgPassphrase  = 'MY_GPG_PASSPHRASE'         // default is GPG_PASSPHRASE
            envNameGpgKeyId       = 'MY_GPG_KEY_ID'             // default is GPG_KEY_ID
            envNameGpgKeyRing     = 'MY_GPG_KEY_RING'           // default is GPG_KEY_RING
            envNameGpgSigningKey  = 'MY_GPG_SIGNING_KEY'        // default is GPG_SIGNING_KEY

            // you can also disable GPG signing (default is true)
            signWithGpg = false
            // yet, we will re-activate it for this test
            signWithGpg = true
        }

        ${publishingRepo()}
        ${taskPrintSigning()}
        """
        //act
        def builder = GradleRunner.create()
        if (gradleVersion != null) {
            builder = builder.withGradleVersion(gradleVersion)
        }
        def result = builder
            .withProjectDir(settingsSetup.tmp)
            .withArguments("publishAllPublicationsToMavenRepository", "printSigning", "--stacktrace")
            .build()

        //assert
        assertTrue(result.output.contains("Some licenses were duplicated. Please check if you made a mistake."), "should contain warning about duplicated licenses:\n$result.output")

        def releasePath = getReleasePath(settingsSetup, projectName, groupId, version)
        def (pom, _) = getPomInclFileNameAndAssertBasicPomProperties(releasePath, projectName, groupId, version, githubUser)

        assertContainsRegex(pom, "licenses", "<licenses>$NL_INDENT" +
            "<license>$NL_INDENT" +
            "<name>${StandardLicenses.APACHE_2_0.longName}</name>$NL_INDENT" +
            "<url>${StandardLicenses.APACHE_2_0.url}</url>$NL_INDENT" +
            "<distribution>manually</distribution>$NL_INDENT" +
            "</license>$NL_INDENT" +
            "<license>$NL_INDENT" +
            "<name>${StandardLicenses.APACHE_2_0.longName}</name>$NL_INDENT" +
            "<url>${StandardLicenses.APACHE_2_0.url}</url>$NL_INDENT" +
            "<distribution>repo</distribution>$NL_INDENT" +
            "</license>$NL_INDENT" +
            "<license>$NL_INDENT" +
            "<name>${StandardLicenses.APACHE_2_0.longName}</name>$NL_INDENT" +
            "<url>${StandardLicenses.APACHE_2_0.url}</url>$NL_INDENT" +
            "<distribution>somethingElse</distribution>$NL_INDENT" +
            "</license>$NL_INDENT" +
            "<license>$NL_INDENT" +
            "<name>License 1.2</name>$NL_INDENT" +
            "<url>https://license.com</url>$NL_INDENT" +
            "<distribution>repo</distribution>$NL_INDENT" +
            "</license>$NL_INDENT" +
            "</licenses>")

        assertContainsRegex(pom, "developers", "<developers>$NL_INDENT" +
            "<developer>$NL_INDENT" +
            "<id>robstoll</id>$NL_INDENT" +
            "<name>Robert Stoll</name>$NL_INDENT" +
            "<email>rstoll@tutteli.ch</email>$NL_INDENT" +
            "<url>tutteli.ch</url>$NL_INDENT" +
            "</developer>$NL_INDENT" +
            "<developer>$NL_INDENT" +
            "<id>robstoll_tutteli</id>$NL_INDENT" +
            "<name>Robert Stoll</name>$NL_INDENT" +
            "<email>rstoll@tutteli.ch</email>$NL_INDENT" +
            "<organization>tutteli</organization>$NL_INDENT" +
            "<organizationUrl>tutteli.ch</organizationUrl>$NL_INDENT" +
            "</developer>$NL_INDENT" +
            "</developers>"
        )

        assertModuleExists(releasePath, projectName, version)
        assertSigning(result, gpgPassphrase, gpgKeyId, gpgKeyRing)
    }

    @Test
    void smokeTest_GpgPassphraseMissing(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        def projectName = 'smoke-gpg'
        settingsSetup.settings << "rootProject.name='$projectName'"
        def version = '1.0.0'
        def githubUser = 'robstoll'

        settingsSetup.buildGradle << """
        buildscript {
            dependencies {
                classpath files($settingsSetup.pluginClasspath)
            }
        }

        apply plugin: 'java'
        apply plugin: 'ch.tutteli.gradle.plugins.publish'

        project.with {
            group = 'com.example'
            version = '$version'
            description = 'test project'
        }

        tutteliPublish {
            // minimal setup required for publish, all other things are only needed if not the default is used
            githubUser = '$githubUser'
            // gpg passphrase not defined via property or something
        }
        """
        // still able to generate a pom, the GPG-key is only used for publishing
        GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("tasks", "generatePom", "--stacktrace")
            .build()
        //assert
        def exception = assertThrows(UnexpectedBuildFailure) {
            GradleRunner.create()
                .withProjectDir(settingsSetup.tmp)
                .withArguments("validateBeforePublish", "--stacktrace")
                .build()
        }
        assertTrue(exception.message.contains("You need to define property with name gpgPassphrase or System.env variable with name GPG_PASSPHRASE"),
            "did not fail due to missing passphase\n$exception.message")
    }

    @Test
    void subproject(SettingsExtensionObject settingsSetup) throws IOException {
        checkSubproject(settingsSetup)
    }

    @Test
    void subproject_gradle6x(SettingsExtensionObject settingsSetup) throws IOException {
        def javaVersion = JavaVersion.toVersion(System.getProperty("java.version"))
        assumeFalse(javaVersion >= JavaVersion.VERSION_15)
        checkSubproject(settingsSetup, "6.9.4")
    }

    private void checkSubproject(SettingsExtensionObject settingsSetup, String gradleVersion = null) {
        //arrange
        def rootProjectName = 'rootProject'
        def subprojectName = "test-sub-jvm"
        def dependentName = 'dependent'
        settingsSetup.settings << """rootProject.name='$rootProjectName'
        include '$subprojectName'
        include '$dependentName'
        """
        def groupId = 'ch.tutteli'
        def version = '1.0.0'
        def githubUser = 'robstoll'
        def vendor = 'tutteli.ch'
        def gpgPassphrase = 'bla'
        def gpgKeyId = 'A5875B96'
        def gpgKeyRing = 'keyring.gpg'

        settingsSetup.gpgKeyRing << PublishPluginIntTest.class.getResourceAsStream('/test-tutteli-gradle-plugin.gpg')

        settingsSetup.buildGradle << """
        buildscript {
            repositories { maven { url "https://plugins.gradle.org/m2/" } }
            dependencies {
                classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:$OLD_KOTLIN_VERSION'
                classpath files($settingsSetup.pluginClasspath)
            }
            ext {
                // required since we don't set the System.env variables.
                gpgPassphrase = '$gpgPassphrase'
                gpgKeyRing = "\$rootProject.projectDir/$gpgKeyRing"
                gpgKeyId = '$gpgKeyId'
            }
        }

        project.with {
            group = '$groupId'
            version = '$version'
        }

        subprojects {
            it.description = 'test project'

            repositories {  mavenCentral(); }
            apply plugin: 'kotlin'

            task('testJar', type: Jar) {
                from sourceSets.test.output
                classifier = 'tests'
            }

           apply plugin: 'ch.tutteli.gradle.plugins.publish'

            // still included in publish, use the artifactFilter to exclude a jar as artifact
            task('testSourcesJar', type: Jar) {
                from sourceSets.test.allSource
                classifier = 'testsources'
            }

            task('testSourcesJarFiltered', type: Jar) {
                from sourceSets.test.allSource
                classifier = 'testsources-filtered'
            }

            tutteliPublish {
                resetLicenses 'EUPL-1.2'

                //already defined because it is a ch.tutteli project
                //githubUser = '$githubUser'
                //manifestVendor = $vendor // we don't have a manifestVendor, thus we reset it to null

                artifactFilter = { jar -> jar.name != 'testSourcesJarFiltered' } as kotlin.jvm.functions.Function1
            }
            ${publishingRepo()}
            ${taskPrintSigning()}
        }

        configure(project(':$dependentName')) {
            dependencies {
                compile rootProject.project(':$subprojectName')
            }
        }
        """
        File license = new File(settingsSetup.tmp, 'LICENSE.txt')
        license << "Copyright..."
        //act
        def builder = GradleRunner.create()
        if (gradleVersion != null) {
            builder = builder.withGradleVersion(gradleVersion)
        }
        def result = builder
            .withProjectDir(settingsSetup.tmp)
            .withArguments("publishAllPublicationsToMavenRepository", "printSigning", "--stacktrace")
            .build()

        //assert
        assertSigning(result, gpgPassphrase, gpgKeyId, "${settingsSetup.tmpPath.toRealPath()}/$gpgKeyRing")

        def dependentReleasePath = getReleasePath(settingsSetup, dependentName, groupId, version, dependentName)
        def (dependentPom, dependentPomName) = getPomInclFileNameAndAssertBasicPomProperties(dependentReleasePath, dependentName, groupId, version, githubUser, rootProjectName)
        assertContainsRegex(dependentPom, "licenses", "<licenses>$NL_INDENT" +
            "<license>$NL_INDENT" +
            "<name>${StandardLicenses.EUPL_1_2.longName}</name>$NL_INDENT" +
            "<url>${StandardLicenses.EUPL_1_2.url}</url>$NL_INDENT" +
            "<distribution>repo</distribution>$NL_INDENT" +
            "</license>$NL_INDENT" +
            "</licenses"
        )
        assertContainsRegex(dependentPom, "developers", "<developers>$NL_INDENT" +
            "<developer>$NL_INDENT" +
            "<id>robstoll</id>$NL_INDENT" +
            "<name>Robert Stoll</name>$NL_INDENT" +
            "<email>rstoll@tutteli.ch</email>$NL_INDENT" +
            "<url>https://tutteli.ch</url>$NL_INDENT" +
            "</developer>$NL_INDENT" +
            "</developers>")
        assertContainsRegex(dependentPom, "dependencies", "<dependencies>$NL_INDENT" +
            "<dependency>$NL_INDENT" +
            "<groupId>$groupId</groupId>$NL_INDENT" +
            "<artifactId>$subprojectName</artifactId>$NL_INDENT" +
            "<version>$version</version>$NL_INDENT" +
            "<scope>compile</scope>$NL_INDENT" +
            "</dependency>$NL_INDENT" +
            "<dependency>$NL_INDENT" +
            "<groupId>$groupId</groupId>$NL_INDENT" +
            "<artifactId>$subprojectName</artifactId>$NL_INDENT" +
            "<version>$version</version>$NL_INDENT" +
            "<scope>runtime</scope>$NL_INDENT" +
            "</dependency>$NL_INDENT" +
            "</dependencies>"
        )

        def repoUrl = "https://github.com/$githubUser/$rootProjectName"


        assertJarsWithLicenseAndManifest(dependentReleasePath, dependentName, version, repoUrl, vendor, OLD_KOTLIN_VERSION, dependentPomName,
            ".jar",
            "-sources.jar",
            "-tests.jar"
        )
        assertModuleExists(dependentReleasePath, dependentName, version)

        def subReleasePath = getReleasePath(settingsSetup, subprojectName, groupId, version, subprojectName)
        def (_, subPomName) = getPomInclFileNameAndAssertBasicPomProperties(subReleasePath, subprojectName, groupId, version, githubUser, rootProjectName)
        assertJarsWithLicenseAndManifest(subReleasePath, subprojectName, version, repoUrl, vendor, OLD_KOTLIN_VERSION, subPomName,
            ".jar",
            "-sources.jar",
            "-tests.jar"
        )
        assertModuleExists(subReleasePath, subprojectName, version)
    }

    @Test
    void withOldKotlinApplied(SettingsExtensionObject settingsSetup) throws IOException {
        def projectName = 'kotlin-jvm-old'
        settingsSetup.settings << "rootProject.name='$projectName'"
        def groupId = 'com.example'
        def version = '1.0.0'
        def githubUser = 'robstoll'
        def gpgPassphrase = 'bla'
        def gpgKeyId = 'A5875B96'
        def gpgKeyRing = 'keyring.gpg'

        settingsSetup.gpgKeyRing << PublishPluginIntTest.class.getResourceAsStream('/test-tutteli-gradle-plugin.gpg')

        settingsSetup.buildGradle << """
        ${settingsSetup.buildscriptWithKotlin(OLD_KOTLIN_VERSION)}
        buildscript {
            ext {
                // required since we don't set the System.env variables.
                gpgPassphrase = '$gpgPassphrase'
                gpgKeyRing = '$gpgKeyRing'
                gpgKeyId = '$gpgKeyId'
            }
        }
        repositories {
            mavenCentral()
        }

        apply plugin: 'kotlin'
       apply plugin: 'ch.tutteli.gradle.plugins.publish'

        project.with {
            group = '$groupId'
            version = '$version'
            description = 'test project'
        }
        tutteliPublish {
            //minimal setup required for local publish, all other things are only needed if not the default is used
            githubUser = '$githubUser'
        }
        ${publishingRepo()}
        ${taskPrintSigning()}
        """
        File license = new File(settingsSetup.tmp, 'LICENSE.txt')
        license << 'Copyright...'
        Path main = Files.createDirectories(settingsSetup.tmpPath.resolve('src').resolve('main'))
        Path resources = Files.createDirectory(main.resolve('resources'))
        File txt = new File(resources.toFile(), 'a.txt')
        txt << 'dummy'
        Path tutteli = Files.createDirectories(main.resolve('kotlin').resolve('ch').resolve('tutteli').resolve('atrium'))
        File kt = new File(tutteli.toFile(), 'a.kt')
        kt << 'package ch.tutteli.atrium'

        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("publishAllPublicationsToMavenRepository", "printSigning")
            .build()

        assertSigning(result, gpgPassphrase, gpgKeyId, gpgKeyRing)

        Asserts.assertTaskRunSuccessfully(result, ":$PublishPlugin.TASK_GENERATE_GRADLE_METADATA")
        Asserts.assertTaskRunSuccessfully(result, ":$PublishPlugin.TASK_GENERATE_POM")
        Asserts.assertTaskRunSuccessfully(result, ":signTutteliPublication")
        Asserts.assertTaskRunSuccessfully(result, ":publishTutteliPublicationToMavenRepository")

        def repoUrl = "https://github.com/$githubUser/$projectName"

        def releasePath = getReleasePath(settingsSetup, projectName, groupId, version)
        def (_, pomFileName) = getPomInclFileNameAndAssertBasicPomProperties(releasePath, projectName, groupId, version, githubUser)

        assertModuleExists(releasePath, projectName, version)
        assertJarsWithLicenseAndManifest(releasePath, projectName, version, repoUrl, null, OLD_KOTLIN_VERSION, pomFileName,
            ".jar",
            "-sources.jar",
        )
        new ZipFile(releasePath.resolve("$projectName-${version}.jar").toFile()).withCloseable { zipFile ->
            assertInZipFile(zipFile, 'a.txt')
        }
        new ZipFile(releasePath.resolve("$projectName-${version}-sources.jar").toFile()).withCloseable { zipFile ->
            assertInZipFile(zipFile, 'main/ch/tutteli/atrium/a.kt')
        }
    }

    @Test
    void withKotlinJvmApplied(SettingsExtensionObject settingsSetup) throws IOException {
        def projectName = 'kotlin-jvm'
        settingsSetup.settings << "rootProject.name='$projectName'"
        def groupId = 'com.example'
        def version = '1.0.0'
        def githubUser = 'robstoll'
        def gpgPassphrase = 'bla'
        def gpgKeyId = 'A5875B96'
        def gpgKeyRing = 'keyring.gpg'

        settingsSetup.gpgKeyRing << PublishPluginIntTest.class.getResourceAsStream('/test-tutteli-gradle-plugin.gpg')

        settingsSetup.buildGradle << """
        ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}
        buildscript {
            ext {
                // required since we don't set the System.env variables.
                gpgPassphrase = '$gpgPassphrase'
                gpgKeyRing = '$gpgKeyRing'
                gpgKeyId = '$gpgKeyId'
            }
        }
        repositories {
            mavenCentral()
        }

        apply plugin: 'org.jetbrains.kotlin.jvm'
       apply plugin: 'ch.tutteli.gradle.plugins.publish'

        project.with {
            group = '$groupId'
            version = '$version'
            description = 'test project'
        }
        tutteliPublish {
            // minimal setup required for local publish, all other things are only needed if not the default is used
            githubUser = '$githubUser'
        }
        dependencies {
            implementation 'ch.tutteli.atrium:atrium-fluent-en_GB:$ATRIUM_VERSION'
        }

        ${publishingRepo()}
        ${taskPrintSigning()}
        """
        File license = new File(settingsSetup.tmp, 'LICENSE.txt')
        license << 'Copyright...'
        Path main = Files.createDirectories(settingsSetup.tmpPath.resolve('src').resolve('main'))
        Path resources = Files.createDirectory(main.resolve('resources'))
        File txt = new File(resources.toFile(), 'a.txt')
        txt << 'dummy'
        Path tutteli = Files.createDirectories(main.resolve('kotlin').resolve('ch').resolve('tutteli').resolve('atrium'))
        File kt = new File(tutteli.toFile(), 'a.kt')
        kt << 'package ch.tutteli.atrium'

        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("publishAllPublicationsToMavenRepository", "printSigning", "--stacktrace")
            .build()

        Asserts.assertTaskRunSuccessfully(result, ":$PublishPlugin.TASK_GENERATE_GRADLE_METADATA")
        Asserts.assertTaskRunSuccessfully(result, ":$PublishPlugin.TASK_GENERATE_POM")
        Asserts.assertTaskRunSuccessfully(result, ":signTutteliPublication")
        Asserts.assertTaskRunSuccessfully(result, ":publishTutteliPublicationToMavenRepository")

        def releasePath = getReleasePath(settingsSetup, projectName, groupId, version)
        def (pom, pomName) = getPomInclFileNameAndAssertBasicPomProperties(releasePath, projectName, groupId, version, githubUser)
        assertContainsRegex(pom, "licenses", "<licenses>$NL_INDENT" +
            "<license>$NL_INDENT" +
            "<name>${StandardLicenses.APACHE_2_0.longName}</name>$NL_INDENT" +
            "<url>${StandardLicenses.APACHE_2_0.url}</url>$NL_INDENT" +
            "<distribution>repo</distribution>$NL_INDENT" +
            "</license>$NL_INDENT" +
            "</licenses"
        )
        assertContainsNotRegex(pom, "developers", "<developers")
        assertContainsRegex(pom, "dependencies", "<dependencies>$NL_INDENT" +
            "<dependency>$NL_INDENT" +
            "<groupId>org.jetbrains.kotlin</groupId>$NL_INDENT" +
            "<artifactId>kotlin-stdlib-jdk8</artifactId>$NL_INDENT" +
            "<version>$KOTLIN_VERSION</version>$NL_INDENT" +
            "<scope>compile</scope>$NL_INDENT" +
            "</dependency>$NL_INDENT" +
            "<dependency>$NL_INDENT" +
            "<groupId>ch.tutteli.atrium</groupId>$NL_INDENT" +
            "<artifactId>atrium-fluent-en_GB</artifactId>$NL_INDENT" +
            "<version>$ATRIUM_VERSION</version>$NL_INDENT" +
            "<scope>runtime</scope>$NL_INDENT" +
            "</dependency>$NL_INDENT" +
            "</dependencies>"
        )

        def repoUrl = "https://github.com/$githubUser/$projectName"
        assertModuleExists(releasePath, projectName, version)
        assertJarsWithLicenseAndManifest(releasePath, projectName, version, repoUrl, null, KOTLIN_VERSION, pomName,
            ".jar",
            "-sources.jar",
        )
        new ZipFile(releasePath.resolve("$projectName-${version}.jar").toFile()).withCloseable { zipFile ->
            assertInZipFile(zipFile, 'a.txt')
        }
        new ZipFile(releasePath.resolve("$projectName-${version}-sources.jar").toFile()).withCloseable { zipFile ->
            assertInZipFile(zipFile, 'main/ch/tutteli/atrium/a.kt')
        }
        assertSigning(result, gpgPassphrase, gpgKeyId, gpgKeyRing)
    }

    @Test
    void withKotlinMultiplatformApplied_enableGranularSourceSetsMetadataIsTrue(SettingsExtensionObject settingsSetup) throws IOException {
        checkKotlinMultiplatform('mpp-enabled-granular', settingsSetup, true)
    }

    @Test
    void withKotlinMultiplatformApplied_enableGranularSourceSetsMetadataIsFalse(SettingsExtensionObject settingsSetup) throws IOException {
        checkKotlinMultiplatform('mpp-enabled-non-granular', settingsSetup, false)
    }

    @Test
    void withKotlinMultiplatformApplied_Kotlin1_7(SettingsExtensionObject settingsSetup) throws IOException {
        checkKotlinMultiplatform('mpp-kotlin-1.7.0', settingsSetup, false, "1.7.0")
    }

    @Test
    void withKotlinMultiplatformApplied_gradle_6_x(SettingsExtensionObject settingsSetup) throws IOException {
        def javaVersion = JavaVersion.toVersion(System.getProperty("java.version"))
        assumeFalse(javaVersion >= JavaVersion.VERSION_15)
        checkKotlinMultiplatform('mpp-gradle-6.9.4', settingsSetup, false, KOTLIN_VERSION, "6.9.4")
    }

    static void checkKotlinMultiplatform(
        String projectName,
        SettingsExtensionObject settingsSetup,
        boolean enableGranularSourceSetsMetadata,
        String kotlinVersion = KOTLIN_VERSION,
        String gradleVersion = null
    ) {

        settingsSetup.settings << "rootProject.name='$projectName'"
        def groupId = 'com.example'
        def version = '1.0.0'
        def githubUser = 'robstoll'
        def gpgPassphrase = 'bla'
        def gpgKeyId = 'A5875B96'
        def gpgKeyRing = 'keyring.gpg'

        settingsSetup.gpgKeyRing << PublishPluginIntTest.class.getResourceAsStream('/test-tutteli-gradle-plugin.gpg')

        settingsSetup.buildGradle << """
        import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTests

        ${settingsSetup.buildscriptWithKotlin(kotlinVersion)}
        buildscript {
            ext {
                // required since we don't set the System.env variables.
                gpgPassphrase = '$gpgPassphrase'
                gpgKeyRing = '$gpgKeyRing'
                gpgKeyId = '$gpgKeyId'
            }
        }
        repositories {
            mavenCentral()
        }

        apply plugin: 'org.jetbrains.kotlin.multiplatform'
        apply plugin: 'ch.tutteli.gradle.plugins.publish'

        project.with {
            group = '$groupId'
            version = '$version'
            description = 'test project'
        }
        tutteliPublish {
            //minimal setup required for local publish, all other things are only needed if not the default is used
            githubUser = '$githubUser'
        }

        kotlin {
           jvm { }
           js(LEGACY) { browser { } }
           def hostOs = System.getProperty("os.name")
           def isMingwX64 = hostOs.startsWith("Windows")
           KotlinNativeTargetWithTests nativeTarget
           if (hostOs == "Mac OS X") nativeTarget = macosX64('native')
           else if (hostOs == "Linux") nativeTarget = linuxX64("native")
           else if (isMingwX64) nativeTarget = mingwX64("native")
           else throw new GradleException("Host OS is not supported in Kotlin/Native.")


           sourceSets {
               commonMain { }
               commonTest { }
               jvmMain { }
               jvmTest { }
               jsMain { }
               jsTest { }
               nativeMain { }
               nativeTest { }
           }
        }
        ${publishingRepo()}
        ${taskPrintSigning()}
        """
        File license = new File(settingsSetup.tmp, 'LICENSE.txt')
        license << 'Copyright...'
        Path commonMain = Files.createDirectories(settingsSetup.tmpPath.resolve('src').resolve('commonMain'))
        Path commonTutteli = Files.createDirectories(commonMain.resolve("kotlin").resolve('ch').resolve('tutteli').resolve('atrium'))
        File commonKt = new File(commonTutteli.toFile(), 'common.kt')
        commonKt << """package ch.tutteli.atrium
                    val a = 1"""

        Path jvmMain = Files.createDirectories(settingsSetup.tmpPath.resolve('src').resolve('jvmMain'))
        Path resources = Files.createDirectory(jvmMain.resolve('resources'))
        File txt = new File(resources.toFile(), 'a.txt')
        txt << 'dummy'
        Path tutteli = Files.createDirectories(jvmMain.resolve('kotlin').resolve('ch').resolve('tutteli').resolve('atrium'))
        File kt = new File(tutteli.toFile(), 'a.kt')
        kt << 'package ch.tutteli.atrium'

        if (enableGranularSourceSetsMetadata) {
            File gradleProperties = new File(settingsSetup.tmpPath.toFile(), 'gradle.properties')
            gradleProperties << """
            kotlin.mpp.enableGranularSourceSetsMetadata=true
            """
        }

        //act

        def builder = GradleRunner.create()
        if (gradleVersion != null) {
            builder = builder.withGradleVersion(gradleVersion)
        }
        def result = builder
            .withProjectDir(settingsSetup.tmp)
            .withArguments("publishAllPublicationsToMavenRepository", "printSigning", "--stacktrace")
            .build()

        Asserts.assertTaskNotInvolved(result, ":$PublishPlugin.TASK_GENERATE_GRADLE_METADATA")
        Asserts.assertTaskNotInvolved(result, ":$PublishPlugin.TASK_GENERATE_POM")
        if (enableGranularSourceSetsMetadata) {
            Asserts.assertTaskRunSuccessfully(result, ":allMetadataJar")
        }
        assertSigning(result, gpgPassphrase, gpgKeyId, gpgKeyRing)


        def repoUrl = "https://github.com/$githubUser/$projectName"

        def rootReleasePath = getReleasePath(settingsSetup, projectName, groupId, version)
        def (_, rootPomName) = getPomInclFileNameAndAssertBasicPomProperties(rootReleasePath, projectName, groupId, version, githubUser)
        assertModuleExists(rootReleasePath, projectName, version)
        assertJarsWithLicenseAndManifest(rootReleasePath, projectName, version, repoUrl, null, kotlinVersion, rootPomName,
            ".jar",
            "-sources.jar",
        )
        if (enableGranularSourceSetsMetadata) {
            assertJarsWithLicenseAndManifest(rootReleasePath, projectName, version, repoUrl, null, kotlinVersion, rootPomName,
                "-all.jar",
            )
        } else {
            Path path = rootReleasePath.resolve("$projectName-${version}-all.jar")
            assertFalse(Files.exists(path), "${path} found even though enableGranularSourceSetsMetadata is set to false")
        }

        def jsReleasePath = getReleasePath(settingsSetup, projectName + "-js", groupId, version)
        def (_2, jsPomName) = getPomInclFileNameAndAssertBasicPomProperties(jsReleasePath, projectName + "-js", groupId, version, githubUser, projectName)
        assertModuleExists(jsReleasePath, projectName + "-js", version)
        assertJarsWithLicenseAndManifest(
            jsReleasePath, projectName, version, repoUrl, null, kotlinVersion, jsPomName,
            ".jar",
            "-sources.jar",
        )

        def jvmReleasePath = getReleasePath(settingsSetup, projectName + "-jvm", groupId, version)
        def (_3, jvmPomName) = getPomInclFileNameAndAssertBasicPomProperties(jvmReleasePath, projectName + "-jvm", groupId, version, githubUser, projectName)
        assertModuleExists(jvmReleasePath, projectName + "-jvm", version)
        assertJarsWithLicenseAndManifest(
            jvmReleasePath, projectName, version, repoUrl, null, kotlinVersion, jvmPomName,
            ".jar",
            "-sources.jar",
        )
        new ZipFile(jvmReleasePath.resolve("$projectName-jvm-${version}-sources.jar").toFile()).withCloseable { zipFile ->
            assertInZipFile(zipFile, 'commonMain/ch/tutteli/atrium/common.kt')
            assertInZipFile(zipFile, 'jvmMain/ch/tutteli/atrium/a.kt')
        }


        def nativeReleasePath = getReleasePath(settingsSetup, projectName + "-native", groupId, version)
        def (_4, nativePomName) = getPomInclFileNameAndAssertBasicPomProperties(nativeReleasePath, projectName + "-native", groupId, version, githubUser, projectName)
        assertModuleExists(nativeReleasePath, projectName + "-native", version)
        assertJarsWithLicenseAndManifest(
            nativeReleasePath, projectName, version, repoUrl, null, kotlinVersion, nativePomName,
            "-sources.jar",
        )
        def klibName = "$projectName-native-${version}.klib"
        Path path = nativeReleasePath.resolve(klibName)
        assertTrue(Files.exists(path), "${path} not found")
        assertAscWithHashesExistForFile(nativeReleasePath, klibName)

    }

    @Test
    void withKotlinMultiplatformApplied_andDokka(SettingsExtensionObject settingsSetup) throws IOException {
        def projectName = 'kotlin-mpp-with-dokka'
        settingsSetup.settings << "rootProject.name='$projectName'"
        def groupId = 'com.example'
        def version = '1.0.0'
        def githubUser = 'robstoll'
        def gpgPassphrase = 'bla'
        def gpgKeyId = 'A5875B96'
        def gpgKeyRing = 'keyring.gpg'

        settingsSetup.gpgKeyRing << PublishPluginIntTest.class.getResourceAsStream('/test-tutteli-gradle-plugin.gpg')

        settingsSetup.buildGradle << """
        import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTests

        ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}
        buildscript {
            ext {
                // required since we don't set the System.env variables.
                gpgPassphrase = '$gpgPassphrase'
                gpgKeyRing = '$gpgKeyRing'
                gpgKeyId = '$gpgKeyId'
            }
        }
        repositories {
            mavenCentral()
        }

        apply plugin: 'org.jetbrains.kotlin.multiplatform'
        apply plugin: 'ch.tutteli.gradle.plugins.publish'

        project.with {
            group = '$groupId'
            version = '$version'
            description = 'test project'
        }
        tutteliPublish {
            //minimal setup required for local publish, all other things are only needed if not the default is used
            githubUser = '$githubUser'
        }

        // simulate dokka's javadocJar task
        project.tasks.register("javadocJar", org.gradle.jvm.tasks.Jar) {
            archiveClassifier.set("javadoc")
        }

        kotlin {
           jvm { }
           js { browser { } }

           sourceSets {
               commonMain { }
               commonTest { }
               jvmMain { }
               jvmTest { }
               jsMain { }
               jsTest { }
           }
        }
        ${publishingRepo()}
        ${taskPrintSigning()}
        """
        File license = new File(settingsSetup.tmp, 'LICENSE.txt')
        license << 'Copyright...'
        Path commonMain = Files.createDirectories(settingsSetup.tmpPath.resolve('src').resolve('commonMain'))
        Path commonTutteli = Files.createDirectories(commonMain.resolve("kotlin").resolve('ch').resolve('tutteli').resolve('atrium'))
        File commonKt = new File(commonTutteli.toFile(), 'common.kt')
        commonKt << """package ch.tutteli.atrium
                    val a = 1"""

        Path jvmMain = Files.createDirectories(settingsSetup.tmpPath.resolve('src').resolve('jvmMain'))
        Path resources = Files.createDirectory(jvmMain.resolve('resources'))
        File txt = new File(resources.toFile(), 'a.txt')
        txt << 'dummy'
        Path tutteli = Files.createDirectories(jvmMain.resolve('kotlin').resolve('ch').resolve('tutteli').resolve('atrium'))
        File kt = new File(tutteli.toFile(), 'a.kt')
        kt << 'package ch.tutteli.atrium'

        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("publishAllPublicationsToMavenRepository", "printSigning")
            .build()

        Asserts.assertTaskNotInvolved(result, ":$PublishPlugin.TASK_GENERATE_GRADLE_METADATA")
        Asserts.assertTaskNotInvolved(result, ":$PublishPlugin.TASK_GENERATE_POM")
        assertSigning(result, gpgPassphrase, gpgKeyId, gpgKeyRing)


        def repoUrl = "https://github.com/$githubUser/$projectName"

        def rootReleasePath = getReleasePath(settingsSetup, projectName, groupId, version)
        def (_, rootPomName) = getPomInclFileNameAndAssertBasicPomProperties(rootReleasePath, projectName, groupId, version, githubUser)
        assertModuleExists(rootReleasePath, projectName, version)
        assertJarsWithLicenseAndManifest(rootReleasePath, projectName, version, repoUrl, null, KOTLIN_VERSION, rootPomName,
            ".jar",
            "-sources.jar",
            "-javadoc.jar"
        )
        Path path = rootReleasePath.resolve("$projectName-${version}-all.jar")
        assertFalse(Files.exists(path), "${path} found even though enableGranularSourceSetsMetadata is set to false")

        def jsReleasePath = getReleasePath(settingsSetup, projectName + "-js", groupId, version)
        def (_2, jsPomName) = getPomInclFileNameAndAssertBasicPomProperties(jsReleasePath, projectName + "-js", groupId, version, githubUser, projectName)
        assertModuleExists(jsReleasePath, projectName + "-js", version)
        assertJarsWithLicenseAndManifest(
            jsReleasePath, projectName, version, repoUrl, null, KOTLIN_VERSION, jsPomName,
            ".jar",
            "-sources.jar",
            "-javadoc.jar"
        )

        def jvmReleasePath = getReleasePath(settingsSetup, projectName + "-jvm", groupId, version)
        def (_3, jvmPomName) = getPomInclFileNameAndAssertBasicPomProperties(jvmReleasePath, projectName + "-jvm", groupId, version, githubUser, projectName)
        assertModuleExists(jvmReleasePath, projectName + "-jvm", version)
        assertJarsWithLicenseAndManifest(
            jvmReleasePath, projectName, version, repoUrl, null, KOTLIN_VERSION, jvmPomName,
            ".jar",
            "-sources.jar",
            "-javadoc.jar"
        )
        new ZipFile(jvmReleasePath.resolve("$projectName-jvm-${version}-sources.jar").toFile()).withCloseable { zipFile ->
            assertInZipFile(zipFile, 'commonMain/ch/tutteli/atrium/common.kt')
            assertInZipFile(zipFile, 'jvmMain/ch/tutteli/atrium/a.kt')
        }
    }

    private static Path getReleasePath(SettingsExtensionObject settingsSetup, String projectName, String groupId, String version, String subDir = '') {
        def build = Paths.get(settingsSetup.tmp.absolutePath, subDir, 'build', 'repo')
        def group = groupId.split("\\.").inject(build) {
            path, part -> path.resolve(part)
        }
        return group.resolve(projectName).resolve(version)
    }

    private static String taskPrintSigning() {
        return """
            task printSigning {
                doLast {
                    ${printSigning()}
                }
            }
        """
    }

    private static String publishingRepo() {
        return """
        // only here for testing purposes, not needed for the plugin
        publishing {
            repositories {
                maven {
                    url = layout.buildDirectory.dir('repo')
                }
            }
        }
        """
    }

    private static String printSigning() {
        return """
            if (project.hasProperty('signing')) {
                println("signing.password: \${project.ext."signing.password"}")
                println("signing.keyId: \${project.ext."signing.keyId"}")
                println("signing.secretKeyRingFile: \${project.ext."signing.secretKeyRingFile"}")
            }
        """
    }

    private static void assertSigning(BuildResult result, String gpgPassphrase, String gpgKeyId, String gpgKeyRing) {
        assertTrue(result.output.contains("signing.password: $gpgPassphrase"), "project.ext.\"signing.password\" $gpgPassphrase\n$result.output")
        assertTrue(result.output.contains("signing.keyId: $gpgKeyId"), "project.ext.\"signing.keyId\" $gpgKeyId\n$result.output")
        assertTrue(result.output.contains("signing.secretKeyRingFile: $gpgKeyRing"), "project.ext.\"signing.secretKeyRingFile\" $gpgKeyRing\n$result.output")
    }

    private static void assertJarsWithLicenseAndManifest(Path releasePath, String projectName, String version, String repoUrl, String vendor, String kotlinVersion, String pomName, String... jarNameEndings) {
        def prefix = pomName.substring(0, pomName.length() - 4)
        jarNameEndings.each { jarNameEnding ->
            assertJarWithLicenseAndManifest(releasePath, prefix + jarNameEnding, projectName, version, repoUrl, vendor, kotlinVersion)
            assertAscWithHashesExistForFile(releasePath, prefix + jarNameEnding)
        }
    }

    private static void assertJarWithLicenseAndManifest(Path releasePath, String jarName, String projectName, String version, String repoUrl, String vendor, String kotlinVersion) {
        def zipFile = new ZipFile(releasePath.resolve(jarName).toFile())
        zipFile.withCloseable {
            def manifest = zipFile.getInputStream(findInZipFile(zipFile, 'META-INF/MANIFEST.MF')).text
            assertManifest(manifest, ': ', projectName, jarName, version, repoUrl, vendor, kotlinVersion)
            assertTrue(manifest.contains("Build-Time: ${new Date().format('yyyy-MM-dd\'T\'HH:mm:ss.SSSZZ').substring(0, 10)}"), "manifest build time was not ${new Date().format('yyyy-MM-dd\'T\'HH:mm:ss.SSSZZ').substring(0, 10)}\n${manifest}")
            assertInZipFile(zipFile, 'LICENSE.txt')
        }
    }

    private static void assertInZipFile(ZipFile zipFile, String path) {
        assertNotNull(findInZipFile(zipFile, path), "$path not found in $zipFile.name")
    }

    private static ZipEntry findInZipFile(ZipFile zipFile, String path) {
        return zipFile.entries().find {
            it.getName() == path
        } as ZipEntry
    }


    private static void assertManifest(String output, String separator, String projectName, String jarName, String version, String repoUrl, String vendor, String kotlinVersion) {
        assertTrue(output.contains("Implementation-Title$separator$projectName"), "$jarName contains: Implementation-Title$separator$projectName\nbut was:\n${output}")
        assertTrue(output.contains("Implementation-Version$separator$version"), "$jarName  contains: Implementation-Version$separator$version\nbut was:\n${output}")
        assertTrue(output.contains("Implementation-URL$separator$repoUrl"), "$jarName contains: Implementatison-URL$separator$repoUrl\nbut was:\n${output}")
        if (vendor != null) {
            assertTrue(output.contains("Implementation-Vendor$separator$vendor"), "$jarName contains: Implementation-Vendor$separator$vendor\nbut was:\n${output}")
        } else {
            assertFalse(output.contains("Implementation-Vendor"), "$jarName: should not contain Implementation-Vendor")
        }
        assertTrue(output.contains("Implementation-Kotlin-Version$separator$kotlinVersion"), "$jarName contains: Implementation-Kotlin-Version$separator$kotlinVersion\nbut was:\n${output}")
    }

    private static void assertModuleExists(Path releasePath, String projectName, String version) {
        def moduleFileName = getArtifactName(releasePath, projectName, version, "module")
        assertTrue(Files.exists(releasePath.resolve(moduleFileName)))
        assertAscWithHashesExistForFile(releasePath, moduleFileName)
    }


    private static List<String> getPomInclFileNameAndAssertBasicPomProperties(Path releasePath, String projectName, String groupId, String version, String githubUser, String rootProjectName = '') {
        String pomFileName = getArtifactName(releasePath, projectName, version, "pom")
        String pom = releasePath.resolve(pomFileName).toFile().getText('UTF-8')
        assertAscWithHashesExistForFile(releasePath, pomFileName)
        assertContainsRegex(pom, "published-with-gradle-metadata", "published-with-gradle-metadata")
        assertContainsRegex(pom, "groupId", "<groupId>$groupId</groupId>")
        assertContainsRegex(pom, "artifactId", "<artifactId>$projectName</artifactId>")
        assertContainsRegex(pom, "description", "<description>test project</description>")

        def domainAndPath = "github.com/$githubUser/${rootProjectName != '' ? rootProjectName : projectName}"
        assertContainsRegex(pom, "scm", "<scm>$NL_INDENT" +
            "<connection>scm:git:git://${domainAndPath}.git</connection>$NL_INDENT" +
            "<developerConnection>scm:git:ssh://${domainAndPath}.git</developerConnection>$NL_INDENT" +
            "<url>https://$domainAndPath</url>$NL_INDENT" +
            "</scm>")
        return [pom, pomFileName]
    }

    private static String getArtifactName(Path releasePath, String projectName, String version, String extension) {
        return version.contains("-SNAPSHOT") ? findArtifact(releasePath, extension) : "$projectName-${version}.$extension"
    }

    private static String findArtifact(Path releasePath, String extension) {
        return Paths.get(new FileNameFinder().getFileNames(releasePath.toAbsolutePath().toString(), "*.$extension").head()).fileName.toString()
    }

    private static void assertAscWithHashesExistForFile(Path releasePath, String fileName) {
        assertHashesExistForFile(releasePath, fileName)
        assertTrue(Files.exists(releasePath.resolve(fileName + ".asc")), "asc for $fileName not found")
        assertHashesExistForFile(releasePath, fileName + ".asc")
    }

    private static void assertHashesExistForFile(Path releasePath, String fileName) {
        ['md5', 'sha1', 'sha256', 'sha512'].forEach {
            assertTrue(Files.exists(releasePath.resolve(fileName + "." + it)), "$it for $fileName not found")
        }
    }
}
