package ch.tutteli.gradle.dokka

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.jetbrains.dokka.gradle.DokkaPlugin as JetbrainsDokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.LinkMapping


class DokkaPlugin implements Plugin<Project> {
    static final String EXTENSION_NAME = 'tutteliDokka'

    @Override
    void apply(Project project) {
        project.pluginManager.apply(JetbrainsDokkaPlugin)
        project.extensions.create(EXTENSION_NAME, DokkaPluginExtension, project)
    }
}

