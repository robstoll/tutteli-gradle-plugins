package ch.tutteli.gradle.settings

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class SettingsUtilPluginIntTest {
    private File settingsFile
    private Path tmp
    private File tmpDir
    private List<String> pluginClasspath

    @BeforeEach
    void setup() throws IOException {
        tmp = Files.createTempDirectory("myTests")
        tmpDir = tmp.toFile()
        settingsFile = new File(tmpDir, "settings.gradle")
        URL pluginClasspathResource = getClass().classLoader.getResource('plugin-classpath.txt')
        if (pluginClasspathResource == null) {
            throw new IllegalStateException('Did not find plugin classpath resource, run `testClasses` build task.')
        }

        pluginClasspath = pluginClasspathResource.readLines()
            .collect { it.replace('\\', '\\\\') }
            .collect { "\'${it}\'" }
    }

    @AfterEach
    void tearDown() {
        deleteTmp(tmp)
    }

    static void deleteTmp(Path tmpFolder) {
        Files.walkFileTree(tmpFolder, new SimpleFileVisitor<Path>() {

            @Override
            FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                return deleteAndContinue(file)
            }

            @Override
            FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return deleteAndContinue(dir)
            }

            private FileVisitResult deleteAndContinue(Path path) throws IOException {
                Files.delete(path)
                return FileVisitResult.CONTINUE
            }
        })
    }


    @Test
    void smokeTest() throws IOException {
        //arrange
        new File(tmpDir, 'test-project-one').mkdir()
        new File(tmpDir, 'test-project-two').mkdir()
        new File(tmpDir, 'test/test-project-three').mkdirs()
        new File(tmpDir, 'test/test-project-four').mkdirs()
        new File(tmpDir, 'test/five').mkdir()
        new File(tmpDir, 'test/six').mkdir()
        new File(tmpDir, 'seven').mkdir()
        new File(tmpDir, 'eight').mkdir()

        settingsFile << """
        rootProject.name='test-project'
        buildscript {
            dependencies {
                classpath files($pluginClasspath)
            }
        }
        apply plugin: 'ch.tutteli.settings'
        includeOwn 'one'
        includeOwn ('one', 'two')
        includeInFolder('test', 'three')
        includeInFolder('test', 'three', 'four')
        includeCustomInFolder('test', 'five')
        includeCustomInFolder('test', 'five', 'six')
               
        include {
            modules 'one'
            modules ('one', 'two')
            
            folder ('test') {
                modules 'three'
                modules ('three', 'four')
            }
            
            folder ('test') {
                custom 'five'
                custom ('five', 'six')
            }
            
            custom 'seven'
            custom ('seven', 'eight')
        }
        """
        //act
        def result = GradleRunner.create()
            .withProjectDir(tmpDir)
            .withArguments("projects")
            .build()
        //assert
        assertTrue(result.output.contains('test-project-one'), "project test-project-one in output: ${result.output}")
        assertTrue(result.output.contains('test-project-two'), "project test-project-two in output: ${result.output}")
        assertTrue(result.output.contains('three'), "project test-project-two in output: ${result.output}")
        assertEquals([':projects'], result.taskPaths(TaskOutcome.SUCCESS))
        assertTrue(result.taskPaths(TaskOutcome.SKIPPED).empty, 'SKIPPED is empty')
        assertTrue(result.taskPaths(TaskOutcome.UP_TO_DATE).empty, 'UP_TO_DATE is empty')
        assertTrue(result.taskPaths(TaskOutcome.FAILED).empty, 'FAILED is empty')
    }
}
