package ch.tutteli.gradle.settings

import org.gradle.api.Action
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import java.nio.file.Files
import java.nio.file.Path

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
        SettingsUtilPluginIntTest.deleteTmp(tmp)
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
        testee.folder(folderName, new Action<SettingsUtilPluginExtension>() {
            void execute(SettingsUtilPluginExtension ex) {
                ex.prefixed('a')
            }
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
        testee.folder(folderName, new Action<SettingsUtilPluginExtension>() {
            void execute(SettingsUtilPluginExtension ex) {
                ex.project(nameA, nameB)
            }
        })
        //assert
        verifyProjectIncluded(nameA, descriptorA, projectDirA)
        verifyProjectIncluded(nameB, descriptorB, projectDirB)
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
