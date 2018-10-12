package ch.tutteli.gradle.test

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
    public final List<String> pluginClasspath

    SettingsExtensionObject(Path tmpPath) {
        this.tmpPath = tmpPath
        tmp = tmpPath.toFile()
        settings = new File(tmp, 'settings.gradle')
        buildGradle = new File(tmp, 'build.gradle')

        URL pluginClasspathResource = getClass().classLoader.getResource('plugin-classpath.txt')
        if (pluginClasspathResource == null) {
            throw new IllegalStateException('Did not find plugin classpath resource, run `testClasses` build task.')
        }

        pluginClasspath = pluginClasspathResource.readLines()
            .collect { it.replace('\\', '\\\\') }
            .collect { "\'${it}\'" }
    }
}

class SettingsExtension implements ParameterResolver, AfterEachCallback {

    @Override
    boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == SettingsExtensionObject.class
    }

    @Override
    Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) {
        return getStore(context)
            .getOrComputeIfAbsent("settingsSetup") {
            new SettingsExtensionObject(Files.createTempDirectory("myTests"))
        }
    }

    private ExtensionContext.Store getStore(ExtensionContext context) {
        context.getStore(ExtensionContext.Namespace.create(this.class, context))
    }

    @Override
    void afterEach(ExtensionContext context) throws Exception {
        SettingsExtensionObject settingsSetup = getStore(context).get("settingsSetup") as SettingsExtensionObject
        if (settingsSetup != null) {
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
