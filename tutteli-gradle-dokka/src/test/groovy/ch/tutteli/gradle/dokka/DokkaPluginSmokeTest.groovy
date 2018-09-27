package ch.tutteli.gradle.dokka

import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.DokkaVersion
import org.junit.jupiter.api.Test

import static ch.tutteli.gradle.dokka.DokkaPlugin.EXTENSION_NAME
import static ch.tutteli.gradle.test.Asserts.assertThrowsProjectConfigExceptionWithCause
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
        DokkaTask dokkaTask = getDokkaTask(project)
        assertEquals('html', dokkaTask.outputFormat)
        assertEquals(projectName, dokkaTask.moduleName)
        assertEquals("$project.buildDir/kdoc".toString(), dokkaTask.outputDirectory,)
        assertEquals(1, dokkaTask.linkMappings.size(), "linkMappings: " + dokkaTask.linkMappings)
        def linkMapping = dokkaTask.linkMappings.get(0)
        assertEquals(project.projectDir.absolutePath, linkMapping.dir,)
        assertEquals(DokkaPluginExtension.DEFAULT_REPO_URL, linkMapping.url)
        assertEquals('#L', linkMapping.suffix)
        assertEquals('0.9.17', DokkaVersion.version)

        Jar javadocTask = project.tasks.getByName(DokkaPlugin.TASK_NAME_JAVADOC) as Jar
        assertNotNull(javadocTask, DokkaPlugin.TASK_NAME_JAVADOC)
        assertEquals('javadoc', javadocTask.classifier)
        assertTrue(javadocTask.dependsOn.contains(dokkaTask), "$DokkaPlugin.TASK_NAME_JAVADOC should depend on Dokka task")

        project.evaluate()

        assertEquals(repoUrl + "/tree/vunspecified", linkMapping.url)
    }

    @Test
    void evaluateProject_repoUrlNorGithubUserIsSet_throwsIllegalArgumentIfLinkMappingPresentAnd() {
        //arrange
        Project project = ProjectBuilder.builder().build()
        //act
        project.plugins.apply(DokkaPlugin)
        //assert
        assertThrowsProjectConfigExceptionWithCause(IllegalStateException, DokkaPlugin.ERR_REPO_URL_OR_GITHUB_USER) {
            project.evaluate()
        }
    }

    @Test
    void evaluateProject_repoUrlNorGithubUserButLinkMappingRemoved_DoesNotThrow() {
        //arrange
        Project project = ProjectBuilder.builder().build()
        //act
        project.plugins.apply(DokkaPlugin)
        getDokkaTask(project).linkMappings = new ArrayList<>()
        //assert no exception
    }

    @Test
    void evaluateProject_ghPagesDefinedWithoutUser_throwsIllegalArgumentWhenEvaluatingProject() {
        //arrange
        Project project = ProjectBuilder.builder().build()
        project.plugins.apply(DokkaPlugin)
        def extension = project.extensions.getByName(EXTENSION_NAME)
        extension.repoUrl = "test"
        extension.ghPages = true
        //act
        assertThrowsProjectConfigExceptionWithCause(IllegalStateException, DokkaPlugin.ERR_GH_PAGES_WITHOUT_USER) {
            project.evaluate()
        }
    }

    private static DokkaTask getDokkaTask(Project project) {
        project.tasks.getByName('dokka') as DokkaTask
    }
}
