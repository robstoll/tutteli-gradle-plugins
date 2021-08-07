package ch.tutteli.gradle.plugins.settings

import ch.tutteli.gradle.plugins.test.SettingsExtension
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import org.gradle.api.plugins.Convention
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor

import java.nio.file.Files
import java.nio.file.Path

import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*

class SettingsUtilPluginTest {
    private includeCustomInFolder
    private includePrefixedInFolder
    private includePrefixed
    private Path tmp
    private File tmpDir
    private rootProjectName = "test"
    private settings = mock(Settings)

    @BeforeEach
    void setUp() {
        //arrange
        tmp = Files.createTempDirectory("myTests")
        tmpDir = tmp.toFile()

        def testee = new SettingsUtilPlugin()
        def descriptor = mock(ProjectDescriptor)
        when(settings.rootProject).thenReturn(descriptor)
        when(descriptor.projectDir).thenReturn(tmpDir)
        when(descriptor.name).thenReturn(rootProjectName)


        def defaultConvention = mock(Convention)
        def ext = mock(ExtraPropertiesExtension)
        settings.metaClass.getExtensions = { -> defaultConvention }
        settings.metaClass.getExt = { -> ext }
        //act
        testee.apply(settings)
        //assert
        verify(defaultConvention).create('include', SettingsUtilPluginExtension, settings, '', '')
        def captor = ArgumentCaptor.forClass(Closure)
        verify(ext).set(eq('includeCustomInFolder'), captor.capture())
        includeCustomInFolder = captor.getValue()
        verify(ext).set(eq('includePrefixedInFolder'), captor.capture())
        includePrefixedInFolder = captor.getValue()
        verify(ext).set(eq('includePrefixed'), captor.capture())
        includePrefixed = captor.getValue()
    }

    @AfterEach
    void tearDown() {
        SettingsExtension.deleteTmp(tmp)
    }

    @Test
    void includePrefixed_oneProject_includedInRoot() {
        //arrange
        def nameA = 'test-a'
        def (descriptorA, projectDirA) = setUpProject(nameA)
        //act
        includePrefixed('a')
        //assert
        verifyProjectIncluded(nameA, descriptorA, projectDirA)
    }

    @Test
    void includePrefixed_twoProjects_bothIncludedInRoot() {
        //arrange
        def nameA = 'test-a'
        def nameB = 'test-b'
        def (descriptorA, projectDirA) = setUpProject(nameA)
        def (descriptorB, projectDirB) = setUpProject(nameB)
        //act
        includePrefixed('a', 'b')
        //assert
        verifyProjectIncluded(nameA, descriptorA, projectDirA)
        verifyProjectIncluded(nameB, descriptorB, projectDirB)
    }

    @Test
    void includePrefixed_nonExistingFolder_throwIllegalArgumentException() {
        //act
        Assertions.assertThrows(IllegalArgumentException) {
            includePrefixed('nonExisting')
        }
    }

    @Test
    void includePrefixedInFolder_oneProject_includedInFolder() {
        //arrange
        def folder = new File(tmpDir, 'myFolder')
        folder.mkdir()
        def nameA = 'test-a'
        def (descriptorA, projectDirA) = setUpProject(folder, nameA)
        //act
        includePrefixedInFolder('myFolder', 'a')
        //assert
        verifyProjectIncluded(nameA, descriptorA, projectDirA)
    }

    @Test
    void includePrefixedInFolder_twoProjects_bothIncludedInFolder() {
        //arrange
        def folder = new File(tmpDir, 'myFolder')
        folder.mkdir()
        def nameA = 'test-a'
        def nameB = 'test-b'
        def (descriptorA, projectDirA) = setUpProject(folder, nameA)
        def (descriptorB, projectDirB) = setUpProject(folder, nameB)
        //act
        includePrefixedInFolder('myFolder', 'a', 'b')
        //assert
        verifyProjectIncluded(nameA, descriptorA, projectDirA)
        verifyProjectIncluded(nameB, descriptorB, projectDirB)
    }

    @Test
    void includePrefixedInFolder_nonExistingFolder_throwIllegalArgumentException() {
        //act
        Assertions.assertThrows(IllegalArgumentException) {
            includePrefixedInFolder('myFolder', 'nonExisting')
        }
    }

    @Test
    void includeCustomInFolder_oneProject_includedInFolderWithoutPrefix() {
        //arrange
        def folder = new File(tmpDir, 'myFolder')
        folder.mkdir()
        def nameA = 'a'
        def (descriptorA, projectDirA) = setUpProject(folder, nameA)
        //act
        includeCustomInFolder('myFolder', nameA)
        //assert
        verifyProjectIncluded(nameA, descriptorA, projectDirA)
    }

    @Test
    void includeCustomInFolder_twoProjects_bothIncludedInFolderWithoutPrefix() {
        //arrange
        def folder = new File(tmpDir, 'myFolder')
        folder.mkdir()
        def nameA = 'a'
        def nameB = 'b'
        def (descriptorA, projectDirA) = setUpProject(folder, nameA)
        def (descriptorB, projectDirB) = setUpProject(folder, nameB)
        //act
        includeCustomInFolder('myFolder', nameA, nameB)
        //assert
        verifyProjectIncluded(nameA, descriptorA, projectDirA)
        verifyProjectIncluded(nameB, descriptorB, projectDirB)
    }

    @Test
    void includeCustomInFolder_nonExistingFolder_throwIllegalArgumentException() {
        //act
        Assertions.assertThrows(IllegalArgumentException) {
            includeCustomInFolder('myFolder', 'nonExisting')
        }
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
