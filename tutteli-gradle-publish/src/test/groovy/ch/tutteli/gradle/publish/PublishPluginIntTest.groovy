package ch.tutteli.gradle.publish

import ch.tutteli.gradle.test.Asserts
import ch.tutteli.gradle.test.SettingsExtension
import ch.tutteli.gradle.test.SettingsExtensionObject
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

import static ch.tutteli.gradle.test.Asserts.assertContainsRegex
import static ch.tutteli.gradle.test.Asserts.getNL_INDENT
import static org.junit.jupiter.api.Assertions.*

@ExtendWith(SettingsExtension)
class PublishPluginIntTest {

    @Test
    void smokeTest(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        def projectName = 'test-project'
        settingsSetup.settings << "rootProject.name='$projectName'"
        def version = '1.0.0-SNAPSHOT'
        def githubUser = 'robstoll'
        def user = 'myUser'
        def apiKey = 'test'
        def pkgName = "tutteli-gradle"
        def organisation = "company"
        def gpgPassphrase = 'bla'
        def gpgKeyId = 'A5875B96'
        def gpgKeyRing = 'keyring.gpg'

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
        
        // has to be before ch.tutteli.publish
        apply plugin: 'java' 
        apply plugin: 'ch.tutteli.publish'
        
        project.with {
            group = 'com.example'
            version = '$version'
            description = 'test project'
        }
        
        tutteliPublish {
            //minimal setup required for publish, all other things are only needed if not the default is used
            githubUser = '$githubUser'
            bintrayRepo = 'tutteli-jars'

            //different ways to override the default license
            resetLicenses 'EUPL-1.2'             // default distribution is 'repo'
            resetLicenses 'EUPL-1.2', 'manually' 
            resetLicenses ch.tutteli.gradle.publish.StandardLicenses.EUPL_1_2
            resetLicenses ch.tutteli.gradle.publish.StandardLicenses.EUPL_1_2, 'manually'
            resetLicenses {
                shortName = 'Lic-1.2'
                longName = 'License 1.2'
                url = 'https://license.com'
                distribution = 'manually'
            }
            resetLicenses {
                shortName = 'Lic-1.2'
                longName = 'License 1.2'
                url = 'https://license.com'
                //default distribution is repo
            }
            
            // different ways to add additional licenses
            license 'Apache-2.0'
            license 'Apache-2.0', 'manually'
            license ch.tutteli.gradle.publish.StandardLicenses.APACHE_2_0
            license ch.tutteli.gradle.publish.StandardLicenses.APACHE_2_0, 'somethingElse'
            license {
                shortName = 'Lic-1.2'
                longName = 'License 1.2'
                url = 'https://license.com'
                distribution = 'repo'
            }
            
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
            
            // you can change the pkg name if it does not correspond to `project.name`
            bintrayPkg = '$pkgName'

            // you can define the organisation if you don't publish to a repo of the user
            bintrayOrganisation = '$organisation'

            // you can customise the property and env variable names if they differ from the convention
            propNameBintrayUser   = 'myBintrayUser'             // default is bintrayUser
            propNameBintrayApiKey = 'myBintrayApiKey'           // default is bintrayApiKey
            propNameGpgPassphrase = 'myGpgPassphrase'           // default is gpgPassphrase
            propNameGpgKeyId      = 'myGpgKeyId'                // default is gpgKeyId
            propNameGpgKeyRing     = 'myGpgKeyRing'             // default is gpgKeyRing
            envNameBintrayUser    = 'MY_BINTRAY_USER'           // default is BINTRAY_USER
            envNameBintrayApiKey  = 'MY_BINTRAY_API_KEY'        // default is BINTRAY_API_KEY
            envNameGpgPassphrase  = 'MY_GPG_PASSPHRASE'         // default is GPG_PASSPHRASE
            envNameGpgKeyId       = 'MY_GPG_KEY_ID'             // default is GPG_KEY_ID
            envNameGpgKeyRing     = 'MY_GPG_KEY_RING'           // default is GPG_KEY_RING
            envNameGpgSigningKey  = 'MY_GPG_SIGNING_KEY'        // default is GPG_SIGNING_KEY
            
            // you can also disable GPG signing (default is true)
            signWithGpg = false
            // yet, we will re-activate it for this test
            signWithGpg = true
            
            // you could configure JFrog's bintray extension here if you like.
            // There is no need for it though, everything can be configured via the above
            bintray {
                user = '$user' //usually provided by property or env variable
            }     
        }        
        
         // you could also configure JFrog's bintray extension outside of publish
         // but again, there is no need for it.
        bintray {
            key = '$apiKey'
        }  
        project.afterEvaluate { 
            ${printBintray()}
        }
        ${taskPrintSigning()}
        """
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("publishTutteliPublicationToMavenLocal", "printSigning", "--stacktrace")
            .build()
        //assert
        assertTrue(result.output.contains("Some licenses were duplicated. Please check if you made a mistake."), "should contain warning about duplicated licenses:\n$result.output")
        String pom = Paths.get(settingsSetup.tmp.absolutePath, 'build', 'publications', 'tutteli', 'pom-default.xml').toFile().getText('UTF-8')
        assertContainsRegex(pom, "name", "<name>$projectName</name>")
        assertContainsRegex(pom, "description", "<description>test project</description>")
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

        def domainAndPath = "github.com/$githubUser/$projectName"
        assertContainsRegex(pom, "scm", "<scm>$NL_INDENT" +
            "<connection>scm:git:git://${domainAndPath}.git</connection>$NL_INDENT" +
            "<developerConnection>scm:git:ssh://${domainAndPath}.git</developerConnection>$NL_INDENT" +
            "<url>https://$domainAndPath</url>$NL_INDENT" +
            "</scm>")
        assertBintray(result, user, apiKey, pkgName, projectName, "https://" + domainAndPath, version, "Apache-2.0,Lic-1.2", organisation)
        assertSigning(result, gpgPassphrase, gpgKeyId, gpgKeyRing)
    }

    @Test
    void smokeTest_GpgPassphraseMissing(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        def projectName = 'test-project'
        settingsSetup.settings << "rootProject.name='$projectName'"
        def version = '1.0.0-SNAPSHOT'
        def githubUser = 'robstoll'
        def user = 'myUser'

        settingsSetup.buildGradle << """
        buildscript {
            dependencies {
                classpath files($settingsSetup.pluginClasspath)
            }
        }
        // has to be before ch.tutteli.publish
        apply plugin: 'java' 
        apply plugin: 'ch.tutteli.publish'
        
        project.with {
            group = 'com.example'
            version = '$version'
            description = 'test project'
        }
        
        tutteliPublish {
            //minimal setup required for publish, all other things are only needed if not the default is used
            githubUser = '$githubUser'
            bintrayRepo = 'tutteli-jars'
            //gpg passphrase not defined via property or something
            
            bintray {
                user = '$user'
                key = 'apiKey'
            }
        }        
        """
        //act
        GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("tasks", "--stacktrace")
            .build()
        //assert no exception
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
    void combinePlugins(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        def projectName = 'test-project'
        settingsSetup.settings << "rootProject.name='$projectName'"
        def groupId = 'com.example'
        def version = '1.0.0-SNAPSHOT'
        def githubUser = 'robstoll'
        def vendor = 'tutteli'
        def kotlinVersion = '1.2.71'
        def user = 'test-user'
        def apiKey = 'test-key'
        def gpgPassphrase = 'bla'
        def gpgKeyId = 'A5875B96'
        def gpgKeyRing = 'keyring.gpg'

        settingsSetup.gpgKeyRing << PublishPluginIntTest.class.getResourceAsStream('/test-tutteli-gradle-plugin.gpg')

        settingsSetup.buildGradle << """
        buildscript {
            repositories { maven { url "https://plugins.gradle.org/m2/" } }
            dependencies {
                classpath 'ch.tutteli:tutteli-gradle-dokka:0.10.1'
                classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion'
                classpath files($settingsSetup.pluginClasspath)
            }
            
            ext {
                // required since we don't set the System.env variables.
                gpgPassphrase = '$gpgPassphrase'
                gpgKeyRing = '$gpgKeyRing'
                gpgKeyId = '$gpgKeyId'
            }
        }
        repositories {  
            mavenCentral();
            maven { url "http://dl.bintray.com/robstoll/tutteli-jars" } 
        }
        apply plugin: 'kotlin'
        apply plugin: 'ch.tutteli.dokka' 
        tutteliDokka.githubUser = '$githubUser'

        apply plugin: 'ch.tutteli.publish'
                
        project.with {
            group = '$groupId'
            version = '$version'
            description = 'test project'
        }
        
        tutteliPublish {
            githubUser = '$githubUser'
            // Apache License 2.0 is the default
            // developers are optional
            
            // Optional, in case you want to mention the vendor in the manifest file of all jars
            manifestVendor = '$vendor'

            // minimal setup required for bintray extension
            bintrayRepo = 'tutteli-jars'
            
            // required since we don't set the System.env variables.
            bintray {
                user = '$user'
                key = '$apiKey'
            }
        }        
        
        dependencies { 
            compile 'ch.tutteli.atrium:atrium-cc-en_GB-robstoll:0.7.0'
        }
        
        project.afterEvaluate {
            ${printArtifactsAndManifest()}
            ${printBintray()}
        }

        ${taskPrintSigning()}
        """
        File license = new File(settingsSetup.tmp, 'LICENSE.txt')
        license << "Copyright..."
        //act


        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("pubToMaLo", "printSigning", "--stacktrace")
            .build()
        //assert
        String pom = Paths.get(settingsSetup.tmp.absolutePath, 'build', 'publications', 'tutteli', 'pom-default.xml').toFile().getText('UTF-8')
        assertContainsRegex(pom, "licenses", "<licenses>$NL_INDENT" +
            "<license>$NL_INDENT" +
            "<name>${StandardLicenses.APACHE_2_0.longName}</name>$NL_INDENT" +
            "<url>${StandardLicenses.APACHE_2_0.url}</url>$NL_INDENT" +
            "<distribution>repo</distribution>$NL_INDENT" +
            "</license>$NL_INDENT" +
            "</licenses"
        )
        assertContainsRegex(pom, "developers", "<developers/>")
        assertContainsRegex(pom, "dependencies", "<dependencies>$NL_INDENT" +
            "<dependency>$NL_INDENT" +
            "<groupId>ch.tutteli.atrium</groupId>$NL_INDENT" +
            "<artifactId>atrium-cc-en_GB-robstoll</artifactId>$NL_INDENT" +
            "<version>0.7.0</version>$NL_INDENT" +
            "<scope>compile</scope>$NL_INDENT" +
            "</dependency>$NL_INDENT" +
            "</dependencies>"
        )

        def repoUrl = "https://github.com/$githubUser/$projectName"
        def output = result.output
        assertTrue(output.contains("groupId: $groupId"), "groupId $groupId\n${output}")
        assertTrue(output.contains("artifactId: $projectName"), "artifactId: $projectName\n${output}")
        assertTrue(output.contains("version: $version"), "version: $version\n${output}")
        assertTrue(output.contains("artifact: jar - null"), "java jar\n${output}")
        assertTrue(output.contains("artifact: jar - sources"), "sources jar\n${output}")
        assertTrue(output.contains("artifact: jar - javadoc"), "javadoc jar\n${output}")
        assertBintray(result, user, apiKey, projectName, projectName, repoUrl, version, "Apache-2.0", "null")
        assertSigning(result, gpgPassphrase, gpgKeyId, gpgKeyRing)
        assertJarWithLicenseAndManifest(settingsSetup, "$projectName-${version}.jar", projectName, version, repoUrl, vendor, kotlinVersion)
        assertJarWithLicenseAndManifest(settingsSetup, "$projectName-${version}-sources.jar", projectName, version, repoUrl, vendor, kotlinVersion)
        assertJarWithLicenseAndManifest(settingsSetup, "$projectName-${version}-javadoc.jar", projectName, version, repoUrl, vendor, kotlinVersion)
    }

    @Test
    void subproject(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        def rootProjectName = 'test-project'
        def subprojectNameWithoutJvm = 'test-sub'
        def subprojectName = "$subprojectNameWithoutJvm-jvm"
        def dependentName = 'dependent'
        settingsSetup.settings << """rootProject.name='$rootProjectName'
        include '$subprojectName'
        include '$dependentName'
        """
        def groupId = 'ch.tutteli'
        def version = '1.0.0-SNAPSHOT'
        def githubUser = 'robstoll'
        def vendor = 'tutteli.ch'
        def kotlinVersion = '1.2.71'
        def gpgPassphrase = 'bla'
        def gpgKeyId = 'A5875B96'
        def gpgKeyRing = 'keyring.gpg'

        settingsSetup.gpgKeyRing << PublishPluginIntTest.class.getResourceAsStream('/test-tutteli-gradle-plugin.gpg')

        settingsSetup.buildGradle << """
        buildscript {
            repositories { maven { url "https://plugins.gradle.org/m2/" } }
            dependencies {
                classpath 'ch.tutteli:tutteli-gradle-dokka:0.10.1'
                classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion'
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
            it.description = 'sub description' 
            
            repositories {  mavenCentral(); }
            apply plugin: 'kotlin'
            
            def testJar = task('testJar', type: Jar) {
                from sourceSets.test.output
                classifier = 'tests'
            } 
            
            apply plugin: 'ch.tutteli.publish'
         
            // not included in publish as it was defined after the publish plugin was applied
            
            def testSourcesJar = task('testSourcesJar', type: Jar) {
                from sourceSets.test.allSource
                classifier = 'testsources'
            }
         
            tutteliPublish {
                resetLicenses 'EUPL-1.2'

                //already defined because it is a ch.tutteli project
                //githubUser = '$githubUser'
                //bintrayRepo = 'tutteli-jars' is default no need to set it                
                //manifestVendor = $vendor // we don't have a manifestVendor, thus we reset it to null
                 
                /* would be required if we used task publishToBintray, we don't so we don't have to define it
                bintray {
                    user = 'test'
                    key = 'api-key'
                }
                */
            }            
         
            afterEvaluate {
                ${printArtifactsAndManifest()}
                ${printBintray()}
            }
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
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("pubToMaLo", "printSigning", "--stacktrace")
            .build()
        //assert
        String pom = Paths.get(settingsSetup.tmp.absolutePath, dependentName, 'build', 'publications', 'tutteli', 'pom-default.xml').toFile().getText('UTF-8')
        assertContainsRegex(pom, "name", "<name>$dependentName</name>")
        assertContainsRegex(pom, "description", "<description>sub description</description>")
        assertContainsRegex(pom, "licenses", "<licenses>$NL_INDENT" +
            "<license>$NL_INDENT" +
            "<name>${StandardLicenses.EUPL_1_2.longName}</name>$NL_INDENT" +
            "<url>${StandardLicenses.EUPL_1_2.url}</url>$NL_INDENT" +
            "<distribution>repo</distribution>$NL_INDENT" +
            "</license>$NL_INDENT" +
            "</licenses"
        )
        assertContainsRegex(pom, "developers", "<developers>$NL_INDENT" +
            "<developer>$NL_INDENT" +
            "<id>robstoll</id>$NL_INDENT" +
            "<name>Robert Stoll</name>$NL_INDENT" +
            "<email>rstoll@tutteli.ch</email>$NL_INDENT" +
            "<url>https://tutteli.ch</url>$NL_INDENT" +
            "</developer>$NL_INDENT" +
            "</developers>")
        assertContainsRegex(pom, "dependencies", "<dependencies>$NL_INDENT" +
            "<dependency>$NL_INDENT" +
            "<groupId>$groupId</groupId>$NL_INDENT" +
            "<artifactId>$subprojectNameWithoutJvm</artifactId>$NL_INDENT" +
            "<version>$version</version>$NL_INDENT" +
            "<scope>compile</scope>$NL_INDENT" +
            "</dependency>$NL_INDENT" +
            "<dependency>$NL_INDENT" +
            "<groupId>$groupId</groupId>$NL_INDENT" +
            "<artifactId>$subprojectNameWithoutJvm</artifactId>$NL_INDENT" +
            "<version>$version</version>$NL_INDENT" +
            "<scope>runtime</scope>$NL_INDENT" +
            "</dependency>$NL_INDENT" +
            "</dependencies>"
        )

        def repoUrl = "https://github.com/$githubUser/$rootProjectName"
        def output = result.output
        assertTrue(output.contains("groupId: $groupId"), "groupId $groupId\n${output}")
        assertTrue(output.contains("artifactId: $subprojectNameWithoutJvm"), "artifactId: $subprojectNameWithoutJvm\n${output}")
        assertTrue(output.contains("version: $version"), "version: $version\n${output}")
        assertTrue(output.contains("artifact: jar - null"), "java jar\n${output}")
        assertTrue(output.contains("artifact: jar - sources"), "sources jar\n${output}")
        assertTrue(output.contains("artifact: jar - tests"), "test jar \n${output}")
        assertFalse(output.contains("artifact: jar - testsources"), "testsources jar should not be in output, was defined after plugin apply\n${output}")
        assertBintray(result, "null", "null", rootProjectName, subprojectNameWithoutJvm, repoUrl, version, "EUPL-1.2", "null")
        assertSigning(result, gpgPassphrase, gpgKeyId, "${settingsSetup.tmpPath.toRealPath()}/$gpgKeyRing")
        assertTrue(result.output.contains("bintrayExtension.pkg.version.desc: " + dependentName + " $version"), "bintrayExtension.pkg.version.desc " + dependentName + " $version\n$result.output")

        assertJarOfSubProjectWithLicenseAndManifest(settingsSetup, subprojectName, "$subprojectNameWithoutJvm-${version}.jar", subprojectNameWithoutJvm, version, repoUrl, vendor, kotlinVersion)
        assertJarOfSubProjectWithLicenseAndManifest(settingsSetup, subprojectName, "$subprojectNameWithoutJvm-${version}-sources.jar", subprojectNameWithoutJvm, version, repoUrl, vendor, kotlinVersion)
        assertJarOfSubProjectWithLicenseAndManifest(settingsSetup, subprojectName, "$subprojectNameWithoutJvm-${version}-tests.jar", subprojectNameWithoutJvm, version, repoUrl, vendor, kotlinVersion)

        assertJarOfSubProjectWithLicenseAndManifest(settingsSetup, dependentName, "$dependentName-${version}.jar", dependentName, version, repoUrl, vendor, kotlinVersion)
        assertJarOfSubProjectWithLicenseAndManifest(settingsSetup, dependentName, "$dependentName-${version}-sources.jar", dependentName, version, repoUrl, vendor, kotlinVersion)
        assertJarOfSubProjectWithLicenseAndManifest(settingsSetup, dependentName, "$dependentName-${version}-tests.jar", dependentName, version, repoUrl, vendor, kotlinVersion)
    }

    @Test
    void kotlinApplied1_2_71(SettingsExtensionObject settingsSetup) throws IOException {
        kotlinApplied(settingsSetup, '1.2.71')
    }

    @Test
    void kotlinApplied1_3_31(SettingsExtensionObject settingsSetup) throws IOException {
        kotlinApplied(settingsSetup, '1.3.31')
    }

    private static void kotlinApplied(SettingsExtensionObject settingsSetup, String kotlinVersion) {
        def projectName = 'test-project'
        settingsSetup.settings << "rootProject.name='$projectName'"
        def version = '1.0.0-SNAPSHOT'
        def githubUser = 'robstoll'
        def gpgPassphrase = 'bla'
        def gpgKeyId = 'A5875B96'
        def gpgKeyRing = 'keyring.gpg'

        settingsSetup.gpgKeyRing << PublishPluginIntTest.class.getResourceAsStream('/test-tutteli-gradle-plugin.gpg')

        settingsSetup.buildGradle << """
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
            jcenter()
        }        

        apply plugin: 'kotlin'
        apply plugin: 'ch.tutteli.publish'
        
        project.with {
            group = 'com.example'
            version = '$version'
            description = 'test project'
        }
        tutteliPublish {
            //minimal setup required for local publish, all other things are only needed if not the default is used
            githubUser = '$githubUser'
            bintrayRepo = 'tutteli-jars'
        }
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
            .withArguments("publishTutteliPublicationToMavenLocal", "printSigning")
            .build()

        Asserts.assertStatusOk(result,
            [
                ":validateBeforePublish",
                ":includeBuildTimeInManifest",
                ":$PublishPlugin.TASK_GENERATE_POM".toString(),
                ":compileKotlin",
                ":processResources",
                ":classes",
                ":inspectClassesForKotlinIC",
                ":jar",
                ":sourcesJar",
                ":signTutteliPublication",
                ":publishTutteliPublicationToMavenLocal",
                ":printSigning"
            ],
            [],
            []
        )

        def repoUrl = "https://github.com/$githubUser/$projectName"
        assertJarWithLicenseAndManifest(settingsSetup, "$projectName-${version}.jar", projectName, version, repoUrl, null, kotlinVersion)
        assertJarWithLicenseAndManifest(settingsSetup, "$projectName-${version}-sources.jar", projectName, version, repoUrl, null, kotlinVersion)
        def zipFile = new ZipFile(buildLib(settingsSetup).resolve("$projectName-${version}-sources.jar").toFile())
        zipFile.withCloseable {
            assertInZipFile(zipFile, 'main/resources/a.txt')
            assertInZipFile(zipFile, 'main/kotlin/ch/tutteli/atrium/a.kt')
        }
        assertSigning(result, gpgPassphrase, gpgKeyId, gpgKeyRing)
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


    private static String printBintray() {
        return """def bintrayExtension = project.extensions.getByName('bintray')
            println("bintrayExtension.user: \$bintrayExtension.user")
            println("bintrayExtension.key: \$bintrayExtension.key")
            println("bintrayExtension.publications: \$bintrayExtension.publications")
            println("bintrayExtension.pkg.repo: \$bintrayExtension.pkg.repo")
            println("bintrayExtension.pkg.name: \$bintrayExtension.pkg.name")
            println("bintrayExtension.pkg.userOrg: \$bintrayExtension.pkg.userOrg")
            println("bintrayExtension.pkg.licenses: \${bintrayExtension.pkg.licenses.join(',')}")
            println("bintrayExtension.pkg.vcsUrl: \$bintrayExtension.pkg.vcsUrl")
            println("bintrayExtension.pkg.version.name: \$bintrayExtension.pkg.version.name")
            println("bintrayExtension.pkg.version.desc: \$bintrayExtension.pkg.version.desc")
            println("bintrayExtension.pkg.version.released: \$bintrayExtension.pkg.version.released")
            println("bintrayExtension.pkg.version.vcsTag: \$bintrayExtension.pkg.version.vcsTag")
            println("bintrayExtension.pkg.version.gpg.sign: \$bintrayExtension.pkg.version.gpg.sign")
            println("bintrayExtension.pkg.version.gpg.passphrase: \$bintrayExtension.pkg.version.gpg.passphrase")
        """
    }

    private static String printArtifactsAndManifest() {
        return """
        project.publishing.publications.withType(MavenPublication) {
            println("groupId: \$it.groupId")
            println("artifactId: \$it.artifactId")
            println("version: \$it.version")
            it.artifacts.each {
                println("artifact: \$it.extension - \$it.classifier")
            }
        }
        """
    }

    private static void assertBintray(
        BuildResult result,
        String user,
        String key,
        String pkgName,
        String projectName,
        String repoUrl,
        String version,
        String licenses,
        String organisation
    ) {
        assertTrue(result.output.contains("bintrayExtension.user: $user"), "bintrayExtension.user $user\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.key: $key"), "bintrayExtension.key $key\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.publications: [tutteli]"), "bintrayExtension.publications [tutteli]\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.repo: tutteli-jars"), "bintrayExtension.pkg.repo tutteli-jars\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.name: $pkgName"), "bintrayExtension.pkg.name $pkgName\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.userOrg: $organisation"), "bintrayExtension.pkg.userOrg $organisation\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.licenses: $licenses"), "bintrayExtension.pkg.licenses $licenses\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.vcsUrl: $repoUrl"), "bintrayExtension.pkg.vcsUrl $repoUrl\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.version.name: $version"), "bintrayExtension.pkg.version.name $version\n$result.output")

        assertTrue(result.output.contains("bintrayExtension.pkg.version.desc: " + projectName + " $version"), "bintrayExtension.pkg.version.desc " + pkgName + " $version\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.version.released: ${new Date().format('yyyy-MM-dd\'T\'HH:mm:ss.SSSZZ').substring(0, 10)}"), "bintrayExtension.pkg.version.released\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.version.vcsTag: v$version"), "bintrayExtension.pkg.version.vcsTag v$version\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.version.gpg.sign: false"), "bintrayExtension.pkg.version.gpg.sign false\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.version.gpg.passphrase: null"), "bintrayExtension.pkg.version.gpg.passphrase null\n$result.output")
    }

    private static void assertJarWithLicenseAndManifest(SettingsExtensionObject settingsSetup, String jarName, String projectName, String version, String repoUrl, String vendor, String kotlinVersion) {
        assertJarWithLicenseAndManifest(buildLib(settingsSetup), jarName, projectName, version, repoUrl, vendor, kotlinVersion)
    }

    private static Path buildLib(SettingsExtensionObject settingsSetup) {
        return Paths.get(settingsSetup.tmp.absolutePath, 'build', 'libs')
    }

    private static void assertJarOfSubProjectWithLicenseAndManifest(SettingsExtensionObject settingsSetup, String dirName, String jarName, String subprojectName, String version, String repoUrl, String vendor, String kotlinVersion) {
        assertJarWithLicenseAndManifest(Paths.get(settingsSetup.tmp.absolutePath, dirName, 'build', 'libs'), jarName, subprojectName, version, repoUrl, vendor, kotlinVersion)
    }

    private static void assertJarWithLicenseAndManifest(Path jarPath, String jarName, String projectName, String version, String repoUrl, String vendor, String kotlinVersion) {
        def zip = jarPath.resolve(jarName)
        def zipFile = new ZipFile(jarPath.resolve(jarName).toFile())
        zipFile.withCloseable {
            def manifest = zipFile.getInputStream(findInZipFile(zipFile, 'META-INF/MANIFEST.MF')).text
            assertManifest(manifest, ': ', projectName, version, repoUrl, vendor, kotlinVersion)
            assertTrue(manifest.contains("Build-Time: ${new Date().format('yyyy-MM-dd\'T\'HH:mm:ss.SSSZZ').substring(0, 10)}"), "manifest build time was not ${new Date().format('yyyy-MM-dd\'T\'HH:mm:ss.SSSZZ').substring(0, 10)}\n${manifest}")
            assertInZipFile(zipFile, 'LICENSE.txt')
        }
        def zipAsc = Paths.get(zip.toAbsolutePath().toString() + ".asc")
        assertTrue(zipAsc.toFile().exists(), "${zipAsc.toAbsolutePath()} does not exist")
    }

    private static void assertInZipFile(ZipFile zipFile, String path) {
        assertNotNull(findInZipFile(zipFile, path), "$path not found in $zipFile.name")
    }

    private static ZipEntry findInZipFile(ZipFile zipFile, String path) {
        return zipFile.entries().find {
            it.getName() == path
        } as ZipEntry
    }

    private static void assertManifest(String output, String separator, String projectName, String version, String repoUrl, String vendor, String kotlinVersion) {
        assertTrue(output.contains("Implementation-Title$separator$projectName"), "Implementation-Title$separator$projectName\n${output}")
        assertTrue(output.contains("Implementation-Version$separator$version"), "Implementation-Version$separator$version\n${output}")
        assertTrue(output.contains("Implementation-URL$separator$repoUrl"), "Implementatison-URL$separator$repoUrl\n${output}")
        if (vendor != null) {
            assertTrue(output.contains("Implementation-Vendor$separator$vendor"), "Implementation-Vendor$separator$vendor\n${output}")
        } else {
            assertFalse(output.contains("Implementation-Vendor"), "should not contain Implementation-Vendor")
        }
        assertTrue(output.contains("Implementation-Kotlin-Version$separator$kotlinVersion"), "Implementation-Kotlin-Version$separator$kotlinVersion\n${output}")
    }
}
