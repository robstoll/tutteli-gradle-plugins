plugins {
    id("build-logic.kotlin-dsl-gradle-plugin")
    alias(buildLibs.plugins.build.parameters)
}

buildParameters {
    pluginId("build-logic.build-params")

    // Other plugins can contribute parameters, so below list is not exhaustive, hence we disable the validation
    enableValidation.set(false)

    string("defaultJdkVersion") {
        defaultValue.set("1.8")
        mandatory.set(true)
        description.set("Default Java version for source and target compatibility")
    }

    group("kotlin") {
        string("version") {
            fromEnvironment()
            defaultValue.set("1.8.0")
            description.set("kotlin version")
        }
        bool("werror") {
            defaultValue.set(true)
            description.set("Treat kotlinc warnings as errors")
        }
    }


    group("java") {
        integer("version") {
            fromEnvironment()
            defaultValue.set(11)
            description.set("Java version used for java.toolchain")
        }
        bool("werror") {
            defaultValue.set(true)
            description.set("Treat javac, javadoc, warnings as errors")
        }
    }

}
