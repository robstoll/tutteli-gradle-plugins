package ch.tutteli.gradle.bintray

import com.jfrog.bintray.gradle.BintrayExtension
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

class SetUp {
    protected static final String ARTIFACT_ID = "test-project"
    protected static final String VERSION = "1.0.0"
    protected static final String GROUP_ID = "ch.tutteli"
    protected static final String DESCRIPTION = "description of the project"
    protected static final String GITHUB_USER = "robstoll"

    protected static Project setUp() {
        //arrange
        Project project = ProjectBuilder.builder()
            .withName(ARTIFACT_ID)
            .build()
        project.version = VERSION
        project.group = GROUP_ID
        project.description = DESCRIPTION
        project.plugins.apply(BintrayPlugin)
        project.plugins.apply('java')
        BintrayPluginExtension extension = getPluginExtension(project)
        extension.githubUser.set(GITHUB_USER)
        extension.artifacts.add(project.tasks.getByName('jar'))
        extension.bintrayRepo.set('tutteli-jars')
        extension.bintrayPkg.set('atrium')
        def jfrogBintray = getJfrogBintrayExtension(project)
        jfrogBintray.user = 'user'
        jfrogBintray.key = 'key'
        jfrogBintray.pkg.version.gpg.sign = false
        return project
    }

    protected static BintrayPluginExtension getPluginExtension(Project project) {
        return project.extensions.getByName(BintrayPlugin.EXTENSION_NAME) as BintrayPluginExtension
    }

    protected static BintrayExtension getJfrogBintrayExtension(Project project) {
        project.extensions.getByType(BintrayExtension)
    }
}
