package ch.tutteli.gradle.dokka

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.jetbrains.dokka.gradle.DokkaTask

class DokkaPluginExtension {
    protected static final String DEFAULT_REPO_URL = "repoUrlWillBeReplacedDuringProjectEvaluation"
    private DokkaTask dokkaTask

    Property<String> repoUrl
    Property<String> githubUser
    Property<Boolean> ghPages

    DokkaPluginExtension(Project project) {
        dokkaTask = project.tasks.getByName('dokka') as DokkaTask
        repoUrl = project.objects.property(String)
        githubUser = project.objects.property(String)
        if (isTutteliProject(project) || isTutteliProject(project.rootProject)) {
            githubUser.set('robstoll')
        }
        ghPages = project.objects.property(Boolean)
        ghPages.set(false)

        dokka {
            outputFormat = 'html'
            outputDirectory = "$project.buildDir/kdoc"
            linkMapping {
                dir = './'
                url = DEFAULT_REPO_URL
                suffix = '#L'
            }
        }
    }

    private static boolean isTutteliProject(Project project) {
        return project.group?.startsWith("ch.tutteli")
    }

    void dokka(Action<DokkaTask> configure) {
        configure.execute(dokkaTask)
    }
}

