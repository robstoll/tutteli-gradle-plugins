package ch.tutteli.gradle.bintray

import org.gradle.api.provider.Property

class Validation {
    protected static void requireNotNullNorEmpty(value, String valueDescription) {
        if (!value?.trim()) throw new IllegalStateException("You need to define $valueDescription for publishing (empty or blank is considered to be undefined)")
    }

    protected static void requirePresentAndNotEmpty(Property<String> property, String valueDescription) {
        if (!property.getOrNull()?.trim()) throw new IllegalStateException("You need to define $valueDescription for publishing (empty or blank is considered to be undefined)")
    }
}
