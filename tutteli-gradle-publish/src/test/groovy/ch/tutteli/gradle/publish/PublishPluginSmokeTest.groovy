package ch.tutteli.gradle.publish


import com.jfrog.bintray.gradle.BintrayExtension
import org.apache.maven.model.Developer
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Writer
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.UncheckedIOException
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.MavenPomInternal
import org.gradle.api.publish.maven.internal.tasks.MavenPomFileGenerator
import org.gradle.internal.xml.XmlTransformer
import org.junit.jupiter.api.Test

import static ch.tutteli.gradle.publish.SetUp.*
import static ch.tutteli.gradle.test.Asserts.NL_INDENT
import static ch.tutteli.gradle.test.Asserts.assertContainsRegex
import static org.junit.jupiter.api.Assertions.*

class PublishPluginSmokeTest {

    @Test
    void smokeTest_TasksAndExtensionPresent() {
        //arrange & act
        Project project = setUp()
        //assert
        assertExtensionAndTaskDefined(project)
        project.evaluate()
        assertNotNull(project.extensions.getByType(BintrayExtension).user, "bintrayExtension.user")
        assertFalse(project.extensions.getByType(BintrayExtension).pkg.version.gpg.sign, "bintrayExtension.pkg.version.gpg.sign")
    }

    @Test
    void kotlin_TasksAndExtensionPresent() {

        //arrange & act
        Project project = setUp { project ->
            project.plugins.apply('kotlin')
        }
        project.evaluate()
        //assert
        assertExtensionAndTaskDefined(project)
    }

    @Test
    void kotlinJs_TasksAndExtensionPresent() {
        //arrange & act
        Project project = setUp { project ->
            project.plugins.apply('kotlin2js')
        }
        project.evaluate()
        //assert
        assertExtensionAndTaskDefined(project)
    }

    @Test
    void kotlinPlatformJvm_TasksAndExtensionPresent() {
        //arrange & act
        Project project = setUp { project ->
            project.plugins.apply('kotlin-platform-jvm')
        }
        project.evaluate()
        //assert
        assertExtensionAndTaskDefined(project)
    }

    @Test
    void kotlinPlatformJs_TasksAndExtensionPresent() {
        //arrange & act
        Project project = setUp { project ->
            project.plugins.apply('kotlin-platform-js')
        }
        project.evaluate()
        //assert
        assertExtensionAndTaskDefined(project)
    }

    @Test
    void kotlinPlatformCommon_TasksAndExtensionPresent() {
        //arrange & act
        Project project = setUp { project ->
            project.plugins.apply('kotlin-platform-common')
        }
        project.evaluate()
        //assert
        assertExtensionAndTaskDefined(project)
    }

    @Test
    void resetLicensesToEupl_LicenseEtcSetButNoDevelopers() {
        //arrange
        def distribution = 'someDistro'
        Project project = setUp()
        //act
        getPluginExtension(project).resetLicenses(StandardLicenses.EUPL_1_2, distribution)
        project.evaluate()
        //assert
        project.publishing.publications.withType(MavenPublication) {
            MavenPublication pub = it
            assertGroupIdArtifactIdAndVersion(pub)
            def pom = getPomAsString(pub)
            assertContainsRegex(pom, "description", "<description>$DESCRIPTION</description>")
            def repoUrl = "https://github.com/$GITHUB_USER/$ARTIFACT_ID"
            assertContainsRegex(pom, "url", "\n    <url>$repoUrl</url>")
            assertContainsRegex(pom, "license", "<licenses>$NL_INDENT<license>$NL_INDENT" +
                "<name>${StandardLicenses.EUPL_1_2.longName}</name>$NL_INDENT" +
                "<url>${StandardLicenses.EUPL_1_2.url}</url>$NL_INDENT" +
                "<distribution>$distribution</distribution>$NL_INDENT" +
                "</license>$NL_INDENT</licenses>"
            )
            assertContainsRegex(pom, "developers", "<developers>$NL_INDENT" +
                "<developer>$NL_INDENT" +
                "<id>robstoll</id>$NL_INDENT" +
                "<name>Robert Stoll</name>$NL_INDENT" +
                "<email>rstoll@tutteli.ch</email>$NL_INDENT" +
                "<url>https://tutteli.ch</url>$NL_INDENT" +
                "</developer>$NL_INDENT" +
                "</developers>")
            assertContainsRegex(pom, "scm url", "<scm>$NL_INDENT<url>$repoUrl</url>\r?\n\\s*</scm>")
        }
    }

    @Test
    void developerSet_LicenseApacheAndDevelopersSet() {
        //arrange
        Project project = setUp()
        //act
        def dev = new Developer()
        dev.id = GITHUB_USER
        getPluginExtension(project).developers.set([dev])
        project.evaluate()
        //assert
        project.publishing.publications.withType(MavenPublication) {
            MavenPublication pub = it
            assertGroupIdArtifactIdAndVersion(pub)
            def pom = getPomAsString(pub)
            assertContainsRegex(pom, "description", "<description>$DESCRIPTION</description>")
            def repoUrl = "https://github.com/$GITHUB_USER/$ARTIFACT_ID"
            assertContainsRegex(pom, "url", "\n    <url>$repoUrl</url>")
            assertContainsRegex(pom, "license", "<licenses>$NL_INDENT<license>$NL_INDENT" +
                "<name>${StandardLicenses.APACHE_2_0.longName}</name>$NL_INDENT" +
                "<url>${StandardLicenses.APACHE_2_0.url}</url>$NL_INDENT" +
                "<distribution>repo</distribution>$NL_INDENT" +
                "</license>$NL_INDENT</licenses>"
            )
            assertContainsRegex(pom, "developers", "<developers>$NL_INDENT<developer>$NL_INDENT" +
                "<id>$GITHUB_USER</id>$NL_INDENT" +
                "</developer>$NL_INDENT</developers>")
            assertContainsRegex(pom, "scm url", "<scm>$NL_INDENT<url>$repoUrl</url>\r?\n\\s*</scm>")
        }
    }

    private static void assertExtensionAndTaskDefined(Project project) {
        project.extensions.getByName(PublishPlugin.EXTENSION_NAME)
        project.tasks.getByName(PublishPlugin.TASK_NAME_INCLUDE_TIME)
        project.tasks.getByName(PublishPlugin.TASK_NAME_PUBLISH_TO_BINTRAY)
        project.tasks.getByName(PublishPlugin.TASK_NAME_SOURCES_JAR)
        project.tasks.getByName(PublishPlugin.TASK_NAME_SOURCES_JAR)
        project.tasks.getByName(PublishPlugin.TASK_NAME_VALIDATE_PUBLISH)
    }

    private static PublishPluginExtension getPluginExtension(Project project) {
        return project.extensions.getByName(PublishPlugin.EXTENSION_NAME) as PublishPluginExtension
    }

    private static String getPomAsString(MavenPublication pub) {
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

    private static void assertGroupIdArtifactIdAndVersion(MavenPublication pub) {
        assertEquals(GROUP_ID, pub.groupId)
        assertEquals(ARTIFACT_ID, pub.artifactId)
        assertEquals(VERSION, pub.version)
    }
}
