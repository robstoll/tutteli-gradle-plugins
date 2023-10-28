package ch.tutteli.gradle.plugins.dokka

import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.GradleExternalDocumentationLinkBuilder
import org.jetbrains.dokka.gradle.GradleSourceLinkBuilder
import org.junit.jupiter.api.Test

import java.nio.file.FileSystems
import java.nio.file.Files

import static ch.tutteli.gradle.plugins.dokka.DokkaPlugin.EXTENSION_NAME
import static ch.tutteli.gradle.plugins.test.Asserts.assertThrowsProjectConfigExceptionWithCause
import static org.junit.jupiter.api.Assertions.*

class DokkaPluginSmokeTest {

    def s = FileSystems.getDefault().separator

    @Test
    void smokeTestSimpleMode() {
        //arrange
        def githubUser = 'test-user'
        def projectName = "projectName"
        Project project = ProjectBuilder.builder()
            .withName(projectName)
            .build()
        Files.createDirectories(project.projectDir.toPath().resolve("src/main/kotlin"))
        //act
        project.plugins.apply('org.jetbrains.kotlin.jvm')
        project.plugins.apply(DokkaPlugin)
        def extension = getExtension(project)
        extension.githubUser.set(githubUser)



        //assert
        DokkaTask dokkaTask = getDokkaTask(project)
        assertEquals(projectName, dokkaTask.moduleName.get())
        assertEquals("$project.projectDir${s}docs${s}kdoc".toString(), dokkaTask.outputDirectory.get().asFile.absolutePath)

        GradleSourceLinkBuilder sourceLink = getSingleMainSourceLink(dokkaTask)
        assertEquals("$project.projectDir${s}src${s}main${s}kotlin".toString(), sourceLink.localDirectory.get().absolutePath)
        assertEquals('#L', sourceLink.remoteLineSuffix.get())

        project.evaluate()
        assertEquals("https://github.com/$githubUser/projectName/tree/main/src/main/kotlin".toString(), sourceLink.remoteUrl.get().toString())
    }

    @Test
    void smokeTestGhPages_ReleaseVersion() {
        //arrange
        def githubUser = 'robstoll'
        def projectName = 'atrium'
        def version = '0.6.0'
        Project project = ProjectBuilder.builder()
            .withName(projectName)
            .build()
        Files.createDirectories(project.projectDir.toPath().resolve("src/main/kotlin"))
        //act
        project.version = version
        project.plugins.apply('org.jetbrains.kotlin.jvm')
        project.plugins.apply(DokkaPlugin)
        def extension = getExtension(project)
        extension.githubUser.set(githubUser)
        extension.getWriteToDocs().set(false)
        project.evaluate()

        //assert
        DokkaTask dokkaTask = getDokkaTask(project)
        assertEquals(projectName, dokkaTask.moduleName.get())
        assertEquals("${project.projectDir.toPath().resolve("..").normalize()}${s}$projectName-gh-pages${s}$version${s}kdoc".toString(), dokkaTask.outputDirectory.get().asFile.absolutePath)

        GradleSourceLinkBuilder sourceLink = getSingleMainSourceLink(dokkaTask)
        assertEquals("$project.projectDir${s}src${s}main${s}kotlin".toString(), sourceLink.localDirectory.get().absolutePath)
        assertEquals('#L', sourceLink.remoteLineSuffix.get())

        GradleExternalDocumentationLinkBuilder externalDocLInk = getSingleMainExternalDocumentationLink(dokkaTask)
        assertEquals("https://${githubUser}.github.io/$projectName/$version/kdoc/".toString(), externalDocLInk.url.get().toString())

        project.evaluate()
        assertEquals("https://github.com/$githubUser/$projectName/tree/v$version/src/main/kotlin".toString(), sourceLink.remoteUrl.get().toString())
    }

    @Test
    void smokeTestGhPages_RcVersion() {
        def version = '0.6.0-RC.1'
        //arrange
        def githubUser = 'robstoll'
        def projectName = 'atrium'

        Project project = ProjectBuilder.builder()
            .withName(projectName)
            .build()
        Files.createDirectories(project.projectDir.toPath().resolve("src/main/kotlin"))
        //act
        project.version = version
        project.plugins.apply('org.jetbrains.kotlin.jvm')
        project.plugins.apply(DokkaPlugin)
        def extension = getExtension(project)
        extension.githubUser.set(githubUser)
        extension.getWriteToDocs().set(false)
        project.evaluate()

        //assert
        DokkaTask dokkaTask = getDokkaTask(project)
        assertEquals(projectName, dokkaTask.moduleName.get())
        assertEquals("${project.projectDir.toPath().resolve("..").normalize()}${s}$projectName-gh-pages${s}$version${s}kdoc".toString(), dokkaTask.outputDirectory.get().asFile.absolutePath)

        GradleSourceLinkBuilder sourceLink = getSingleMainSourceLink(dokkaTask)
        assertEquals("$project.projectDir${s}src${s}main${s}kotlin".toString(), sourceLink.localDirectory.get().absolutePath)
        assertEquals('#L', sourceLink.remoteLineSuffix.get())

        assertHasNoExternalDocumentationLinksDefined(dokkaTask)

        project.evaluate()
        assertEquals("https://github.com/$githubUser/$projectName/tree/v$version/src/main/kotlin".toString(), sourceLink.remoteUrl.get().toString())
    }

    @Test
    void smokeTestGhPages_SnapshotVersion() {
        def version = '0.6.0-SNAPSHOT'
        //arrange
        def githubUser = 'robstoll'
        def projectName = 'atrium'

        Project project = ProjectBuilder.builder()
            .withName(projectName)
            .build()
        Files.createDirectories(project.projectDir.toPath().resolve("src/main/kotlin"))
        //act
        project.version = version
        project.plugins.apply('org.jetbrains.kotlin.jvm')
        project.plugins.apply(DokkaPlugin)
        def extension = getExtension(project)
        extension.githubUser.set(githubUser)
        extension.getWriteToDocs().set(false)
        project.evaluate()

        //assert
        DokkaTask dokkaTask = getDokkaTask(project)
        assertEquals(projectName, dokkaTask.moduleName.get())
        assertEquals("${project.projectDir.toPath().resolve("..").normalize()}${s}$projectName-gh-pages${s}$version${s}kdoc".toString(), dokkaTask.outputDirectory.get().asFile.absolutePath)

        GradleSourceLinkBuilder sourceLink = getSingleMainSourceLink(dokkaTask)
        assertEquals("$project.projectDir${s}src${s}main${s}kotlin".toString(), sourceLink.localDirectory.get().absolutePath)
        assertEquals('#L', sourceLink.remoteLineSuffix.get())

        assertHasNoExternalDocumentationLinksDefined(dokkaTask)

        project.evaluate()
        assertEquals("https://github.com/$githubUser/$projectName/tree/main/src/main/kotlin".toString(), sourceLink.remoteUrl.get().toString())
    }

    @Test
    void repoUrlExplicitlySet() {
        //arrange
        def projectName = "projectName"
        def repoUrl = "https://github.com/robstoll/tutteli-gradle-plugins"
        Project project = ProjectBuilder.builder()
            .withName(projectName)
            .build()
        Files.createDirectories(project.projectDir.toPath().resolve("src/main/kotlin"))
        //act
        project.plugins.apply('org.jetbrains.kotlin.jvm')
        project.plugins.apply(DokkaPlugin)
        def extension = getExtension(project)
        extension.repoUrl.set(repoUrl)

        //assert
        DokkaTask dokkaTask = getDokkaTask(project)
        GradleSourceLinkBuilder sourceLink = getSingleMainSourceLink(dokkaTask)
        assertEquals(repoUrl + "/tree/main/src/main/kotlin", sourceLink.remoteUrl.get().toString())
    }
    @Test
    void repoUrlIndirectlySetViaGithubAndVersionNotDefined(){
        //arrange
        def projectName = "projectName"
        def githubUser = "test-user"
        Project project = ProjectBuilder.builder()
            .withName(projectName)
            .build()
        Files.createDirectories(project.projectDir.toPath().resolve("src/main/kotlin"))
        //act
        project.plugins.apply('org.jetbrains.kotlin.jvm')
        project.plugins.apply(DokkaPlugin)
        def extension = getExtension(project)
         extension.githubUser.set(githubUser)

        //assert
        // repoUrl not set explicitly, need to evaluate project first so that repoUrl is derived from github user
        DokkaTask dokkaTask = getDokkaTask(project)
        GradleSourceLinkBuilder sourceLink = getSingleMainSourceLink(dokkaTask)
        assertEquals(false, sourceLink.remoteUrl.isPresent())

        project.evaluate()

        //assert
        sourceLink = getSingleMainSourceLink(dokkaTask)
        assertEquals("https://github.com/$githubUser/$projectName/tree/main/src/main/kotlin".toString(), sourceLink.remoteUrl.get().toString())
    }
    @Test
    void repoUrlIndirectlySetViaGithubAndVersionDefined(){
        //arrange
        def projectName = "projectName"
        def githubUser = "test-user"
        def version = "1.2.3"
        Project project = ProjectBuilder.builder()
            .withName(projectName)
            .build()
        Files.createDirectories(project.projectDir.toPath().resolve("src/main/kotlin"))
        //act
        project.version= version
        project.plugins.apply('org.jetbrains.kotlin.jvm')
        project.plugins.apply(DokkaPlugin)
        def extension = getExtension(project)
        extension.githubUser.set(githubUser)

        //assert
        // repoUrl not set explicitly, need to evaluate project first so that repoUrl is derived from github user
        DokkaTask dokkaTask = getDokkaTask(project)
        GradleSourceLinkBuilder sourceLink = getSingleMainSourceLink(dokkaTask)
        assertEquals(false, sourceLink.remoteUrl.isPresent())

        project.evaluate()

        //assert
        sourceLink = getSingleMainSourceLink(dokkaTask)
        assertEquals("https://github.com/$githubUser/$projectName/tree/v$version/src/main/kotlin".toString(), sourceLink.remoteUrl.get().toString())
    }


    @Test
    void evaluateProject_noGithubUserGivenNorRepoUrl_throwsIllegalStateException() {
        //arrange
        Project project = ProjectBuilder.builder().build()
        //act
        project.plugins.apply('org.jetbrains.kotlin.jvm')
        project.plugins.apply(DokkaPlugin)
        //assert
        assertThrowsProjectConfigExceptionWithCause(IllegalStateException, "${EXTENSION_NAME}.githubUser needs to be defined") {
            project.evaluate()
        }
    }

    private static assertHasNoExternalDocumentationLinksDefined(DokkaTask dokkaTask){
        def list = dokkaTask.dokkaSourceSets.collect { it.externalDocumentationLinks.get().collect() }.flatten()
        assertEquals(0, list.size(), "was $list" )
    }

    private static GradleSourceLinkBuilder getSingleMainSourceLink(DokkaTask dokkaTask) {
        def sourceSetWithSourceLinks = dokkaTask.dokkaSourceSets.collect {
            new Tuple2<String, List<GradleSourceLinkBuilder>>(it.name, it.sourceLinks.get().collect())
        }
        assertEquals(2, sourceSetWithSourceLinks.size(), "sourceSetWithSourceLinks: " + sourceSetWithSourceLinks.collect { it.v1 })

        def sourceLinks = sourceSetWithSourceLinks.find { it.v1 == "main" }.v2
        assertEquals(1, sourceLinks.size(), "sourceLinks: " + sourceLinks)
        sourceLinks.get(0)
    }

    private static GradleExternalDocumentationLinkBuilder getSingleMainExternalDocumentationLink(DokkaTask dokkaTask) {
        def sourceSetWithExternalDocLinks = dokkaTask.dokkaSourceSets.collect {
            new Tuple2<String, List<GradleExternalDocumentationLinkBuilder>>(it.name, it.externalDocumentationLinks.get().collect())
        }
        assertEquals(2, sourceSetWithExternalDocLinks.size(), "sourceSetWithExternalDocLinks: " + sourceSetWithExternalDocLinks.collect { it.v1 })

        def externalDocLinks = sourceSetWithExternalDocLinks.find { it.v1 == "main" }.v2
        assertEquals(1, externalDocLinks.size(), "externalDocLinks: " + externalDocLinks)
        externalDocLinks.get(0)
    }

    private static DokkaTask getDokkaTask(Project project) {
        project.tasks.getByName('dokkaHtml') as DokkaTask
    }

    private static DokkaPluginExtension getExtension(Project project) {
        project.extensions.getByName(EXTENSION_NAME) as DokkaPluginExtension
    }
}
