package ch.tutteli.gradle.dokka

import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.jvm.tasks.Jar
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.DokkaVersion
import org.junit.jupiter.api.Test

import static ch.tutteli.gradle.dokka.DokkaPlugin.EXTENSION_NAME
import static org.junit.jupiter.api.Assertions.*

class DokkaPluginSmokeTest {

    @Test
    void smokeTest() {
        //arrange
        def projectName = "projectName"
        def repoUrl = "https://github.com/robstoll/tutteli-gradle-plugins"
        Project project = ProjectBuilder.builder()
            .withName(projectName)
            .build()
        //act
        project.plugins.apply(DokkaPlugin)
        def extension = project.extensions.getByName(EXTENSION_NAME)
        extension.repoUrl = repoUrl
        //assert
        DokkaTask dokkaTask = project.tasks.getByName('dokka') as DokkaTask
        assertNotNull(dokkaTask, 'dokka task')
        assertEquals('html', dokkaTask.outputFormat)
        assertEquals(projectName, dokkaTask.moduleName)
        assertEquals("$project.buildDir/kdoc".toString(), dokkaTask.outputDirectory,)
        assertEquals(1, dokkaTask.linkMappings.size(), "linkMappings: " + dokkaTask.linkMappings)
        def linkMapping = dokkaTask.linkMappings.get(0)
        assertEquals(project.projectDir.absolutePath, linkMapping.dir,)
        assertEquals(repoUrl + "/tree/vunspecified", linkMapping.url)
        assertEquals('#L', linkMapping.suffix)
        assertEquals('0.9.17', DokkaVersion.version)

        Jar javadocTask = project.tasks.getByName(DokkaPlugin.JAVADOC_JAR_TASK_NAME) as Jar
        assertNotNull(javadocTask, DokkaPlugin.JAVADOC_JAR_TASK_NAME)
        assertEquals('javadoc', javadocTask.classifier)
        assertTrue(javadocTask.dependsOn.contains(dokkaTask), "$DokkaPlugin.JAVADOC_JAR_TASK_NAME should depend on Dokka task")
    }

    @Test
    void accessUrlOfLinkMapping_throwsIllegalArgumentIfRepoUrlIsNotSet() {
        //arrange
        Project project = ProjectBuilder.builder().build()
        //act
        project.plugins.apply(DokkaPlugin)
        //assert
        def linkMappings = project.tasks.getByName('dokka').linkMappings
        assertEquals(1, linkMappings.size())
        def exception = assertThrows(IllegalStateException) {
            linkMappings.get(0).url
        }
        assertEquals(DokkaPluginExtension.LazyUrlLinkMapping.ERR_REPO_URL, exception.message)
    }

    @Test
    void ghPagesDefinedWithoutUser_throwsIllegalArgumentWhenEvaluatingProject() {
        //arrange
        Project project = ProjectBuilder.builder().build()
        project.plugins.apply(DokkaPlugin)
        def extension = project.extensions.getByName(EXTENSION_NAME)
        extension.ghPages = true
        //act

        def exception = assertThrows(ProjectConfigurationException) {
            project.evaluate()
        }
        //assert
        assertEquals(IllegalStateException, exception.cause.class)
        assertEquals(DokkaPlugin.ERR_GH_PAGES_WITHOUT_USER, exception.cause.message)
    }
}
