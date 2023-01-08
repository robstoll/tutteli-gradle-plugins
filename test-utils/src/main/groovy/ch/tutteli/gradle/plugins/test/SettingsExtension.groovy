package ch.tutteli.gradle.plugins.test

import org.junit.jupiter.api.extension.*

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

class SettingsExtensionObject {
    public final Path tmpPath
    public final File tmp
    public final File settings
    public final File buildGradle
    public final File gpgKeyRing

    public final List<String> pluginClasspath

    SettingsExtensionObject(Path tmpPath) {
        this.tmpPath = tmpPath
        tmp = tmpPath.toFile()
        settings = new File(tmp, 'settings.gradle')
        buildGradle = new File(tmp, 'build.gradle')
        gpgKeyRing = new File(tmp, 'keyring.gpg')

        URL pluginClasspathResource = getClass().classLoader.getResource('plugin-classpath.txt')
        if (pluginClasspathResource == null) {
            throw new IllegalStateException('Did not find plugin classpath resource, run `createClasspathManifest` build task.')
        }

        pluginClasspath = pluginClasspathResource.readLines()
            .collect { it.replace('\\', '\\\\') }
            .collect { "\'${it}\'" }
    }

    String buildscriptWithKotlin(String kotlinVersion) {
        return """
        import org.gradle.api.tasks.testing.logging.TestLogEvent
        buildscript {
            repositories {
                maven { url "https://plugins.gradle.org/m2/" }
            }
            dependencies {
                classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion'
                classpath files($pluginClasspath)
            }
        }
        """
    }


    static String configureTestLogging() {
        return """
            tasks.withType(Test) {
                testLogging {
                    events TestLogEvent.FAILED,
                        TestLogEvent.PASSED,
                        TestLogEvent.SKIPPED,
                        TestLogEvent.STANDARD_OUT
                }
            }
            """
    }
}

class SettingsExtension implements ParameterResolver, AfterEachCallback, BeforeEachCallback {

    @Override
    boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == SettingsExtensionObject.class
    }

    @Override
    Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) {
        return getStore(context).getOrComputeIfAbsent("settingsSetup") {
            new SettingsExtensionObject(Files.createTempDirectory("myTests"))
        }
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        context.getStore(ExtensionContext.Namespace.create(this.class))
    }


    @Override
    void beforeEach(ExtensionContext context) throws Exception {
        // we also cleanup beforeEach just in case it did not work out as expected (we used to have shaky tests)
        afterEach(context)
    }

    @Override
    void afterEach(ExtensionContext context) throws Exception {
        SettingsExtensionObject settingsSetup = getStore(context).get("settingsSetup") as SettingsExtensionObject
        if (settingsSetup != null) {
            println("tmp folder is: $settingsSetup.tmpPath")
            deleteTmp(settingsSetup.tmpPath)
        }
    }

    static Path deleteTmp(Path tmpDir) {
        Files.walkFileTree(tmpDir, new SimpleFileVisitor<Path>() {

            @Override
            FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                return deleteAndContinue(file)
            }

            @Override
            FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return deleteAndContinue(dir)
            }

            private FileVisitResult deleteAndContinue(Path path) throws IOException {
                Files.delete(path)
                return FileVisitResult.CONTINUE
            }
        })
    }

}
