package ch.tutteli.gradle.publish


import ch.tutteli.gradle.test.Asserts
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

import static SetUp.*
import static ch.tutteli.gradle.test.Asserts.assertContainsRegex
import static org.junit.jupiter.api.Assertions.assertEquals

class BintrayPluginSmokeTest {

    @Test
    void overrideDefaultLicenseToEupl_LicenseEtcSetButNoDevelopers() {
        //arrange
        def distribution = 'someDistro'
        Project project = setUp()
        //act
        getPluginExtension(project).overrideDefaultLicense(StandardLicenses.EUPL_1_2, distribution)
        project.evaluate()
        //assert
        project.publishing.publications.withType(MavenPublication) {
            MavenPublication pub = it
            assertGroupIdArtifactIdAndVersion(pub)
            def pom = getPomAsString(pub)
            assertContainsRegex(pom, "description", "<description>$DESCRIPTION</description>")
            def repoUrl = "https://github.com/$GITHUB_USER/$ARTIFACT_ID"
            assertContainsRegex(pom, "url", "\n    <url>$repoUrl</url>")
            assertContainsRegex(pom, "license", "<licenses>$Asserts.NL_INDENT<license>$Asserts.NL_INDENT" +
                "<name>${StandardLicenses.EUPL_1_2.longName}</name>$Asserts.NL_INDENT" +
                "<url>${StandardLicenses.EUPL_1_2.url}</url>$Asserts.NL_INDENT" +
                "<distribution>$distribution</distribution>$Asserts.NL_INDENT" +
                "</license>$Asserts.NL_INDENT</licenses>"
            )
            assertContainsRegex(pom, "developers", "<developers/>")
            assertContainsRegex(pom, "scm url", "<scm>$Asserts.NL_INDENT<url>$repoUrl</url>\r?\n\\s*</scm>")
        }
    }

    @Test
    void developerSet_LicenseApacheAndDevelopersSet() {
        //arrange
        Project project = setUp()
        //act
        getPluginExtension(project).developer {
            id GITHUB_USER
        }
        project.evaluate()
        //assert
        project.publishing.publications.withType(MavenPublication) {
            MavenPublication pub = it
            assertGroupIdArtifactIdAndVersion(pub)
            def pom = getPomAsString(pub)
            assertContainsRegex(pom, "description", "<description>$DESCRIPTION</description>")
            def repoUrl = "https://github.com/$GITHUB_USER/$ARTIFACT_ID"
            assertContainsRegex(pom, "url", "\n    <url>$repoUrl</url>")
            assertContainsRegex(pom, "license", "<licenses>$Asserts.NL_INDENT<license>$Asserts.NL_INDENT" +
                "<name>${StandardLicenses.APACHE_2_0.longName}</name>$Asserts.NL_INDENT" +
                "<url>${StandardLicenses.APACHE_2_0.url}</url>$Asserts.NL_INDENT" +
                "<distribution>repo</distribution>$Asserts.NL_INDENT" +
                "</license>$Asserts.NL_INDENT</licenses>"
            )
            assertContainsRegex(pom, "developers", "<developers>$Asserts.NL_INDENT<developer>$Asserts.NL_INDENT" +
                "<id>$GITHUB_USER</id>$Asserts.NL_INDENT" +
                "</developer>$Asserts.NL_INDENT</developers>")
            assertContainsRegex(pom, "scm url", "<scm>$Asserts.NL_INDENT<url>$repoUrl</url>\r?\n\\s*</scm>")
        }
    }


    private static BintrayPluginExtension getPluginExtension(Project project) {
        return project.extensions.getByName(BintrayPlugin.EXTENSION_NAME) as BintrayPluginExtension
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
