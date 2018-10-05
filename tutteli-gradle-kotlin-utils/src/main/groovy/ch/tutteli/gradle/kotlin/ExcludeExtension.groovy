package ch.tutteli.gradle.kotlin

import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency

class ExcludeExtension {
    private ExternalModuleDependency externalModuleDependency

    ExcludeExtension(ExternalModuleDependency externalModuleDependency) {
        this.externalModuleDependency = externalModuleDependency
    }

    void kotlin() {
        excludeKotlin(externalModuleDependency)
    }

    void kbox() {
        excludeKbox(externalModuleDependency)
    }

    void atriumVerbs() {
        excludeAtriumVerbs(externalModuleDependency)
    }

    ModuleDependency exclude(Map<String, String> excludeProperties){
        externalModuleDependency.exclude(excludeProperties)
    }

    static void excludeKotlin(ExternalModuleDependency externalModuleDependency) {
        externalModuleDependency.exclude group: 'org.jetbrains.kotlin'
    }

    static void excludeKbox(ExternalModuleDependency externalModuleDependency) {
        externalModuleDependency.exclude group: 'ch.tutteli.kbox'
    }

    static void excludeAtriumVerbs(ExternalModuleDependency externalModuleDependency) {
        externalModuleDependency.exclude group: 'ch.tutteli.atrium', module: 'atrium-verbs'
    }
}
