package ch.tutteli.gradle.plugins.kotlin.module.info

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.*
import java.io.File

class ModuleInfoPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        if (JavaVersion.current() >= JavaVersion.VERSION_11) {
            project.afterEvaluate {
                if (project.plugins.findPlugin("java") == null) throw IllegalStateException(
                    """\
                    Looks like the java plugin was not applied. Did you forget to apply the kotlin plugin?
                    In case you use the multiplatform plugin, then activate java in the jvm target as follows:

                    kotlin {
                        jvm {
                            withJava()
                        }
                    }
                    """.trimIndent()
                )
                setUpModuleInfo(project)
            }
        }
    }

    private fun setUpModuleInfo(project: Project) {
        val javaCompile = project.tasks.getByName<JavaCompile>("compileJava")
        val javaFiles = javaCompile.source.files

        val moduleName = if (project.extra.has("moduleName")) {
            val moduleName = project.extra.get("moduleName") as String
            project.logger.info("tutteli-gradle-kotlin-module-info: using moduleName from project.extra which is: $moduleName")
            moduleName
        } else {
            val moduleInfo = javaFiles.find { it.name == "module-info.java" }
                ?: throw IllegalStateException(
                    "no module-info.java found in compileJava.source, following the first 10 files:\n${
                        javaFiles.take(10).joinToString("\n")
                    }"
                )

            var line = ""
            moduleInfo.forEachLine {
                if (it.startsWith("module")) {
                    line = it
                    return@forEachLine
                }
            }
            val moduleName = Regex("module\\s+([^ ]+).*").matchEntire(line)?.groupValues?.get(1)
                ?: throw  IllegalStateException("line starting with module in module-info.java did not specify moduleName")
            project.logger.info("tutteli-gradle-kotlin-module-info: using moduleName from module-info.java which is: $moduleName")
            moduleName
        }
        val java = project.the<JavaPluginExtension>()
        with(javaCompile) {
            inputs.property("moduleName", moduleName)
            with(options) {
                javaModuleVersion.set(project.provider { project.rootProject.version as String })
                compilerArgs =
                    listOf("--patch-module", "$moduleName=${java.sourceSets.getByName("main").output.asPath}")
            }
        }
        with(java) {
            modularity.inferModulePath.set(true)
            if (sourceCompatibility <= JavaVersion.VERSION_11) {
                checkOnlyModuleInfoInSrc(javaFiles)
                sourceCompatibility = JavaVersion.VERSION_11
            }
            if (targetCompatibility <= JavaVersion.VERSION_11) {
                checkOnlyModuleInfoInSrc(javaFiles)
                targetCompatibility = JavaVersion.VERSION_11
            }
        }
    }

    private fun checkOnlyModuleInfoInSrc(javaFiles: Set<File>) {
        if (javaFiles.size > 1) throw IllegalStateException(
            "tutteli-gradle-kotlin-module-info assumes you either have set source/targetCompatibility >= 11 or only have module-info.java to compile"
        )
    }
}
