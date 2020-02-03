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
        createBuildTasks(project)
    }

    private void augmentProjectExt(Project project, KotlinUtilsPluginExtension extension) {
        project.ext.kotlinStdlib = { getKotlinDependency(extension, 'stdlib') }
        project.ext.kotlinStdlibJs = { getKotlinDependency(extension, 'stdlib-js') }
        project.ext.kotlinStdlibCommon = { getKotlinDependency(extension, 'stdlib-common') }
        project.ext.kotlinReflect = { getKotlinDependency(extension, 'reflect') }
        project.ext.kotlinTest= { getKotlinDependency(extension, 'test') }
        project.ext.kotlinTestJunit5= { getKotlinDependency(extension, 'test-junit5') }
        project.ext.kotlinTestJs = { getKotlinDependency(extension, 'test-js') }
        project.ext.kotlinTestCommon = { getKotlinDependency(extension, 'test-common') }
        project.ext.kotlinTestAnnotationsCommon = { getKotlinDependency(extension, 'test-annotations-common') }

        project.ext.excludeKbox = { ExcludeExtension.excludeKbox(owner as ExternalModuleDependency) }
        project.ext.excludeKotlin = { ExcludeExtension.excludeKotlin(owner as ExternalModuleDependency) }
        project.ext.excludeAtriumVerbs = { ExcludeExtension.excludeAtriumVerbs(owner as ExternalModuleDependency) }
        project.ext.excluding = { Closure<ExcludeExtension> closure ->
            return {
                def excludes = closure.clone()
                excludes.resolveStrategy = DELEGATE_FIRST
                excludes.delegate = new ExcludeExtension(owner as ExternalModuleDependency)
                excludes.call()
            }
        }

        def getCommonProjects = { getSubprojectsWithSuffix(project, "-common") }
        def getJsProjects = { getSubprojectsWithSuffix(project, "-js") }
        def getJvmProjects = { getSubprojectsWithSuffix(project, "-jvm") }
        def getAndroidProjects = { getSubprojectsWithSuffix(project, "-android") }

        project.ext.getCommonProjects = getCommonProjects
        project.ext.getJsProjects = getJsProjects
        project.ext.getJvmProjects = getJvmProjects
        project.ext.getAndroidProjects = getAndroidProjects
        project.ext.getProjectNameWithoutSuffix = { Project aProject -> getProjectNameWithoutSuffix(aProject) }

        project.ext.configureCommonProjects = {

            project.configure(getCommonProjects()) {
                apply plugin: 'kotlin-platform-common'

                sourceCompatibility = 8
                targetCompatibility = 8

                compileKotlinCommon {
                    if (treatWarningsAsErrors()) {
                        kotlinOptions.allWarningsAsErrors = true
                    }
                }
                compileTestKotlinCommon {
                    if (treatWarningsAsErrors()) {
                        kotlinOptions.allWarningsAsErrors = true
                    }
                }

                dependencies {
                    implementation kotlinStdlibCommon()
                    testImplementation kotlinTestCommon()
                    testImplementation kotlinTestAnnotationsCommon()
                }
            }
        }

        project.ext.configureJsProjects = {
            project.configure(getJsProjects()) { Project subproject ->
                apply plugin: 'kotlin-platform-js'

                compileKotlin2Js {
                    if (treatWarningsAsErrors()) {
                        kotlinOptions.allWarningsAsErrors = true
                    }
                }
                compileTestKotlin2Js {
                    if (treatWarningsAsErrors()) {
                        kotlinOptions.allWarningsAsErrors = true
                    }
                }

                dependencies {
                    implementation kotlinStdlibJs()
                    expectedBy getCommonProject(project, subproject)

                    testImplementation kotlinTestJs()
                }

                compileKotlin2Js {
                    kotlinOptions.moduleKind = "umd"
                    kotlinOptions.sourceMap = true
                    kotlinOptions.sourceMapEmbedSources = "always"
                }
            }
        }

        project.ext.configureJvmProjects = {
            configureJvmLikeProjects(project, getJvmProjects())
        }

        project.ext.configureAndroidProjects = {
            configureJvmLikeProjects(project, getAndroidProjects())
        }
    }

    private static void configureJvmLikeProjects(Project rootProject, Set<Project> projects){
        rootProject.configure(projects) { Project subproject ->
            apply plugin: 'kotlin-platform-jvm'

            sourceCompatibility = 8
            targetCompatibility = 8

            compileKotlin {
                if (treatWarningsAsErrors()) {
                    kotlinOptions.allWarningsAsErrors = true
                }
            }
            compileTestKotlin {
                if (treatWarningsAsErrors()) {
                    kotlinOptions.allWarningsAsErrors = true
                }
            }

            dependencies {
                implementation kotlinStdlib()
                expectedBy getCommonProject(rootProject, subproject)

                testImplementation kotlinTest()
                testImplementation kotlinTestJunit5()
            }
        }
    }

    private static boolean treatWarningsAsErrors() {
        return Boolean.parseBoolean(System.getenv('CI')) || Boolean.parseBoolean(System.getenv('WARN_AS_ERROR'))
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
        def commonName = ":${getProjectNameWithoutSuffix(subproject)}-common"
        return project.project(commonName)
    }

    private static String getProjectNameWithoutSuffix(Project subproject){
        def name = subproject.name
        def suffix = name.endsWith('-jvm') ? '-jvm'
            : name.endsWith('-js') ? '-js'
            : name.endsWith('-android') ? '-android'
            : null
        if (suffix == null) throw new IllegalArgumentException("unknown project suffix, expected -jvm, -js or -android. Project name was: $name")

        return name.substring(0, name.indexOf(suffix))
    }

    private static void createBuildTasks(Project project) {
        project.afterEvaluate {
            createBuildTask(project, 'buildAllAndroid', '-android')
            createBuildTask(project, 'buildAllJs', '-js')
            createBuildTask(project, 'buildAllJvm', '-jvm')
            createBuildTask(project, 'buildAllCommon', '-common')
        }
    }
    private static void createBuildTask(Project project, String taskName, String suffix){
        def buildTasks = getSubprojectsWithSuffix(project, suffix).findResults { it.tasks.findByName('build') }
        project.tasks.create(name: taskName, group: 'build', description: "depends on all subprojects with a $suffix suffix", dependsOn: buildTasks )
    }
}
