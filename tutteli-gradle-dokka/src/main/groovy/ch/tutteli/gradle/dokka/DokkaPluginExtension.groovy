package ch.tutteli.gradle.dokka

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.LinkMapping

class DokkaPluginExtension {

    Property<String> repoUrl
    Property<String> githubUser
    private DokkaTask dokkaTask

    DokkaPluginExtension(Project project) {
        dokkaTask = project.tasks.getByName('dokka') as DokkaTask
        repoUrl = project.objects.property(String)
        githubUser = project.objects.property(String)

        dokka {
            outputFormat = 'html'
            outputDirectory = "$project.buildDir/kdoc"
            linkMappings = linkMappings + new LazyUrlLinkMapping(project, this)
        }
    }

    void dokka(Action<DokkaTask> configure) {
        configure.execute(dokkaTask)
    }

    static class LazyUrlLinkMapping extends LinkMapping {
        protected static final String ERR_REPO_URL = 'tutteliDokka.repoUrl or tutteliDokka.githubUser has to be defined'
        private Project project
        private DokkaPluginExtension extension

        LazyUrlLinkMapping(Project project, DokkaPluginExtension extension) {
            super.setDir(project.projectDir.absolutePath)
            super.setSuffix('#L')
            this.project = project
            this.extension = extension
        }

        @Override
        String getUrl() {
            if (!extension.repoUrl.isPresent() && !extension.githubUser.isPresent()) throw new IllegalStateException(ERR_REPO_URL)
            if (!extension.repoUrl.isPresent()) {
                extension.repoUrl.set("https://github.com/${extension.githubUser.get()}/$project.name".toString())
            }
            def urlWithPossibleSlash = extension.repoUrl.get()
            def urlWithSlash = urlWithPossibleSlash.endsWith("/") ? urlWithPossibleSlash : urlWithPossibleSlash + "/"
            def gitRef = project.version.endsWith("-SNAPSHOT") ? 'master' : 'v' + project.version
            return "${urlWithSlash}tree/$gitRef"
        }

        @Override
        void setUrl(String s) {
            throw new IllegalStateException("Cannot set URL of a ${LazyUrlLinkMapping.class.name}")
        }
    }
}

