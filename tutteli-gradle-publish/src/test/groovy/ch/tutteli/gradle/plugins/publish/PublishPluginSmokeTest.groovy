package ch.tutteli.gradle.plugins.publish

import org.apache.maven.model.Developer
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.MavenPomInternal
import org.junit.jupiter.api.Test

import static ch.tutteli.gradle.plugins.publish.SetUp.*
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNull

class PublishPluginSmokeTest {

    @Test
    void smokeTest_TasksAndExtensionPresent() {
        Project project = setUp()
        //assert
        assertExtensionAndTaskDefined(project)
        project.evaluate()
    }

    @Test
    void kotlin_TasksAndExtensionPresent() {
        //arrange & act
        Project project = setUp { project ->
            project.plugins.apply('kotlin')
        }
        project.evaluate()
        //assert
        assertExtensionAndTaskDefinedAfterEvaluate(project)
    }

    private static void assertExtensionAndTaskDefinedAfterEvaluate(Project project) {
        assertExtensionAndTaskDefined(project)
        project.tasks.getByName(PublishPlugin.TASK_GENERATE_POM)
        project.tasks.getByName(PublishPlugin.TASK_GENERATE_GRADLE_METADATA)
    }

    @Test
    void kotlinJs_TasksAndExtensionPresent() {
        //arrange & act
        Project project = setUp { project ->
            project.plugins.apply('kotlin2js')
        }
        project.evaluate()
        //assert
        assertExtensionAndTaskDefinedAfterEvaluate(project)
    }

    @Test
    void kotlinPlatformJvm_TasksAndExtensionPresent() {
        //arrange & act
        Project project = setUp { project ->
            project.plugins.apply('org.jetbrains.kotlin.jvm')
        }
        project.evaluate()
        //assert
        assertExtensionAndTaskDefinedAfterEvaluate(project)
    }

    @Test
    void kotlinOldPlatformJvm_TasksAndExtensionPresent() {
        //arrange & act
        Project project = setUp { project ->
            project.plugins.apply('kotlin-platform-jvm')
        }
        project.evaluate()
        //assert
        assertExtensionAndTaskDefinedAfterEvaluate(project)
    }

    @Test
    void kotlinPlatformJs_TasksAndExtensionPresent() {
        //arrange & act
        Project project = setUp { project ->
            project.plugins.apply('org.jetbrains.kotlin.js')
            project.extensions.configure('kotlin') {
                it.js {
                    nodejs()
                }
            }
        }
        project.evaluate()
        //assert
        assertExtensionAndTaskDefinedAfterEvaluate(project)
    }

    @Test
    void kotlinOldPlatformJs_TasksAndExtensionPresent() {
        //arrange & act
        Project project = setUp { project ->
            project.plugins.apply('kotlin-platform-js')
        }
        project.evaluate()
        //assert
        assertExtensionAndTaskDefinedAfterEvaluate(project)
    }

    @Test
    void kotlinOldPlatformCommon_TasksAndExtensionPresent() {
        //arrange & act
        Project project = setUp { project ->
            project.plugins.apply('kotlin-platform-common')
        }
        project.evaluate()
        //assert
        assertExtensionAndTaskDefinedAfterEvaluate(project)
    }

    @Test
    void kotlinMultiplatform_TasksAndExtensionPresent() {
        //arrange & act
        Project project = setUp { project ->
            project.plugins.apply('org.jetbrains.kotlin.multiplatform')
        }
        project.evaluate()
        //assert
        assertExtensionAndTaskDefined(project)
        assertNull(project.tasks.findByName(PublishPlugin.TASK_GENERATE_POM), "task ${PublishPlugin.TASK_GENERATE_POM} exists even though we use the new MPP plugin")

    }

    @Test
    void resetLicensesToEupl_LicenseEtcSetButNoDevelopersButTutteliProject_DeveloperSet() {
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
            assertPom(pub, StandardLicenses.EUPL_1_2, distribution)
            def pom = pub.pom as MavenPomInternal

            def developer = pom.developers.get(0)
            assertEquals("Robert Stoll", developer.name.get(), "dev name")
            assertEquals("rstoll@tutteli.ch", developer.email.get(), "dev email")
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
            assertPom(pub, StandardLicenses.APACHE_2_0, "repo")
        }
    }

    private void assertPom(MavenPublication pub, StandardLicenses standardLicense, String expectedDistribution) {
        def pom = pub.pom as MavenPomInternal
        assertEquals(pom.description.get(), DESCRIPTION, "description differs")
        def domainAndPath = "github.com/$GITHUB_USER/$ARTIFACT_ID"
        assertEquals(pom.url.get(), "https://$domainAndPath".toString(), "url differs")
        assertEquals(pom.licenses.size(), 1, "one license")
        def license = pom.licenses.get(0)


        assertEquals(standardLicense.longName, license.name.get(), "longname")
        assertEquals(standardLicense.url.toString(), license.url.get(), "url")
        assertEquals(expectedDistribution, license.distribution.get(), "distribution")
        assertEquals(pom.developers.size(), 1, "one developer")
        def developer = pom.developers.get(0)
        assertEquals(GITHUB_USER, developer.id.get(), "dev id")
        assertEquals("scm:git:git://${domainAndPath}.git".toString(), pom.scm.connection.get(), "scm connection")
        assertEquals("scm:git:ssh://${domainAndPath}.git".toString(), pom.scm.developerConnection.get(), "scm developerConnection")
        assertEquals("https://$domainAndPath".toString(), pom.scm.url.get(), "scm url")
    }

    private static void assertExtensionAndTaskDefined(Project project) {
        project.extensions.getByName(PublishPlugin.EXTENSION_NAME)
        project.tasks.getByName(PublishPlugin.TASK_NAME_INCLUDE_TIME)
        project.tasks.getByName(PublishPlugin.TASK_NAME_VALIDATE_PUBLISH)
    }

    private static PublishPluginExtension getPluginExtension(Project project) {
        return project.extensions.getByType(PublishPluginExtension)
    }

    private static void assertGroupIdArtifactIdAndVersion(MavenPublication pub) {
        assertEquals(GROUP_ID, pub.groupId)
        assertEquals(ARTIFACT_ID, pub.artifactId)
        assertEquals(VERSION, pub.version)
    }
}
