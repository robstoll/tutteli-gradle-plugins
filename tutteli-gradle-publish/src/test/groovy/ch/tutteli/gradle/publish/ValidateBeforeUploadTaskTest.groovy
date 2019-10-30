package ch.tutteli.gradle.publish

import org.gradle.api.Project
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

import static ch.tutteli.gradle.publish.SetUp.*
import static org.junit.jupiter.api.Assertions.*

class ValidateBeforeUploadTaskTest {

    @Test
    void evaluate_propUserNullAndEnvUserNullAndOnBintrayNotSet_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        getBintrayExtension(project).user = null
        //act && assert no exception
        project.evaluate()
        //act && assert
        assertThrowsIllegalState("property with name bintrayUser or System.env variable with name BINTRAY_USER") {
            validationTask(project).validate()
        }
    }

    @Test
    void evaluate_propUserNameChangedButNullAndEnvUserNameChangedButNullAndBintrayNotSet_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        def extension = getPluginExtension(project)
        extension.propNameBintrayUser.set('testName')
        getBintrayExtension(project).user = null
        //act && assert no exception
        project.evaluate()
        //act && assert
        assertThrowsIllegalState("property with name testName or System.env variable with name BINTRAY_USER") {
            validationTask(project).validate()
        }
    }

    @Test
    void evaluate_propUserNullAndEnvUserNameChangedButNullAndBintrayNotSet_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        def extension = getPluginExtension(project)
        extension.envNameBintrayUser.set('TEST')
        getBintrayExtension(project).user = null
        //act && assert no exception
        project.evaluate()
        //act && assert
        assertThrowsIllegalState("property with name bintrayUser or System.env variable with name TEST") {
            validationTask(project).validate()
        }
    }

    @Test
    void evaluate_propUserNullAndEnvUserNotSetButSetOnBintray_noError() {
        //arrange
        Project project = setUp()
        getBintrayExtension(project).user = 'test'
        //act && assert no exception
        project.evaluate()
        validationTask(project).validate()
    }

    @Test
    void evaluate_propUserSetAndEnvUserNullAndNotOnBintray_noError() {
        //arrange
        Project project = setUp()
        project.ext.bintrayUser = 'test'
        getBintrayExtension(project).user = null
        //act && assert no exception
        project.evaluate()
        validationTask(project).validate()
    }

    @Test
    void evaluate_propUserNameChangedAndSetAndEnvUserNullAndNotOnBintray_noError() {
        //arrange
        Project project = setUp()
        def propName = 'TEST_USER'
        getPluginExtension(project).propNameBintrayUser.set(propName)
        project.ext[propName] = 'test'
        getBintrayExtension(project).user = null
        //act && assert no exception
        project.evaluate()
        validationTask(project).validate()
    }

    @Test
    void evaluate_propApiKeyNullAndEnvApiKeyNullAndNotSetOnBintray_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        getBintrayExtension(project).key = null
        //act && assert no exception
        project.evaluate()
        //act && assert
        assertThrowsIllegalState("property with name bintrayApiKey or System.env variable with name BINTRAY_API_KEY") {
            validationTask(project).validate()
        }
    }

    @Test
    void evaluate_propApiKeyNameChangedButNullAndEnvApiKeyNullAndNotSetOnBintray_publishToBintrayThrowsIllegalStateException() {
        //arrange
        Project project = setUp()
        def extension = getPluginExtension(project)
        extension.propNameBintrayApiKey.set('testName')
        getBintrayExtension(project).key = null
        //act && assert no exception
        project.evaluate()
        //act && assert
        assertThrowsIllegalState("property with name testName or System.env variable with name BINTRAY_API_KEY") {
            validationTask(project).validate()
        }
    }

    @Test
    void evaluate_propApiKeyNullAndEnvApiKeyNameChangedButNullAndNotSetOnBintray_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        def extension = getPluginExtension(project)
        extension.envNameBintrayApiKey.set('TEST')
        getBintrayExtension(project).key = null
        //act && assert no exception
        project.evaluate()
        //act && assert
        assertThrowsIllegalState("property with name bintrayApiKey or System.env variable with name TEST") {
            validationTask(project).validate()
        }
    }

    @Test
    void evaluate_envApiKeyNotSetButSetOnBintray_noError() {
        //arrange
        Project project = setUp()
        getBintrayExtension(project).key = 'test'
        //act && assert no exception
        project.evaluate()
        validationTask(project).validate()
    }

    @Test
    void evaluate_propApiKeySetAndAndEnvApiKeyNullAndNotOnBintray_noError() {
        //arrange
        Project project = setUp()
        project.ext.bintrayApiKey = 'key'
        getBintrayExtension(project).key = null
        //act && assert no exception
        project.evaluate()
        validationTask(project).validate()
    }

    @Test
    void evaluate_propApiKeyNameChangedAndSetAndEnvApiKeyNullAndNotSetOnBintray_noError() {
        //arrange
        Project project = setUp()
        def propName = 'TEST_USER'
        getPluginExtension(project).propNameBintrayApiKey.set(propName)
        project.ext[propName] = 'test'
        getBintrayExtension(project).key = null
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

    private static ValidateBeforeUploadTask validationTask(Project project) {
        project.tasks.getByName(PublishPlugin.TASK_NAME_VALIDATE_UPLOAD) as ValidateBeforeUploadTask
    }

    private static String getExceptionMessage(String what) {
        return "You need to define $what for publishing (empty or blank is considered to be undefined)"
    }
}
