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
    void evaluate_artifactAndComponentNull_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        getPluginExtension(project).component.set(null)
        getPluginExtension(project).artifacts.set(null)
        //act && assert
        assertThrowsProjectConfigWithCauseIllegalStateNotDefined("either ${PublishPlugin.EXTENSION_NAME}.component or ${PublishPlugin.EXTENSION_NAME}.artifacts", project)
    }

    @Test
    void evaluate_artifactsEmptyAndComponentNull_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        getPluginExtension(project).component.set(null)
        getPluginExtension(project).artifacts.set(new ArrayList<>())
        //act && assert
        assertThrowsProjectConfigWithCauseIllegalStateNotDefined("either ${PublishPlugin.EXTENSION_NAME}.component or ${PublishPlugin.EXTENSION_NAME}.artifacts", project)
    }

    @Test
    void evaluate_artifactsNullButComponentSet_NoError() {
        //arrange
        Project project = setUp()
        getPluginExtension(project).component.set(project.components.getByName('java'))
        getPluginExtension(project).artifacts.set(null)
        //act && assert no exception
        project.evaluate()
    }

    @Test
    void evaluate_componentNullButArtifactsSet_NoError() {
        //arrange
        Project project = setUp()
        getPluginExtension(project).component.set(null)
        getPluginExtension(project).artifacts.add(project.tasks.getByName('jar'))
        //act && assert no exception
        project.evaluate()
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
    void evaluate_envNameBintrayUserUnset_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        getPluginExtension(project).envNameBintrayUser.set(null)
        //act && assert
        assertThrowsProjectConfigWithCauseIllegalStateNotDefined("${PublishPlugin.EXTENSION_NAME}.envNameBintrayUser", project)
    }

    @Test
    void evaluate_envNameBintrayApiKeyUnset_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        getPluginExtension(project).envNameBintrayApiKey.set(null)
        //act && assert
        assertThrowsProjectConfigWithCauseIllegalStateNotDefined("${PublishPlugin.EXTENSION_NAME}.envNameBintrayApiKey", project)
    }

    @Test
    void evaluate_envNameBintrayGpgPassphrase_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        getPluginExtension(project).envNameBintrayGpgPassphrase.set(null)
        //act && assert
        assertThrowsProjectConfigWithCauseIllegalStateNotDefined("${PublishPlugin.EXTENSION_NAME}.envNameBintrayGpgPassphrase", project)
    }

    @Test
    void evaluate_repoNullAndAlsoNotSetOnBintray_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        getPluginExtension(project).bintrayRepo.set(null)
        //act && assert
        assertThrowsProjectConfigWithCauseIllegalStateNotDefined("${PublishPlugin.EXTENSION_NAME}.bintrayRepo", project)
    }

    @Test
    void evaluate_repoBlankButSetOnBintray_noError() {
        //arrange
        Project project = setUp()
        getPluginExtension(project).bintrayRepo.set("  ")
        getBintrayExtension(project).pkg.repo = 'test'
        //act && assert no exception
        project.evaluate()
    }

    @Test
    void evaluate_pkgNullAndAlsoNotSetOnBintray_noErrorCorrespondsToProjectName() {
        //arrange
        Project project = setUp()
        getPluginExtension(project).bintrayPkg.set(null)
        //act && assert no exception
        project.evaluate()
        //assert
        assertEquals(project.name, getBintrayExtension(project).pkg.name)
    }

    @Test
    void evaluate_pkgEmptyButSetOnBintray_noError() {
        //arrange
        Project project = setUp()
        getPluginExtension(project).bintrayPkg.set("")
        getBintrayExtension(project).pkg.name = 'test'
        //act && assert no exception
        project.evaluate()
    }

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

    @Test
    void evaluate_propGpgPassphraseNullAndEnvGpgPassphraseNotSetButNoSign_noError() {
        //arrange
        Project project = setUp()
        getPluginExtension(project).signWithGpg.set(false)
        //act && assert no exception
        project.evaluate()
        validationTask(project).validate()
    }

    @Test
    void evaluate_propGpgPassphraseNullAndEnvGpgPassphraseNotSetButPublishNotCalled_noError() {
        //arrange
        Project project = setUp()
        //act && assert no exception
        project.evaluate()
        validationTask(project).validate()
    }

    @Test
    void evaluate_propGpgPassphraseNullAndEnvGpgPassphraseNotSetAndNoSignDefinedOnBintray_throwsIllegalStateExceptionBecauseBintrayIsIgnored() {
        //arrange
        Project project = setUp()
        getBintrayExtension(project).pkg.version.gpg.sign = false
        getBintrayExtension(project).pkg.version.gpg.passphrase = null
        //act && assert no exception
        project.evaluate()
        //act & assert
        assertThrowsIllegalState("property with name bintrayGpgPassphrase or System.env variable with name BINTRAY_GPG_PASSPHRASE") {
            validationTask(project).validate()
        }
    }

    @Test
    void evaluate_propGpgPassphraseNullAndEnvGpgPassphraseNotSetAndNotSetOnBintray_throwsIllegalStateExceptionBecauseDefaultSignIsTrue() {
        //arrange
        Project project = setUp()
        getBintrayExtension(project).pkg.version.gpg.passphrase = null
        //act && assert no exception
        project.evaluate()
        //act & assert
        assertThrowsIllegalState("property with name bintrayGpgPassphrase or System.env variable with name BINTRAY_GPG_PASSPHRASE") {
            validationTask(project).validate()
        }
    }

    @Test
    void evaluate_propGpgPassphraseNullAndEnvGpgPassphraseNotSetAndNotSetOnBintray_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        getPluginExtension(project).signWithGpg.set(true)
        getBintrayExtension(project).pkg.version.gpg.passphrase = null
        //act &&
        project.evaluate()
        //act && assert no exception
        assertThrowsIllegalState("property with name bintrayGpgPassphrase or System.env variable with name BINTRAY_GPG_PASSPHRASE") {
            validationTask(project).validate()
        }
    }

    @Test
    void evaluate_propGpgPassphraseNameChangedButNullAndEnvGpgPassphraseNullAndNotSetOnBintray_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        def extension = getPluginExtension(project)
        extension.propNameBintrayGpgPassphrase.set('testName')
        extension.signWithGpg.set(true)
        getBintrayExtension(project).pkg.version.gpg.passphrase = null
        //act && assert no exception
        project.evaluate()
        //act && assert
        assertThrowsIllegalState("property with name testName or System.env variable with name BINTRAY_GPG_PASSPHRASE") {
            validationTask(project).validate()
        }
    }

    @Test
    void evaluate_propGpgPassphraseNullAndEnvGpgPassphraseNameChangedButNullAndNotSetOnBintray_throwsIllegalStateException() {
        //arrange
        Project project = setUp()
        def extension = getPluginExtension(project)
        extension.envNameBintrayGpgPassphrase.set('TEST')
        extension.signWithGpg.set(true)
        getBintrayExtension(project).pkg.version.gpg.passphrase = null
        //act && assert no exception
        project.evaluate()
        //act && assert
        assertThrowsIllegalState("property with name bintrayGpgPassphrase or System.env variable with name TEST") {
            validationTask(project).validate()
        }
    }

    @Test
    void evaluate_propGpgPassphraseNullAndEnvGpgPassphraseNotSetButSetOnBintray_noError() {
        //arrange
        Project project = setUp()
        getPluginExtension(project).signWithGpg.set(true)
        getBintrayExtension(project).pkg.version.gpg.passphrase = "test"
        //act && assert no exception
        project.evaluate()
        validationTask(project).validate()
    }

    @Test
    void evaluate_propGpgPassphraseSetAndEnvGpgPassphraseNullAndNotOnBintray_noError() {
        //arrange
        Project project = setUp()
        project.ext.bintrayGpgPassphrase = "pass"
        getPluginExtension(project).signWithGpg.set(true)
        getBintrayExtension(project).pkg.version.gpg.passphrase = null
        //act && assert no exception
        project.evaluate()
        validationTask(project).validate()
    }

    @Test
    void evaluate_propGpgPassphraseNameChangedAndSetAndEnvGpgPassphraseNameNullAndNotOnBintray_noError() {
        //arrange
        Project project = setUp()
        def propName = 'bintrayPassphrase'
        getPluginExtension(project).propNameBintrayGpgPassphrase.set(propName)
        getPluginExtension(project).signWithGpg.set(true)
        project.ext[propName] = 'pass'
        getBintrayExtension(project).pkg.version.gpg.passphrase = null
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

    private static void assertThrowsProjectConfigWithCauseIllegalStateNotDefined(String what, Project project) {
        assertThrowsProjectConfigExceptionWithCause(IllegalStateException, getExceptionMessage(what)) {
            project.evaluate()
        }
    }

    private static ValidateBeforePublishTask validationTask(Project project) {
        project.tasks.getByName(PublishPlugin.TASK_NAME_VALIDATE) as ValidateBeforePublishTask
    }

    private static String getExceptionMessage(String what) {
        return "You need to define $what for publishing (empty or blank is considered to be undefined)"
    }
}
