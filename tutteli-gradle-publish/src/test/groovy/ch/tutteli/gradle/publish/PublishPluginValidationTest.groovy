package ch.tutteli.gradle.publish

import org.gradle.api.Project
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

import static ch.tutteli.gradle.publish.SetUp.*
import static ch.tutteli.gradle.test.Asserts.assertThrowsProjectConfigExceptionWithCause
import static org.junit.jupiter.api.Assertions.*

class PublishPluginValidationTest {

    @Test
    void apply_noSourceSets_throwsIllegalStateException() {
        //arrange
        Project project = ProjectBuilder.builder()
            .withName(ARTIFACT_ID)
            .build()
        //act && assert
        def exception = assertThrows(PluginApplicationException) {
            project.plugins.apply(PublishPlugin)
        }
        //assert
        assertEquals(IllegalStateException, exception.cause.class)
        def message = "The project $project.name does not have any sources"
        assertTrue(exception.cause.message.contains(message), "cause.message contains $message:\n$exception.cause.message")
    }

    @Test
    void evaluate_versionUnspecified_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        project.version = "unspecified"
        //act && assert
        assertThrowsProjectConfigWithCauseIllegalStateNotDefined('project.version or rootProject.version', project)
    }

    @Test
    void evaluate_versionEmpty_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        project.version = ""
        //act && assert
        assertThrowsProjectConfigWithCauseIllegalStateNotDefined('project.version or rootProject.version', project)
    }

    @Test
    void evaluate_groupBlank_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        project.group = "  "
        //act && assert
        assertThrowsProjectConfigWithCauseIllegalStateNotDefined('project.group or rootProject.group', project)
    }

    @Test
    void evaluate_descriptionNull_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        project.description = null
        //act && assert
        assertThrowsProjectConfigWithCauseIllegalStateNotDefined('project.description', project)
    }

    @Test
    void evaluate_githubUserNotDefined_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        getPluginExtension(project).githubUser.set(null)
        //act && assert
        assertThrowsProjectConfigWithCauseIllegalStateNotDefined("${PublishPlugin.EXTENSION_NAME}.githubUser", project)
    }

    @Test
    void evaluate_licenseWithoutShortName_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        //act && assert
        assertThrowsIllegalState("${PublishPlugin.EXTENSION_NAME}.license.shortName") {
            getPluginExtension(project).license {
                longName = "test"
                url = "http"
            }
        }
    }

    @Test
    void evaluate_licenseLongNameEmpty_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        //act && assert
        assertThrowsIllegalState("${PublishPlugin.EXTENSION_NAME}.license.longName") {
            getPluginExtension(project).license {
                shortName = "test"
                longName = ""
                url = "http"
            }
        }
    }

    @Test
    void evaluate_licenseWithoutUrl_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        //act && assert
        assertThrowsIllegalState("${PublishPlugin.EXTENSION_NAME}.license.url") {
            getPluginExtension(project).license {
                shortName = "test"
                longName = "Test License"
            }
        }
    }

    @Test
    void evaluate_licenseDistributionSetToNull_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        //act && assert
        assertThrowsIllegalState("${PublishPlugin.EXTENSION_NAME}.license.distribution") {
            getPluginExtension(project).license {
                shortName = "test"
                longName = "Test License"
                url = "http"
                distribution = null
            }
        }
    }

    @Test
    void evaluate_envNameGpgPassphrase_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        getPluginExtension(project).envNameGpgPassphrase.set(null)
        //act && assert
        assertThrowsProjectConfigWithCauseIllegalStateNotDefined("${PublishPlugin.EXTENSION_NAME}.envNameGpgPassphrase", project)
    }
    @Test
    void evaluate_envNameGpgKeyId_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        getPluginExtension(project).envNameGpgKeyId.set(null)
        //act && assert
        assertThrowsProjectConfigWithCauseIllegalStateNotDefined("${PublishPlugin.EXTENSION_NAME}.envNameGpgKeyId", project)
    }
    @Test
    void evaluate_envNameGpgSecretKeyRingFile_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        getPluginExtension(project).envNameGpgKeyRing.set(null)
        //act && assert
        assertThrowsProjectConfigWithCauseIllegalStateNotDefined("${PublishPlugin.EXTENSION_NAME}.envNameGpgSecretKeyRingFile", project)
    }
    @Test
    void evaluate_envNameGpgSigningKey_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        getPluginExtension(project).envNameGpgSigningKey.set(null)
        //act && assert
        assertThrowsProjectConfigWithCauseIllegalStateNotDefined("${PublishPlugin.EXTENSION_NAME}.envNameGpgSigningKey", project)
    }

    @Test
    void evaluate_signIfNotSnapshot() {
        Project project = setUp()
        project.version = "1.0.0"

        project.evaluate()

        assertTrue(getPluginExtension(project).signWithGpg.get())
    }

    private static void assertThrowsIllegalState(String what, Executable executable) {
        def exception = assertThrows(IllegalStateException) {
            executable.execute()
        }
        assertEquals(getExceptionMessage(what), exception.message)
    }

    private static void assertThrowsProjectConfigWithCauseIllegalStateNotDefined(String what, Project project) {
        assertThrowsProjectConfigExceptionWithCause(IllegalStateException, getExceptionMessage(what)) {
            project.evaluate()
        }
    }

    private static String getExceptionMessage(String what) {
        return "You need to define $what for publishing (empty or blank is considered to be undefined)"
    }
}
