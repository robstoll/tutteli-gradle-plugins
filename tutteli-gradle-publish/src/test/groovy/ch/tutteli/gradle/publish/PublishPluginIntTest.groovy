package ch.tutteli.gradle.publish

import ch.tutteli.gradle.test.SettingsExtension
import ch.tutteli.gradle.test.SettingsExtensionObject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import static ch.tutteli.gradle.test.Asserts.assertContainsRegex
import static ch.tutteli.gradle.test.Asserts.getNL_INDENT
import static org.junit.jupiter.api.Assertions.assertTrue

@ExtendWith(SettingsExtension)
class PublishPluginIntTest {

    @Test
    void smokeTest(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        def projectName = 'test-project'
        settingsSetup.settings << "rootProject.name='$projectName'"
        def version = '1.0.0-SNAPSHOT'
        def githubUser = 'robstoll'

        File buildGradle = new File(settingsSetup.tmp, 'build.gradle')
        buildGradle << """
        buildscript {
            dependencies {
                classpath files($settingsSetup.pluginClasspath)
            }
        }
        // has to be before ch.tutteli.bintray if we want to have components automatically set up
        apply plugin: 'java' 
        apply plugin: 'ch.tutteli.publish'
        
        project.with {
            group = 'ch.tutteli'
            version = '$version'
            description = 'test project'
        }
        
        publish {
            githubUser = '$githubUser'

            //different ways to override the default license
            overrideDefaultLicense 'EUPL-1.2'             // default distribution is 'repo'
            overrideDefaultLicense 'EUPL-1.2', 'manually' 
            overrideDefaultLicense ch.tutteli.gradle.publish.StandardLicenses.EUPL_1_2
            overrideDefaultLicense ch.tutteli.gradle.publish.StandardLicenses.EUPL_1_2, 'manually'
            overrideDefaultLicense {
                shortName = 'Lic-1.2'
                longName = 'License 1.2'
                url = 'https://license.com'
                distribution = 'manually'
            }
            overrideDefaultLicense {
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
                url = 'tuteli.ch'
            }
            developer {
                id = 'robstoll_tutteli'
                name = 'Robert Stoll'
                email = 'rstoll@tutteli.ch'
                organization = 'tutteli'
                organizationUrl = 'tutteli.ch'
            }            
            
            //minimal setup required for bintray extension
            bintrayRepo = 'tutteli-jars'
            bintrayPkg = 'atrium'
            
            // you can customise the env variable names if they differ from the convention
            envNameBintrayUser = 'MY_BINTRAY_USER'                      // default is BINTRAY_USER
            envNameBintrayApiKey = 'MY_BINTRAY_API_KEY'                 // default is BINTRAY_API_KEY
            envNameBintrayGpgPassphrase = 'MY_BINTRAY_GPG_PASSPHRASE'   // default is BINTRAY_GPG_PASSPHRASE
            
            // you can also disable GPG signing (default is true)
            signWithGpg = false
            // yet, we will re-activate it for this test
            signWithGpg = true
            
            // you could configure JFrog's bintray extension here if you like.
            // There is no need for it though, everything can be configured via the above
            bintray {
                user = 'myUser'
            }     
        }        
        
         // you could also configure JFrog's bintray extension outside of tutteliBintray
         // but again, there is no need for it
        bintray {
            key = 'test'
            pkg.version.gpg.passphrase = 'pass'
        }  
        
        ${printInfos()}
        """
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("projects", "--stacktrace")
            .build()
        //assert
        assertContainsRegex(result.output, "licenses", "<licenses>$NL_INDENT" +
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

        assertContainsRegex(result.output, "developers", "<developers>$NL_INDENT" +
            "<developer>$NL_INDENT" +
            "<id>robstoll</id>$NL_INDENT" +
            "<name>Robert Stoll</name>$NL_INDENT" +
            "<email>rstoll@tutteli.ch</email>$NL_INDENT" +
            "<url>tuteli.ch</url>$NL_INDENT" +
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

        def repoUrl = "https://github.com/$githubUser/$projectName"
        assertContainsRegex(result.output, "scm url", "<scm>$NL_INDENT<url>$repoUrl</url>\r?\n\\s*</scm>")

        assertTrue(result.output.contains("bintrayExtension.user: myUser"), "bintrayExtension.user\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.key: test"), "bintrayExtension.key\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.publications: [tutteli]"), "bintrayExtension.publications\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.repo: tutteli-jars"), "bintrayExtension.pkg.repo\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.name: atrium"), "bintrayExtension.pkg.name\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.licenses: Apache-2.0,Apache-2.0,Apache-2.0,Lic-1.2"), "bintrayExtension.pkg.licenses\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.vcsUrl: $repoUrl"), "bintrayExtension.pkg.vcsUrl\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.version.name: $projectName"), "bintrayExtension.pkg.version.name\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.version.desc: atrium $version"), "bintrayExtension.pkg.version.desc\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.version.released: ${new Date().toTimestamp().toString().substring(0, 10)}"), "bintrayExtension.pkg.version.released\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.version.vcsTag: v$version"), "bintrayExtension.pkg.version.vcsTag\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.version.gpg.sign: true"), "bintrayExtension.pkg.version.gpg.sign\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.version.gpg.passphrase: pass"), "bintrayExtension.pkg.version.gpg.passphrase\n$result.output")
    }

    private static String printInfos() {
        """
        project.afterEvaluate {
            project.publishing.publications.withType(MavenPublication) {
                println(getPomAsString(it))
            }
            
            def bintrayExtension = project.extensions.getByName('bintray')
            println("bintrayExtension.user: \$bintrayExtension.user")
            println("bintrayExtension.key: \$bintrayExtension.key")
            println("bintrayExtension.publications: \$bintrayExtension.publications")
            println("bintrayExtension.pkg.repo: \$bintrayExtension.pkg.repo")
            println("bintrayExtension.pkg.name: \$bintrayExtension.pkg.name")
            println("bintrayExtension.pkg.licenses: \${bintrayExtension.pkg.licenses.join(',')}")
            println("bintrayExtension.pkg.vcsUrl: \$bintrayExtension.pkg.vcsUrl")
            println("bintrayExtension.pkg.version.name: \$bintrayExtension.pkg.version.name")
            println("bintrayExtension.pkg.version.desc: \$bintrayExtension.pkg.version.desc")
            println("bintrayExtension.pkg.version.released: \$bintrayExtension.pkg.version.released")
            println("bintrayExtension.pkg.version.vcsTag: \$bintrayExtension.pkg.version.vcsTag")
            println("bintrayExtension.pkg.version.gpg.sign: \$bintrayExtension.pkg.version.gpg.sign")
            println("bintrayExtension.pkg.version.gpg.passphrase: \$bintrayExtension.pkg.version.gpg.passphrase")
        }
        
        import org.apache.maven.model.Model
        import org.apache.maven.model.io.xpp3.MavenXpp3Writer
        import org.gradle.api.Action
        import org.gradle.api.UncheckedIOException
        import org.gradle.api.publish.maven.MavenPublication
        import org.gradle.api.publish.maven.internal.publication.MavenPomInternal
        import org.gradle.api.publish.maven.internal.tasks.MavenPomFileGenerator
        import org.gradle.internal.xml.XmlTransformer
        
        String getPomAsString(MavenPublication pub) {
            XmlTransformer transformer = new XmlTransformer()
            transformer.addAction((pub.pom as MavenPomInternal).xmlAction)
            StringWriter stringWriter = new StringWriter()
            transformer.transform(stringWriter, MavenPomFileGenerator.POM_FILE_ENCODING, new Action<Writer>() {
                void execute(Writer writer) {
                    try {
                        Model model = new Model()
                        new MavenXpp3Writer().write(writer, model)
                    } catch (IOException e) {
                        throw new UncheckedIOException(e)
                    }
                }
            })
            return stringWriter.toString()
        }
        """
    }


    @Test
    void combinePlugins(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        def projectName = 'test-project'
        settingsSetup.settings << "rootProject.name='$projectName'"
        def version = '1.0.0-SNAPSHOT'
        def githubUser = 'robstoll'
        def vendor = 'tutteli'
        def kotlinVersion = '1.2.71'

        File buildGradle = new File(settingsSetup.tmp, 'build.gradle')
        buildGradle << """
        buildscript {
            repositories { maven { url "https://plugins.gradle.org/m2/" } }
            dependencies {
                classpath 'ch.tutteli:tutteli-gradle-dokka:0.10.1'
                classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion'
                classpath files($settingsSetup.pluginClasspath)
            }
        }
        apply plugin: 'kotlin'
        apply plugin: 'ch.tutteli.dokka' 
        tutteliDokka.githubUser = '$githubUser'

        apply plugin: 'ch.tutteli.publish'
                
        project.with {
            group = 'ch.tutteli'
            version = '$version'
            description = 'test project'
        }
        
        publish {
            githubUser = '$githubUser'
            // Apache License 2.0 is the default
            // developers are optional
            
            // Optional, in case you want to mention the vendor in the manifest file of all jars
            manifestVendor = '$vendor'

            //minimal setup required for bintray extension
            bintrayRepo = 'tutteli-jars'
            bintrayPkg = 'atrium'  
            
            //required since we don't set the System.env variables
            bintray {
                user = 'test'
                key = 'api-key'
                pkg.version.gpg.sign = false
            }
        }        
        
        project.afterEvaluate {
            project.publishing.publications.withType(MavenPublication) {
                it.artifacts.each {
                    println("artifact: \$it.extension - \$it.classifier")
                }
            }
            project.tasks.withType(Jar) {
                it.manifest.attributes.each{
                    println(it)
                }
            }
        }
        """
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("projects", "--stacktrace")
            .build()
        //assert
        def repoUrl = "https://github.com/$githubUser/$projectName"
        assertTrue(result.output.contains("artifact: jar - null"), "java jar\n$result.output")
        assertTrue(result.output.contains("artifact: jar - sources"), "sources jar\n$result.output")
        assertTrue(result.output.contains("artifact: jar - javadoc"), "javadoc jar\n$result.output")
        assertTrue(result.output.contains("Implementation-Title=$projectName"), "manifest title was not $projectName\n$result.output")
        assertTrue(result.output.contains("Implementation-Version=$version"), "manifest version was not $version\n$result.output")
        assertTrue(result.output.contains("Implementation-URL=$repoUrl"), "manifest url was not $repoUrl\n$result.output")
        assertTrue(result.output.contains("Implementation-Vendor=$vendor"), "manifest vendor was not $vendor\n$result.output")
        assertTrue(result.output.contains("Implementation-Kotlin-Version=$kotlinVersion"), "manifest kotlin version was not $kotlinVersion\n$result.output")
    }
}
