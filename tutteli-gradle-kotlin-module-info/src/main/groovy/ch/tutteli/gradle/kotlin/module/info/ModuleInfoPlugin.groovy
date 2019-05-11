package ch.tutteli.gradle.kotlin.module.info

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention

class ModuleInfoPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        if (JavaVersion.current() >= JavaVersion.VERSION_1_9) {
            if (project.components.findByName('java') == null) throw new IllegalStateException("""\
                Could not find the java component.
                Did you forget to apply the kotlin plugin? Make sure it is applied before this plugin.
                """.stripIndent()
            )
            setUpModuleInfo(project)
        }
    }

    private static void setUpModuleInfo(Project project) {
        def srcModule = "src/module"
        def moduleInfo = project.file("${project.projectDir}/$srcModule/module-info.java")
        if (moduleInfo.exists()) {
            project.sourceSets {
                module {
                    java {
                        srcDirs = [srcModule]
                        compileClasspath = main.compileClasspath
                    }
                }
                main {
                    kotlin { srcDirs += [srcModule] }
                }
            }

            project.compileModuleJava.configure {
                dependsOn project.compileKotlin
                destinationDir = project.compileKotlin.destinationDir
                doFirst {
                    options.compilerArgs = ['--module-path', classpath.asPath]
                    classpath = project.files()
                }
            }
            project.tasks.getByName('jar').dependsOn project.compileModuleJava
        }
    }
}
