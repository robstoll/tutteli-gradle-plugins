package ch.tutteli.gradle.kotlin

import org.gradle.api.Project
import org.gradle.api.provider.Property

class KotlinUtilsPluginExtension {
    Property<String> kotlinVersion

    KotlinUtilsPluginExtension(Project project) {
        kotlinVersion = project.objects.property(String)
    }
}
