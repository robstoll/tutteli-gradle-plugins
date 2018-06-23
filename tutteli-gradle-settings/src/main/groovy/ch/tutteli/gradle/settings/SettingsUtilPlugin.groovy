package ch.tutteli.gradle.settings

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class SettingsUtilPluginExtension {
    private Settings settings
    private String currentFolder
    private String currentPrefix

    SettingsUtilPluginExtension(Settings settings, String currentFolder, String currentPrefix) {
        this.settings = settings
        this.currentFolder = currentFolder
        this.currentPrefix = currentPrefix
    }

    void folder(String folderName, Closure configure) {
        folder(folderName, "", configure)
    }

    void folder(String folderName, String additionalPrefix, Closure configure) {
        def innerExtension = new SettingsUtilPluginExtension(settings, "$currentFolder/$folderName", "$currentPrefix$additionalPrefix")
        def conf = configure.clone()
        conf.resolveStrategy = Closure.DELEGATE_FIRST
        conf.delegate = innerExtension
        conf.call()
    }

    void kotlinMulti(String folderName, String additionalPrefix) {
        folder(folderName, additionalPrefix) {
            prefixed("common", "js", "jvm")
        }
    }

    void prefixed(String... projects) {
        //we need a local variable if we want to keep the field private
        def currentPrefix = this.currentPrefix
        def prefixedProjects = projects.collect { "$currentPrefix$it" }
        IncludeCustomInFolder.includePrefixedInFolder(settings, currentFolder, *prefixedProjects)
    }

    void project(String... projectsWithoutPrefix) {
        IncludeCustomInFolder.includeCustomInFolder(settings, currentFolder, projectsWithoutPrefix)
    }

    def propertyMissing(String name) {
        prefixed(name)
    }

    def methodMissing(String name, arguments) {
        Object[] args = arguments as Object[]
        if (args.length == 1 && args[0] instanceof Closure) {
            folder(name, args[0] as Closure)
            return null
        } else if (args.length == 2 && args[0] instanceof CharSequence && args[1] instanceof Closure) {
            folder(name, args[0].toString(), args[1] as Closure)
        } else {
            throw new MissingMethodException(name, this.class, args)
        }
    }

    void _(String name) {
        prefixed(name)
    }
}

class SettingsUtilPlugin implements Plugin<Settings> {

    @Override
    void apply(Settings settings) {
        settings.extensions.create('include', SettingsUtilPluginExtension, settings, "", "")

        settings.ext.includeCustomInFolder = { String folder, String... customNames ->
            IncludeCustomInFolder.includeCustomInFolder(settings, folder, customNames)
        }

        settings.ext.includePrefixedInFolder = { String folder, String... namesWithoutPrefix ->
            IncludeCustomInFolder.includePrefixedInFolder(settings, folder, namesWithoutPrefix)
        }

        settings.ext.includePrefixed = { String... namesWithoutPrefix ->
            IncludeCustomInFolder.includePrefixedInFolder(settings, "", namesWithoutPrefix)
        }
    }
}
