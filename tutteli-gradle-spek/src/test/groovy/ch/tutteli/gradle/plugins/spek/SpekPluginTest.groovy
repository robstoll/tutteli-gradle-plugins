package ch.tutteli.gradle.plugins.spek

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.junit.jupiter.api.Test

import static ch.tutteli.gradle.plugins.spek.SpekPlugin.EXTENSION_NAME
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

class SpekPluginTest {

    @Test
    void version_differentThanDefault_compileAndRuntimeClasspathAccordingly() {
        //arrange
        Project project = ProjectBuilder.builder().build()
        project.plugins.apply(KotlinPluginWrapper)
        project.plugins.apply(SpekPlugin)
        def extension = project.extensions.getByName(EXTENSION_NAME)
        extension.version = '1.1.2'
        //act
        project.evaluate()
        //assert
        FileCollection compile = project.sourceSets.test.compileClasspath
        def api = 'spek-api-1.1.2.jar'
        def platform =  'spek-junit-platform-engine-1.1.2.jar'
        assertTrue(compile.join(",").contains(api), "compile classpath contains $api:\n ${compile.join(",")}")
        assertFalse(compile.join(",").contains(platform), "compile classpath should not contain $platform but did:\n ${compile.join(",")}")
        FileCollection runtime = project.sourceSets.test.runtimeClasspath
        assertTrue(compile.join(",").contains(api), "runtime classpath contains $api:\n ${compile.join(",")}")
        assertTrue(runtime.join(",").contains(platform), "runtime classpath contains $platform:\n ${runtime.join(",")}")
    }
}
