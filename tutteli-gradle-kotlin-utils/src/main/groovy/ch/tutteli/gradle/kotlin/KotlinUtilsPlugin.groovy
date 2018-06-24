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
        def getJsProjects = { getSubrojectsWithSuffix(project, "-js") }

        project.ext.getCommonProjects = getCommonProjects
        project.ext.getJsProjects = getJsProjects

        project.ext.configureCommonProjects = {

            project.configure(getCommonProjects()) {
                apply plugin: 'kotlin-platform-common'

                dependencies {
                    compile kotlinStdCommonLib()
                }
            }
        }

        project.ext.configureJsProjects = {
            project.configure(getJsProjects()) { Project subproject ->
                apply plugin: 'kotlin-platform-js'

                dependencies {
                    compile kotlinStdJsLib()
                    expectedBy getCommonProject(project, subproject)
                }

                compileKotlin2Js {
                    kotlinOptions.moduleKind = "umd"
                    kotlinOptions.sourceMap = true
                    kotlinOptions.sourceMapEmbedSources = "always"
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

    private static Project getCommonProject(Project project, Project subproject) {
        def name = subproject.name
        def suffix = name.endsWith('-jvm') ? '-jvm'
            : name.endsWith('-js') ? '-js'
            : null
        if (suffix == null) throw new IllegalArgumentException("unknown project suffix, expected -jvm or -js. Project name was: $name")

        def commonName = ":${name.substring(0, name.indexOf(suffix))}-common"
        return project.project(commonName)
    }
}
