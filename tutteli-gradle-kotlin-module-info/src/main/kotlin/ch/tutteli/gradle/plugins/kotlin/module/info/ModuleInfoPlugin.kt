package ch.tutteli.gradle.plugins.kotlin.module.info

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.the
import java.io.File

class ModuleInfoPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        if (JavaVersion.current() >= JavaVersion.VERSION_11) {
            project.afterEvaluate {
                if (project.tasks.findByName("compileJava") == null) throw IllegalStateException(
                    """\
                    There is no compileJava task. Did you forget to apply the kotlin plugin?
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
                    "no module-info.java found in compileJava.source of project ${project.name}, following the first 10 files:\n${
                        javaFiles.take(10).let { it.ifEmpty { null } }?.joinToString("\n") ?: "no lines"
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
                ?: throw IllegalStateException("line starting with module in module-info.java did not specify moduleName")
            project.logger.info("tutteli-gradle-kotlin-module-info: using moduleName from module-info.java which is: $moduleName")
            moduleName
        }
        val java = project.the<JavaPluginExtension>()
        javaCompile.apply {
            inputs.property("moduleName", moduleName)
            options.apply {
                javaModuleVersion.set(project.provider { project.rootProject.version as String })
                val modulePath =
                    try {
                        java.sourceSets.getByName("main").output.asPath
                    } catch (e: NoSuchMethodError) {
                        //maybe a gradle 6.x user
                        @Suppress("DEPRECATION" /* JavaPluginConvention is deprecated but not yet in 6.x */)
                        project.convention.getPlugin(org.gradle.api.plugins.JavaPluginConvention::class.java)
                            .sourceSets.getByName("main")
                            .output
                            .asPath
                    }
                compilerArgs = listOf("--patch-module", "$moduleName=$modulePath")
            }
            if (JavaVersion.toVersion(sourceCompatibility) < JavaVersion.VERSION_11) {
                checkOnlyModuleInfoInSrc(javaFiles, sourceCompatibility, targetCompatibility)
                sourceCompatibility = JavaVersion.VERSION_11.toString()
            }
            if (JavaVersion.toVersion(targetCompatibility) < JavaVersion.VERSION_11) {
                checkOnlyModuleInfoInSrc(javaFiles, sourceCompatibility, targetCompatibility)
                targetCompatibility = JavaVersion.VERSION_11.toString()
            }
        }
        with(java) {
            modularity.inferModulePath.set(true)
        }
    }

    private fun checkOnlyModuleInfoInSrc(
        javaFiles: Set<File>,
        sourceCompatibility: String,
        targetCompatibility: String
    ) {
        if (javaFiles.size > 1) throw IllegalStateException(
            "tutteli-gradle-kotlin-module-info assumes you either have set java.toolchain.languageVersion or source/targetCompatibility >= 11 (was sourceCompatibility: $sourceCompatibility, targetCompatibility: $targetCompatibility) and in case not only have module-info.java to compile"
        )
    }
}
