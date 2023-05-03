package ch.tutteli.gradle.plugins.publish

import org.gradle.api.Project
import org.junit.jupiter.api.Test

import static ch.tutteli.gradle.plugins.publish.SetUp.*
import static ch.tutteli.gradle.plugins.test.Asserts.assertThrowsProjectConfigExceptionWithCause
import static org.junit.jupiter.api.Assertions.*

class PublishPluginValidationTest {

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
    void evaluate_licensesEmpty_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        getPluginExtension(project).licenses.set(Collections.emptyList())
        //act && assert
        assertThrowsProjectConfigWithCauseIllegalStateNotDefined("${PublishPlugin.EXTENSION_NAME}.licenses", project)
    }

    @Test
    void evaluate_setEnvNameGpgPassphraseToNull_usesConvention() {
        //arrange
        Project project = setUp()
        getPluginExtension(project).envNameGpgPassphrase.set(null)
        //act
        project.evaluate()
        //assert
        assertEquals("GPG_PASSPHRASE", getPluginExtension(project).envNameGpgPassphrase.get())
    }

    @Test
    void evaluate_setEnvNameGpgKeyIdToNull_usesConvention() {
        Project project = setUp()
        getPluginExtension(project).envNameGpgKeyId.set(null)

        project.evaluate()
        assertEquals("GPG_KEY_ID", getPluginExtension(project).envNameGpgKeyId.get())
    }
    @Test
    void evaluate_setEnvNameGpgSecretKeyRingFileToNull_usesConvention() {
        Project project = setUp()
        getPluginExtension(project).envNameGpgKeyRing.set(null)

        project.evaluate()
        assertEquals("GPG_KEY_RING", getPluginExtension(project).envNameGpgKeyRing.get())
    }
    @Test
    void evaluate_setEnvNameGpgSigningKeyToNull_usesConvention() {
        Project project = setUp()
        getPluginExtension(project).envNameGpgSigningKey.set(null)

        project.evaluate()
        assertEquals("GPG_SIGNING_KEY", getPluginExtension(project).envNameGpgSigningKey.get())
    }

    @Test
    void evaluate_signIfNotSnapshot() {
        Project project = setUp()
        project.version = "1.0.0"

        project.evaluate()

        assertTrue(getPluginExtension(project).signWithGpg.get())
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
