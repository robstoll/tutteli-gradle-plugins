package ch.tutteli.gradle.kotlin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property

class KotlinUtilsPluginExtension {
    Property<String> kotlinVersion

    KotlinUtilsPluginExtension(Project project) {
        kotlinVersion = project.objects.property(String)
    }
}

class KotlinUtilsPlugin implements Plugin<Project> {
    static final String EXTENSION_NAME = 'kotlinutils'
    protected static final String ERR_KOTLIN_VERSION = 'kotlinutils.kotlinVersion has to be defined'


    @Override
    void apply(Project project) {
        def extension = project.extensions.create(EXTENSION_NAME, KotlinUtilsPluginExtension, project)

        project.ext.kotlinStdLib = { getKotlinDependency(extension, 'stdlib') }
        project.ext.kotlinStdJsLib= { getKotlinDependency(extension, 'stdlib-js') }
        project.ext.kotlinStdCommonLib = { getKotlinDependency(extension, 'stdlib-common') }
        project.ext.kotlinReflect = { getKotlinDependency(extension, 'reflect') }

        project.ext.withoutKbox = { exclude group: 'ch.tutteli.kbox' }
        project.ext.withoutKotlin = { exclude group: 'org.jetbrains.kotlin' }

        def getCommonProjects = { getSubrojectsWithSuffix(project, "-common") }
        project.ext.getCommonProjects = getCommonProjects

        project.ext.configureCommonProjects = {

            project.configure(getCommonProjects()) {
                apply plugin: 'kotlin-platform-common'

                dependencies {
                    compile kotlinStdCommonLib()
                }
            }
        }
    }

    private static String getKotlinDependency(KotlinUtilsPluginExtension extension, String name){
        def kotlinVersion = resolveKotlinVersion(extension)
        return "org.jetbrains.kotlin:kotlin-$name:$kotlinVersion"
    }

    private static String resolveKotlinVersion(KotlinUtilsPluginExtension extension) {
        String kotlinVersion
        try {
            kotlinVersion = extension.kotlinVersion.get()
        } catch (IllegalStateException ex) {
            throw new IllegalStateException(ERR_KOTLIN_VERSION, ex)
        }
        if (kotlinVersion == null || kotlinVersion.isEmpty()) throw new IllegalStateException(ERR_KOTLIN_VERSION)
        return kotlinVersion
    }

    private static Set<Project> getSubrojectsWithSuffix(project, suffix) {
        return project.subprojects.findAll { it.name.endsWith(suffix) }
    }
}
