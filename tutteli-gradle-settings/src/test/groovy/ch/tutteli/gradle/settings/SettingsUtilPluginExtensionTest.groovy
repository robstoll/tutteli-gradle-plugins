package ch.tutteli.gradle.settings

import ch.tutteli.gradle.test.SettingsExtension
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import java.nio.file.Files
import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue
import static org.mockito.Mockito.*

class SettingsUtilPluginExtensionTest {
    private Path tmp
    private File tmpDir
    private rootProjectName = "test"
    private settings = mock(Settings)
    private folderName = 'myFolder'

    @BeforeEach
    void setUp() {
        //arrange
        tmp = Files.createTempDirectory("myTests")
        tmpDir = tmp.toFile()
        def descriptor = mock(ProjectDescriptor)
        when(settings.rootProject).thenReturn(descriptor)
        when(descriptor.projectDir).thenReturn(tmpDir)
        when(descriptor.name).thenReturn(rootProjectName)
    }

    @AfterEach
    void tearDown() {
        SettingsExtension.deleteTmp(tmp)
    }


    @Test
    void prefixed_oneProjectAndNotInFolder_includedInRoot() {
        //arrange
        def nameA = 'test-a'
        def (descriptorA, projectDirA) = setUpProject(nameA)
        def testee = new SettingsUtilPluginExtension(settings, '')
        //act
        testee.prefixed('a')
        //assert
        verifyProjectIncluded(nameA, descriptorA, projectDirA)
    }

    @Test
    void prefixed_twoProjectsAndNotInFolder_bothIncludedInRoot() {
        //arrange
        def nameA = 'test-a'
        def nameB = 'test-b'
        def (descriptorA, projectDirA) = setUpProject(nameA)
        def (descriptorB, projectDirB) = setUpProject(nameB)
        def testee = new SettingsUtilPluginExtension(settings, '')
        //act
        testee.prefixed('a', 'b')
        //assert
        verifyProjectIncluded(nameA, descriptorA, projectDirA)
        verifyProjectIncluded(nameB, descriptorB, projectDirB)
    }

    @Test
    void project_oneProjectAndNotInFolder_includedInRoot() {
        //arrange
        def nameA = 'a'
        def (descriptorA, projectDirA) = setUpProject(tmpDir, nameA)
        def testee = new SettingsUtilPluginExtension(settings, '')
        //act
        testee.project(nameA)
        //assert
        verifyProjectIncluded(nameA, descriptorA, projectDirA)
    }

    @Test
    void project_twoProjectsAndNotInFolder_bothIncludedInRoot() {
        //arrange
        def nameA = 'a'
        def nameB = 'b'
        def (descriptorA, projectDirA) = setUpProject(tmpDir, nameA)
        def (descriptorB, projectDirB) = setUpProject(tmpDir, nameB)
        def testee = new SettingsUtilPluginExtension(settings, '')
        //act
        testee.project(nameA, nameB)
        //assert
        verifyProjectIncluded(nameA, descriptorA, projectDirA)
        verifyProjectIncluded(nameB, descriptorB, projectDirB)
    }

    @Test
    void prefixed_oneProjectInFolder_includedInFolder() {
        //arrange
        def folder = new File(tmpDir, folderName)
        folder.mkdir()
        def nameA = 'test-a'
        def (descriptorA, projectDirA) = setUpProject(folder, nameA)
        def testee = new SettingsUtilPluginExtension(settings, folderName)
        //act
        testee.prefixed('a')
        //assert
        verifyProjectIncluded(nameA, descriptorA, projectDirA)
    }

    @Test
    void prefixed_twoProjectsInFolder_bothIncludedInFolder() {
        //arrange
        def folder = new File(tmpDir, folderName)
        folder.mkdir()
        def nameA = 'test-a'
        def nameB = 'test-b'
        def (descriptorA, projectDirA) = setUpProject(folder, nameA)
        def (descriptorB, projectDirB) = setUpProject(folder, nameB)
        def testee = new SettingsUtilPluginExtension(settings, folderName)
        //act
        testee.prefixed('a', 'b')
        //assert
        verifyProjectIncluded(nameA, descriptorA, projectDirA)
        verifyProjectIncluded(nameB, descriptorB, projectDirB)
    }

    @Test
    void project_oneProjectInFolder_includedInFolderWithoutPrefix() {
        //arrange
        def folder = new File(tmpDir, folderName)
        folder.mkdir()
        def nameA = 'a'
        def (descriptorA, projectDirA) = setUpProject(folder, nameA)
        def testee = new SettingsUtilPluginExtension(settings, folderName)
        //act
        testee.project(nameA)
        //assert
        verifyProjectIncluded(nameA, descriptorA, projectDirA)
    }

    @Test
    void project_twoProjectsInFolder_bothIncludedInFolderWithoutPrefix() {
        //arrange
        def folder = new File(tmpDir, folderName)
        folder.mkdir()
        def nameA = 'a'
        def nameB = 'b'
        def (descriptorA, projectDirA) = setUpProject(folder, nameA)
        def (descriptorB, projectDirB) = setUpProject(folder, nameB)
        def testee = new SettingsUtilPluginExtension(settings, folderName)
        //act
        testee.project(nameA, nameB)
        //assert
        verifyProjectIncluded(nameA, descriptorA, projectDirA)
        verifyProjectIncluded(nameB, descriptorB, projectDirB)
    }

    @Test
    void folder_callingPrefixedWithOneProjectInFolder_includedInFolder() {
        //arrange
        def folder = new File(tmpDir, folderName)
        folder.mkdir()
        def nameA = 'test-a'
        def (descriptorA, projectDirA) = setUpProject(folder, nameA)
        def testee = new SettingsUtilPluginExtension(settings, '')
        //act
        testee.folder(folderName, {
            prefixed('a')
        })
        //assert
        verifyProjectIncluded(nameA, descriptorA, projectDirA)
    }

    @Test
    void folder_callingProjectWithTwoProjects_bothIncludedInFolderWithoutPrefix() {
        //arrange
        def folder = new File(tmpDir, folderName)
        folder.mkdir()
        def nameA = 'a'
        def nameB = 'b'
        def (descriptorA, projectDirA) = setUpProject(folder, nameA)
        def (descriptorB, projectDirB) = setUpProject(folder, nameB)
        def testee = new SettingsUtilPluginExtension(settings, '')
        //act
        testee.folder(folderName, {
            project(nameA, nameB)
        })
        //assert
        verifyProjectIncluded(nameA, descriptorA, projectDirA)
        verifyProjectIncluded(nameB, descriptorB, projectDirB)
    }

    @Test
    void unknownProperty_notInFolder_callsPrefixedThusIncludesProjectInRoot() {
        //arrange
        def nameA = 'test-a'
        def (descriptorA, projectDirA) = setUpProject(nameA)
        def testee = new SettingsUtilPluginExtension(settings, '')
        //act
        testee.a
        //assert
        verifyProjectIncluded(nameA, descriptorA, projectDirA)
    }

    @Test
    void unknownProperty_inFolder_callsPrefixedThusIncludesInFolder() {
        //arrange
        def folder = new File(tmpDir, folderName)
        folder.mkdir()
        def nameA = 'test-a'
        def (descriptorA, projectDirA) = setUpProject(folder, nameA)
        def testee = new SettingsUtilPluginExtension(settings, folderName)
        //act
        testee.a
        //assert
        verifyProjectIncluded(nameA, descriptorA, projectDirA)
    }

    @Test
    void unknownMethod_withoutParameter_throwsMethodMissingException() {
        //arrange
        def testee = new SettingsUtilPluginExtension(settings, '')
        //assert & act
        def e = assertThrows(MissingMethodException) {
            testee.unknownMethod()
        }
        //assert
        assertTrue(e.message.contains('unknownMethod'), 'contains unknownMethod')
    }

    @Test
    void unknownMethod_withTwoParameters_throwsMethodMissingException() {
        //arrange
        def testee = new SettingsUtilPluginExtension(settings, '')
        //assert & act
        def e = assertThrows(MissingMethodException) {
            testee.unknownMethod(1, 2)
        }
        //assert
        assertTrue(e.message.contains('unknownMethod'), 'contains unknownMethod')
        assertTrue(e.message.contains('[1, 2]'), 'contains [1, 2]')
    }

    @Test
    void unknownMethod_oneParameterButNotClosure_throwsMethodMissingException() {
        //arrange
        def testee = new SettingsUtilPluginExtension(settings, '')
        //assert & act
        def e = assertThrows(MissingMethodException) {
            testee.unknownMethod(1)
        }
        //assert
        assertTrue(e.message.contains('unknownMethod'), 'contains unknownMethod')
        assertTrue(e.message.contains('[1]'), 'contains [1]')
    }

    private List setUpProject(String name) {
        setUpProject(tmpDir, name)
    }

    private List setUpProject(File folder, String name) {
        def projectDirA = new File(folder, name)
        projectDirA.mkdir()
        def descriptor = mock(ProjectDescriptor)
        when(settings.project(":$name")).thenReturn(descriptor)
        [descriptor, projectDirA]
    }

    private void verifyProjectIncluded(String nameA, descriptor, projectDirA) {
        verify(settings).include(":$nameA")
        verify(descriptor).setProjectDir(projectDirA)
    }
}
