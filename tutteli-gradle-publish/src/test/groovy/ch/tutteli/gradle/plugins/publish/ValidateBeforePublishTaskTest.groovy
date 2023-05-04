package ch.tutteli.gradle.plugins.publish

import org.gradle.api.Project
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

import static ch.tutteli.gradle.plugins.publish.SetUp.getPluginExtension
import static ch.tutteli.gradle.plugins.publish.SetUp.setUp
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

class ValidateBeforePublishTaskTest {

    @Test
    void evaluate_allGpgPropsNullAndEnvsNotSetButNoSign_noError() {
        //arrange
        Project project = setUp()
        project.ext.gpgPassphrase = null
        project.ext.gpgSecretFile = null
        project.ext.gpgKeyId = null
        getPluginExtension(project).signWithGpg.set(false)
        //act && assert no exception
        project.evaluate()
        validationTask(project).validate()
    }

    @Test
    void evaluate_allGpgPropsNullAndEnvsNoSetButPublishNotCalled_noError() {
        //arrange
        Project project = setUp()
        project.ext.gpgPassphrase = null
        project.ext.gpgSecretFile = null
        project.ext.gpgKeyId = null
        project.ext.gpgPassphrase = null
        //act && assert no exception
        project.evaluate()
    }

    @Test
    void evaluate_propGpgPassphraseNullAndEnvGpgPassphraseNotSet_throwsIllegalStateExceptionBecauseDefaultSignIsTrue() {
        //arrange
        Project project = setUp()
        project.ext.gpgPassphrase = null
        //act && assert no exception
        project.evaluate()
        //act & assert
        assertThrowsIllegalState("property with name gpgPassphrase or System.env variable with name GPG_PASSPHRASE") {
            validationTask(project).validate()
        }
    }

    @Test
    void evaluate_propGpgPassphraseNullAndEnvGpgPassphraseNotSet_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        project.ext.gpgPassphrase = null
        getPluginExtension(project).signWithGpg.set(true)
        //act &&
        project.evaluate()
        //act && assert no exception
        assertThrowsIllegalState("property with name gpgPassphrase or System.env variable with name GPG_PASSPHRASE") {
            validationTask(project).validate()
        }
    }

    @Test
    void evaluate_propGpgPassphraseNameChangedButNullAndEnvGpgPassphraseNull_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        project.ext.gpgPassphrase = null
        def extension = getPluginExtension(project)
        extension.propNameGpgPassphrase.set('testName')
        extension.signWithGpg.set(true)
        //act && assert no exception
        project.evaluate()
        //act && assert
        assertThrowsIllegalState("property with name testName or System.env variable with name GPG_PASSPHRASE") {
            validationTask(project).validate()
        }
    }

    @Test
    void evaluate_propGpgPassphraseNullAndEnvGpgPassphraseNameChangedButNull_ignoresBintrayAndThrowsIllegalStateException() {
        //arrange
        Project project = setUp()
        project.gpgPassphrase = null
        def extension = getPluginExtension(project)
        extension.envNameGpgPassphrase.set('TEST')
        extension.signWithGpg.set(true)
        //act && assert no exception
        project.evaluate()
        //act && assert
        assertThrowsIllegalState("property with name gpgPassphrase or System.env variable with name TEST") {
            validationTask(project).validate()
        }
    }

    @Test
    void evaluate_propGpgPassphraseNullAndEnvGpgPassphraseNotSet_noError() {
        //arrange
        Project project = setUp()
        getPluginExtension(project).signWithGpg.set(true)
        //act && assert no exception
        project.evaluate()
        validationTask(project).validate()
    }

    @Test
    void evaluate_propGpgPassphraseSetAndEnvGpgPassphraseNull_noError() {
        //arrange
        Project project = setUp()
        project.ext.gpgPassphrase = "pass"
        getPluginExtension(project).signWithGpg.set(true)
        //act && assert no exception
        project.evaluate()
        validationTask(project).validate()
    }

    @Test
    void evaluate_propGpgPassphraseNameChangedAndSetAndEnvGpgPassphraseNameNull_noError() {
        //arrange
        Project project = setUp()
        def propName = 'myPassphrase'
        getPluginExtension(project).propNameGpgPassphrase.set(propName)
        getPluginExtension(project).signWithGpg.set(true)
        project.ext[propName] = 'pass'
        //act && assert no exception
        project.evaluate()
        validationTask(project).validate()
    }

    private static void assertThrowsIllegalState(String what, Executable executable) {
        def exception = assertThrows(IllegalStateException) {
            executable.execute()
        }
        assertEquals(getExceptionMessage(what), exception.message)
    }

    private static ValidateBeforePublishTask validationTask(Project project) {
        project.tasks.getByName(PublishPlugin.TASK_NAME_VALIDATE_PUBLISH) as ValidateBeforePublishTask
    }

    private static String getExceptionMessage(String what) {
        return "You need to define $what for publishing (empty or blank is considered to be undefined)"
    }
}
