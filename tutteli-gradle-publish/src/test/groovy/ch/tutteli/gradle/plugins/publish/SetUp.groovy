package ch.tutteli.gradle.plugins.publish

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

class SetUp {
    protected static final String ARTIFACT_ID = "test-project"
    protected static final String VERSION = "1.0.0"
    protected static final String GROUP_ID = "ch.tutteli"
    protected static final String DESCRIPTION = "description of the project"
    protected static final String GITHUB_USER = "robstoll"

    protected static Project setUp() {
        setUp { project ->
            project.pluginManager.apply('java')
        }
    }

    protected static Project setUp(Action<Project> pluginApplier) {
        //arrange
        Project project = ProjectBuilder.builder()
            .withName(ARTIFACT_ID)
            .build()
        project.version = VERSION
        project.group = GROUP_ID
        project.description = DESCRIPTION

        pluginApplier.execute(project)
        project.plugins.apply(PublishPlugin)
        PublishPluginExtension extension = getPluginExtension(project)
        extension.githubUser.set(GITHUB_USER)
        return project
    }

    protected static PublishPluginExtension getPluginExtension(Project project) {
        return project.extensions.getByName(PublishPlugin.EXTENSION_NAME) as PublishPluginExtension
    }
}
