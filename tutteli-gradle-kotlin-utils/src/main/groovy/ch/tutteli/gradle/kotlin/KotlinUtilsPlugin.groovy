package ch.tutteli.gradle.kotlin


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency

class KotlinUtilsPlugin implements Plugin<Project> {
    static final String EXTENSION_NAME = 'kotlinutils'
    protected static final String ERR_KOTLIN_VERSION = 'kotlinutils.kotlinVersion has to be defined'

    @Override
    void apply(Project project) {
        def extension = project.extensions.create(EXTENSION_NAME, KotlinUtilsPluginExtension, project)
        augmentProjectExt(project, extension)
    }

    private void augmentProjectExt(Project project, KotlinUtilsPluginExtension extension) {
        project.ext.kotlinStdlib = { getKotlinDependency(extension, 'stdlib') }
        project.ext.kotlinStdlibJs = { getKotlinDependency(extension, 'stdlib-js') }
        project.ext.kotlinStdlibCommon = { getKotlinDependency(extension, 'stdlib-common') }
        project.ext.kotlinReflect = { getKotlinDependency(extension, 'reflect') }

        project.ext.excludeKbox = { ExcludeExtension.excludeKbox(owner as ExternalModuleDependency) }
        project.ext.excludeKotlin = { ExcludeExtension.excludeKotlin(owner as ExternalModuleDependency) }
        project.ext.excludeAtriumVerbs = { ExcludeExtension.excludeAtriumVerbs(owner as ExternalModuleDependency) }
        project.ext.excluding = { Closure<ExcludeExtension> closure ->
            return {
                def excludes = closure.clone()
                excludes.resolveStrategy = Closure.DELEGATE_FIRST
                excludes.delegate = new ExcludeExtension(owner as ExternalModuleDependency)
                excludes.call()
            }
        }

        def getCommonProjects = { getSubprojectsWithSuffix(project, "-common") }
        def getJsProjects = { getSubprojectsWithSuffix(project, "-js") }
        def getJvmProjects = { getSubprojectsWithSuffix(project, "-jvm") }

        project.ext.getCommonProjects = getCommonProjects
        project.ext.getJsProjects = getJsProjects
        project.ext.getJvmProjects = getJvmProjects

        project.ext.configureCommonProjects = {

            project.configure(getCommonProjects()) {
                apply plugin: 'kotlin-platform-common'

                dependencies {
                    compile kotlinStdlibCommon()
                }
            }
        }

        project.ext.configureJsProjects = {
            project.configure(getJsProjects()) { Project subproject ->
                apply plugin: 'kotlin-platform-js'

                dependencies {
                    compile kotlinStdlibJs()
                    expectedBy getCommonProject(project, subproject)
                }

                compileKotlin2Js {
                    kotlinOptions.moduleKind = "umd"
                    kotlinOptions.sourceMap = true
                    kotlinOptions.sourceMapEmbedSources = "always"
                }
            }
        }

        project.ext.configureJvmProjects = {
            project.configure(getJvmProjects()) { Project subproject ->
                apply plugin: 'kotlin-platform-jvm'

                dependencies {
                    compile kotlinStdlib()
                    expectedBy getCommonProject(project, subproject)
                }
            }
        }
    }

    private static String getKotlinDependency(KotlinUtilsPluginExtension extension, String name) {
        def kotlinVersion = resolveKotlinVersion(extension)
        return "org.jetbrains.kotlin:kotlin-$name:$kotlinVersion"
    }

    private static String resolveKotlinVersion(KotlinUtilsPluginExtension extension) {
        final String kotlinVersion = extension.kotlinVersion.getOrNull()
        if (!kotlinVersion?.trim()) throw new IllegalStateException(ERR_KOTLIN_VERSION)
        return kotlinVersion
    }

    private static Set<Project> getSubprojectsWithSuffix(project, suffix) {
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
