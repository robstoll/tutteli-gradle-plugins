package ch.tutteli.gradle.plugins.publish

import ch.tutteli.gradle.plugins.test.Asserts
import ch.tutteli.gradle.plugins.test.SettingsExtension
import ch.tutteli.gradle.plugins.test.SettingsExtensionObject
import groovy.io.FileType
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

import static ch.tutteli.gradle.plugins.test.Asserts.*
import static org.junit.jupiter.api.Assertions.*
import static org.junit.jupiter.api.Assumptions.assumeFalse

@ExtendWith(SettingsExtension)
class PublishPluginIntTest {
    def static final KOTLIN_VERSION = '1.8.22'
    def static final ATRIUM_VERSION = '1.0.0'

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

        apply plugin: 'java'
        apply plugin: 'ch.tutteli.gradle.plugins.publish'

        project.ext.set('signing.password', '$gpgPassphrase')
        project.ext.set('signing.keyId', '$gpgKeyId')
        project.ext.set('signing.secretKeyRingFile', '$gpgKeyRing')

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
            addLicense 'Apache-2.0'
            addLicense 'Apache-2.0', 'manually'
            addLicense ch.tutteli.gradle.plugins.publish.StandardLicenses.APACHE_2_0
            addLicense ch.tutteli.gradle.plugins.publish.StandardLicenses.APACHE_2_0, 'somethingElse'
            addLicense(new ch.tutteli.gradle.plugins.publish.License(
                'Lic-1.2',
                'License 1.2',
                'https://license.com',
                'repo'
            ))

            // you can add multiple developers if required
            addDeveloper {
                id = 'robstoll'
                name = 'Robert Stoll'
                email = 'rstoll@tutteli.ch'
                url = 'tutteli.ch'
            }
            addDeveloper {
                id = 'robstoll_tutteli'
                name = 'Robert Stoll'
                email = 'rstoll@tutteli.ch'
                organization = 'tutteli'
                organizationUrl = 'tutteli.ch'
            }

            // will add Implementation-Vendor to all manifest files.
            manifestVendor = 'tutteli.ch'

            // you can also disable GPG signing (default is true)
            signWithGpg = false
            // yet, we will re-activate it for this test
            signWithGpg = true

            // default uses GPG-command, we use the java-version here to have less issues in CI regarding ubuntu/windows
            usePgpJava()
        }

        ${publishingRepo()}
        """
        //act
        def builder = GradleRunner.create()
        if (gradleVersion != null) {
            builder = builder.withGradleVersion(gradleVersion)
        }
        def result = builder
            .withProjectDir(settingsSetup.tmp)
            .withArguments("publishAllPublicationsToMavenRepository",  "--stacktrace")
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
        ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}

        project.ext.set('signing.password', '$gpgPassphrase')
        project.ext.set('signing.keyId', '$gpgKeyId')
        project.ext.set('signing.secretKeyRingFile', "\$project.projectDir/$gpgKeyRing")

        project.with {
            group = '$groupId'
            version = '$version'
        }

        subprojects {
            it.description = 'test project'

            repositories {  mavenCentral(); }
            apply plugin: 'org.jetbrains.kotlin.jvm'

            task('testJar', type: Jar) {
                from sourceSets.test.output
                archiveClassifier = 'tests'
            }

           apply plugin: 'ch.tutteli.gradle.plugins.publish'

            // still included in publish, use the artifactFilter to exclude a jar as artifact
            task('testSourcesJar', type: Jar) {
                from sourceSets.test.allSource
                archiveClassifier = 'testsources'
            }

            task('testSourcesJarFiltered', type: Jar) {
                from sourceSets.test.allSource
                archiveClassifier = 'testsources-filtered'
            }

            tutteliPublish {
                resetLicenses 'EUPL-1.2'

                //already defined because it is a ch.tutteli project
                //githubUser = '$githubUser'
                //manifestVendor = $vendor // we don't have a manifestVendor, thus we reset it to null

                artifactFilter = { jar -> jar.name != 'testSourcesJarFiltered' } as kotlin.jvm.functions.Function1

                // default uses GPG-command, we use the java-version here to have less issues in CI regarding ubuntu/windows
                usePgpJava()
            }
            ${publishingRepo()}
        }

        configure(project(':$dependentName')) {
            dependencies {
                api rootProject.project(':$subprojectName')
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
            .withArguments("publishAllPublicationsToMavenRepository", "--stacktrace")
            .build()

        //assert
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
            "<groupId>org.jetbrains.kotlin</groupId>$NL_INDENT" +
            "<artifactId>kotlin-stdlib-jdk8</artifactId>$NL_INDENT" +
            "<version>$KOTLIN_VERSION</version>$NL_INDENT" +
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

        assertJarsWithLicenseAndManifest(dependentReleasePath, dependentName, version, repoUrl, vendor, KOTLIN_VERSION, dependentPomName,
            ".jar",
            "-sources.jar",
            "-tests.jar"
        )
        assertModuleExists(dependentReleasePath, dependentName, version)

        def subReleasePath = getReleasePath(settingsSetup, subprojectName, groupId, version, subprojectName)
        def (_, subPomName) = getPomInclFileNameAndAssertBasicPomProperties(subReleasePath, subprojectName, groupId, version, githubUser, rootProjectName)
        assertJarsWithLicenseAndManifest(subReleasePath, subprojectName, version, repoUrl, vendor, KOTLIN_VERSION, subPomName,
            ".jar",
            "-sources.jar",
            "-tests.jar"
        )
        assertModuleExists(subReleasePath, subprojectName, version)
    }

    @Test
    void withKotlinJvmApplied(SettingsExtensionObject settingsSetup) throws IOException {
        checkKotlinJvmApplied(settingsSetup, false)
    }

    @Test
    void withKotlinJvmApplied_andDokka(SettingsExtensionObject settingsSetup) throws IOException {
        checkKotlinJvmApplied(settingsSetup, true)
    }

    void checkKotlinJvmApplied(SettingsExtensionObject settingsSetup, Boolean withDokka) {
        String projectName
        if (withDokka) {
            projectName = 'kotlin-jvm-with-dokka'
        } else {
            projectName = 'kotlin-jvm'
        }

        settingsSetup.settings << "rootProject.name='$projectName'"
        def groupId = 'com.example'
        def version = '1.0.0'
        def githubUser = 'robstoll'
        def gpgPassphrase = 'bla'
        def gpgKeyId = 'A5875B96'
        def gpgKeyRing = 'keyring.gpg'
        def dokkaDependency = ""
        def dokkaPlugin = ""
        if (withDokka) {
            dokkaDependency = """
            dependencies {
                classpath 'org.jetbrains.dokka:org.jetbrains.dokka.gradle.plugin:1.9.0'
            }
            """
            dokkaPlugin = "apply plugin: 'org.jetbrains.dokka'"
        }

        settingsSetup.gpgKeyRing << PublishPluginIntTest.class.getResourceAsStream('/test-tutteli-gradle-plugin.gpg')

        settingsSetup.buildGradle << """
        ${settingsSetup.buildscriptWithKotlin(KOTLIN_VERSION)}
        buildscript {
            $dokkaDependency
        }

        project.ext.set('signing.password', '$gpgPassphrase')
        project.ext.set('signing.keyId', '$gpgKeyId')
        project.ext.set('signing.secretKeyRingFile', '$gpgKeyRing')

        repositories {
            mavenCentral()
        }

        apply plugin: 'org.jetbrains.kotlin.jvm'
        $dokkaPlugin
        apply plugin: 'ch.tutteli.gradle.plugins.publish'

        project.with {
            group = '$groupId'
            version = '$version'
            description = 'test project'
        }
        tutteliPublish {
            // minimal setup required for local publish, all other things are only needed if not the default is used
            githubUser = '$githubUser'

            // default uses GPG-command, we use the java-version here to have less issues in CI regarding ubuntu/windows
            usePgpJava()
        }
        dependencies {
            implementation 'ch.tutteli.atrium:atrium-fluent:$ATRIUM_VERSION'
        }

        ${publishingRepo()}
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
            .withArguments("publishAllPublicationsToMavenRepository", "--stacktrace")
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
            "<artifactId>atrium-fluent-jvm</artifactId>$NL_INDENT" +
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
        if (withDokka) {
            assertJarsWithLicenseAndManifest(releasePath, projectName, version, repoUrl, null, KOTLIN_VERSION, pomName,
                "-javadoc.jar"
            )
        }
        new ZipFile(releasePath.resolve("$projectName-${version}.jar").toFile()).withCloseable { zipFile ->
            assertInZipFile(zipFile, 'a.txt')
        }
        new ZipFile(releasePath.resolve("$projectName-${version}-sources.jar").toFile()).withCloseable { zipFile ->
            assertInZipFile(zipFile, 'main/ch/tutteli/atrium/a.kt')
        }
    }


    @Test
    void withKotlinMultiplatformApplied_Kotlin1_7(SettingsExtensionObject settingsSetup) throws IOException {
        checkKotlinMultiplatform('mpp-kotlin-1.7.0', settingsSetup, "1.7.0")
    }

    @Test
    void withKotlinMultiplatformApplied_gradle_6_x(SettingsExtensionObject settingsSetup) throws IOException {
        def javaVersion = JavaVersion.toVersion(System.getProperty("java.version"))
        assumeFalse(javaVersion >= JavaVersion.VERSION_15)
        checkKotlinMultiplatform('mpp-gradle-6.9.4', settingsSetup, KOTLIN_VERSION, "6.9.4")
    }

    static void checkKotlinMultiplatform(
        String projectName,
        SettingsExtensionObject settingsSetup,
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

        project.ext.set('signing.password', '$gpgPassphrase')
        project.ext.set('signing.keyId', '$gpgKeyId')
        project.ext.set('signing.secretKeyRingFile', '$gpgKeyRing')

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

            // default uses GPG-command, we use the java-version here to have less issues in CI regarding ubuntu/windows
            usePgpJava()
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

        def builder = GradleRunner.create()
        if (gradleVersion != null) {
            builder = builder.withGradleVersion(gradleVersion)
        }
        def result = builder
            .withProjectDir(settingsSetup.tmp)
            .withArguments("publishAllPublicationsToMavenRepository", "--stacktrace")
            .build()

        Asserts.assertTaskNotInvolved(result, ":$PublishPlugin.TASK_GENERATE_GRADLE_METADATA")
        Asserts.assertTaskNotInvolved(result, ":$PublishPlugin.TASK_GENERATE_POM")


        def repoUrl = "https://github.com/$githubUser/$projectName"

        def rootReleasePath = getReleasePath(settingsSetup, projectName, groupId, version)
        def (_, rootPomName) = getPomInclFileNameAndAssertBasicPomProperties(rootReleasePath, projectName, groupId, version, githubUser)
        assertModuleExists(rootReleasePath, projectName, version)
        assertJarsWithLicenseAndManifest(rootReleasePath, projectName, version, repoUrl, null, kotlinVersion, rootPomName,
            ".jar",
            "-sources.jar",
        )

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
            dependencies {
                classpath 'org.jetbrains.dokka:org.jetbrains.dokka.gradle.plugin:1.9.0'
            }
        }
        project.ext.set('signing.password', '$gpgPassphrase')
        project.ext.set('signing.keyId', '$gpgKeyId')
        project.ext.set('signing.secretKeyRingFile', '$gpgKeyRing')

        repositories {
            mavenCentral()
        }

        apply plugin: 'org.jetbrains.kotlin.multiplatform'
        apply plugin: 'org.jetbrains.dokka'
        apply plugin: 'ch.tutteli.gradle.plugins.publish'

        project.with {
            group = '$groupId'
            version = '$version'
            description = 'test project'
        }
        tutteliPublish {
            //minimal setup required for local publish, all other things are only needed if not the default is used
            githubUser = '$githubUser'

            // default uses GPG-command, we use the java-version here to have less issues in CI regarding ubuntu/windows
            usePgpJava()
        }

        kotlin {
           jvm { }
           js(LEGACY) { browser { } }

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
        """
        File license = new File(settingsSetup.tmp, 'LICENSE.txt')
        license << 'Copyright...'
        Path commonMain = Files.createDirectories(settingsSetup.tmpPath.resolve('src').resolve('commonMain'))
        Path commonTutteli = Files.createDirectories(commonMain.resolve("kotlin").resolve('ch').resolve('tutteli').resolve('atrium'))
        File commonKt = new File(commonTutteli.toFile(), 'common.kt')
        commonKt << """package ch.tutteli.atrium
                    /**
                     * the variable a.
                     */
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
            .withArguments("publishAllPublicationsToMavenRepository")
            .build()

        Asserts.assertTaskNotInvolved(result, ":$PublishPlugin.TASK_GENERATE_GRADLE_METADATA")
        Asserts.assertTaskNotInvolved(result, ":$PublishPlugin.TASK_GENERATE_POM")


        def repoUrl = "https://github.com/$githubUser/$projectName"

        def rootReleasePath = getReleasePath(settingsSetup, projectName, groupId, version)
        def (_, rootPomName) = getPomInclFileNameAndAssertBasicPomProperties(rootReleasePath, projectName, groupId, version, githubUser)
        assertModuleExists(rootReleasePath, projectName, version)
        assertJarsWithLicenseAndManifest(rootReleasePath, projectName, version, repoUrl, null, KOTLIN_VERSION, rootPomName,
            ".jar",
            "-sources.jar",
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


    private static void assertJarsWithLicenseAndManifest(Path releasePath, String projectName, String version, String repoUrl, String vendor, String kotlinVersion, String pomName, String... jarNameEndings) {
        def prefix = pomName.substring(0, pomName.length() - 4)
        jarNameEndings.each { jarNameEnding ->
            assertJarWithLicenseAndManifest(releasePath, prefix + jarNameEnding, projectName, version, repoUrl, vendor, kotlinVersion)
            assertAscWithHashesExistForFile(releasePath, prefix + jarNameEnding)
        }
    }

    private static void assertJarWithLicenseAndManifest(Path releasePath, String jarName, String projectName, String version, String repoUrl, String vendor, String kotlinVersion) {
        def jarPath = releasePath.resolve(jarName).toFile()
        if (!jarPath.exists()) {
            def list = []
            jarPath.getParentFile().eachFileRecurse(FileType.FILES) { file ->
                list << file
            }
            throw new AssertionError("$jarPath does not exist. Found following files in this folder: ${list.join("\n")}")
        }
        def zipFile = new ZipFile(jarPath)
        zipFile.withCloseable {
            def manifest = zipFile.getInputStream(findInZipFile(zipFile, 'META-INF/MANIFEST.MF')).text
            assertManifest(manifest, ': ', projectName, jarName, version, repoUrl, vendor, kotlinVersion)
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
