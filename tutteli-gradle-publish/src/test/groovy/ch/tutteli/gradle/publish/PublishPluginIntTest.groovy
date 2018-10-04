package ch.tutteli.gradle.publish

import ch.tutteli.gradle.test.SettingsExtension
import ch.tutteli.gradle.test.SettingsExtensionObject
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

import static ch.tutteli.gradle.test.Asserts.assertContainsRegex
import static ch.tutteli.gradle.test.Asserts.getNL_INDENT
import static org.junit.jupiter.api.Assertions.assertFalse
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
        def user = 'myUser'
        def apiKey = 'test'
        def pkgName = "tutteli-gradle"

        File buildGradle = new File(settingsSetup.tmp, 'build.gradle')
        buildGradle << """
        buildscript {
            dependencies {
                classpath files($settingsSetup.pluginClasspath)
            }
        }
        // has to be before ch.tutteli.publish
        apply plugin: 'java' 
        apply plugin: 'ch.tutteli.publish'
        
        project.with {
            group = 'ch.tutteli'
            version = '$version'
            description = 'test project'
        }
        
        publish {
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
                url = 'tuteli.ch'
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
            // you can customise the env variable names if they differ from the convention
            propNameBintrayUser = 'myBintrayUser'                       // default is bintrayUser
            propNameBintrayApiKey = 'myBintrayApiKey'                   // default is bintrayApiKey
            propNameBintrayGpgPassphrase = 'myBintrayGpgPassphrase'     // default is bintrayGpgPassphrase
            envNameBintrayUser = 'MY_BINTRAY_USER'                      // default is BINTRAY_USER
            envNameBintrayApiKey = 'MY_BINTRAY_API_KEY'                 // default is BINTRAY_API_KEY
            envNameBintrayGpgPassphrase = 'MY_BINTRAY_GPG_PASSPHRASE'   // default is BINTRAY_GPG_PASSPHRASE
            
            // you can also disable GPG signing (default is true)
            signWithGpg = false
            // yet, we will re-activate it for this test
            signWithGpg = true
            
            // you could configure JFrog's bintray extension here if you like.
            // There is no need for it though, everything can be configured via the above
            // do not try to disable sign, will be overwritten by signWithGpg
            bintray {
                user = '$user'
            }     
        }        
        
         // you could also configure JFrog's bintray extension outside of publish
         // but again, there is no need for it.
        bintray {
            key = '$apiKey'
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
        assertTrue(result.output.contains("Some licenses were duplicated. Please check if you made a mistake."), "should contain warning about duplicated licenses")
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
        assertBintray(result, user, apiKey, pkgName, repoUrl, version, "Apache-2.0,Lic-1.2", "pass")
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
        def user = 'test-user'
        def apiKey = 'test-key'

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
        repositories {  mavenCentral(); }
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
            
            //required since we don't set the System.env variables
            bintray {
                user = '$user'
                key = '$apiKey'
                pkg.version.gpg.passphrase = 'pass'
            }
        }        
        
        project.afterEvaluate {
            ${printArtifactsAndManifest()}
            ${printBintray()}
        }
        """
        File license = new File(settingsSetup.tmp, 'LICENSE.txt')
        license << "Copyright..."
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("includeBuildTimeInManifest", "jar", "javadocJar", "sourcesJar", "--stacktrace")
            .build()
        //assert
        def repoUrl = "https://github.com/$githubUser/$projectName"
        def output = result.output
        assertTrue(output.contains("artifact: jar - null"), "java jar\n${output}")
        assertTrue(output.contains("artifact: jar - sources"), "sources jar\n${output}")
        assertTrue(output.contains("artifact: jar - javadoc"), "javadoc jar\n${output}")
        assertContainsRegex(output, "task jar", "task ':jar'\r?\nManifest-Version=1.0\r?\nImplementation")
        assertContainsRegex(output, "task sourcesJar", "task ':sourcesJar'\r?\nManifest-Version=1.0\r?\nImplementation")
        assertContainsRegex(output, "task javadocJar", "task ':javadocJar'\r?\nManifest-Version=1.0\r?\nImplementation")
        assertManifest(output, '=', projectName, version, repoUrl, vendor, kotlinVersion)
        assertJarWithLicenseAndManifest(settingsSetup, "$projectName-${version}.jar", projectName, version, repoUrl, vendor, kotlinVersion)
        assertJarWithLicenseAndManifest(settingsSetup, "$projectName-${version}-sources.jar", projectName, version, repoUrl, vendor, kotlinVersion)
        assertJarWithLicenseAndManifest(settingsSetup, "$projectName-${version}-javadoc.jar", projectName, version, repoUrl, vendor, kotlinVersion)
        assertBintray(result, user, apiKey, projectName, repoUrl, version, "Apache-2.0", "pass")
    }

    @Test
    void subproject(SettingsExtensionObject settingsSetup) throws IOException {
        //arrange
        def projectName = 'test-project'
        def subprojectName = 'test-sub'
        settingsSetup.settings << """rootProject.name='$projectName'
        include '$subprojectName'
        """
        def version = '1.0.0-SNAPSHOT'
        def githubUser = 'robstoll'
        def vendor = null
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
                
        project.with {
            group = 'ch.tutteli'
            version = '$version'
        }     
   
        subprojects {
            it.description = 'sub description' 
            
            repositories {  mavenCentral(); }
            apply plugin: 'kotlin'
            apply plugin: 'ch.tutteli.publish'
         
            publish {
                resetLicenses 'EUPL-1.2'
                //minimal setup required for bintray extension
                githubUser = '$githubUser'

                //bintrayRepo = 'tutteli-jars' is default no need to set it                
                manifestVendor = $vendor // we don't have a manifestVendor, thus we reset it to null
                 
                /* would be required if we used task publishToBintray, we don't so we don't have to define it
                bintray {
                    user = 'test'
                    key = 'api-key'
                    passphrase = 'pass'
                }
                */
            }            
         
            afterEvaluate {
                ${printArtifactsAndManifest()}
                ${printBintray()}
            }
        }
        """
        File license = new File(settingsSetup.tmp, 'LICENSE.txt')
        license << "Copyright..."
        //act
        def result = GradleRunner.create()
            .withProjectDir(settingsSetup.tmp)
            .withArguments("includeBuildTimeInManifest", "jar", "sourcesJar", "--stacktrace")
            .build()
        //assert
        def repoUrl = "https://github.com/$githubUser/$projectName"
        def output = result.output
        assertTrue(output.contains("artifact: jar - null"), "java jar\n${output}")
        assertTrue(output.contains("artifact: jar - sources"), "sources jar\n${output}")
        assertContainsRegex(output, "task jar", "task ':test-sub:jar'\r?\nManifest-Version=1.0\r?\nImplementation")
        assertContainsRegex(output, "task sourcesJar", "task ':test-sub:sourcesJar'\r?\nManifest-Version=1.0\r?\nImplementation")
        assertManifest(output, '=', subprojectName, version, repoUrl, vendor, kotlinVersion)
        assertJarOfSubprojectWithLicenseAndManifest(settingsSetup, "$subprojectName-${version}.jar", subprojectName, version, repoUrl, vendor, kotlinVersion)
        assertJarOfSubprojectWithLicenseAndManifest(settingsSetup, "$subprojectName-${version}-sources.jar", subprojectName, version, repoUrl, vendor, kotlinVersion)
        assertBintray(result, "null", "null", projectName, repoUrl, version, "EUPL-1.2", "null")
    }

    private static String printInfos() {
        """
        project.afterEvaluate {
            project.publishing.publications.withType(MavenPublication) {
                println(getPomAsString(it))
            }
            
            ${printBintray()}
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

    private static String printBintray() {
        return """def bintrayExtension = project.extensions.getByName('bintray')
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
        """
    }

    private static String printArtifactsAndManifest() {
        return """project.publishing.publications.withType(MavenPublication) {
            it.artifacts.each {
                println("artifact: \$it.extension - \$it.classifier")
            }
        }
        project.tasks.withType(org.gradle.jvm.tasks.Jar) {
            println(it)
            it.manifest.attributes.each{
                println(it)
            }
            println("----")
        }"""
    }

    private static void assertBintray(BuildResult result, String user, String key, String pkgName, String repoUrl, String version, String licenses, String passphrase) {
        assertTrue(result.output.contains("bintrayExtension.user: $user"), "bintrayExtension.user $user\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.key: $key"), "bintrayExtension.key $key\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.publications: [tutteli]"), "bintrayExtension.publications [tutteli]\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.repo: tutteli-jars"), "bintrayExtension.pkg.repo tutteli-jars\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.name: $pkgName"), "bintrayExtension.pkg.name $pkgName\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.licenses: $licenses"), "bintrayExtension.pkg.licenses $licenses\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.vcsUrl: $repoUrl"), "bintrayExtension.pkg.vcsUrl $repoUrl\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.version.name: $version"), "bintrayExtension.pkg.version.name $version\n$result.output")

        assertTrue(result.output.contains("bintrayExtension.pkg.version.desc: " + pkgName + " $version"), "bintrayExtension.pkg.version.desc " + pkgName + " $version\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.version.released: ${new Date().format('yyyy-MM-dd\'T\'HH:mm:ss.SSSZZ').substring(0, 10)}"), "bintrayExtension.pkg.version.released\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.version.vcsTag: v$version"), "bintrayExtension.pkg.version.vcsTag v$version\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.version.gpg.sign: true"), "bintrayExtension.pkg.version.gpg.sign true\n$result.output")
        assertTrue(result.output.contains("bintrayExtension.pkg.version.gpg.passphrase: $passphrase"), "bintrayExtension.pkg.version.gpg.passphrase pass\n$result.output")
    }

    private static void assertJarWithLicenseAndManifest(SettingsExtensionObject settingsSetup, String jarName, String projectName, String version, String repoUrl, String vendor, String kotlinVersion) {
        assertJarWithLicenseAndManifest(Paths.get(settingsSetup.tmp.absolutePath, 'build', 'libs'), jarName, projectName, version, repoUrl, vendor, kotlinVersion)
    }

    private static void assertJarOfSubprojectWithLicenseAndManifest(SettingsExtensionObject settingsSetup, String jarName, String subprojectName, String version, String repoUrl, String vendor, String kotlinVersion) {
        assertJarWithLicenseAndManifest(Paths.get(settingsSetup.tmp.absolutePath, subprojectName, 'build', 'libs'), jarName, subprojectName, version, repoUrl, vendor, kotlinVersion)
    }

    private static void assertJarWithLicenseAndManifest(Path jarPath, String jarName, String projectName, String version, String repoUrl, String vendor, String kotlinVersion) {
        def zipFile = new ZipFile(jarPath.resolve(jarName).toFile())
        zipFile.withCloseable {
            assertTrue(zipFile.entries().any { it.getName() == 'LICENSE.txt' }, "did not find LICENSE.txt in jar")
            def manifest = zipFile.getInputStream(zipFile.entries().find {
                it.getName() == 'META-INF/MANIFEST.MF'
            } as ZipEntry).text
            assertManifest(manifest, ': ', projectName, version, repoUrl, vendor, kotlinVersion)
            assertTrue(manifest.contains("Build-Time: ${new Date().format('yyyy-MM-dd\'T\'HH:mm:ss.SSSZZ').substring(0, 10)}"), "manifest build time was not ${new Date().format('yyyy-MM-dd\'T\'HH:mm:ss.SSSZZ').substring(0, 10)}\n${manifest}")
        }
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
