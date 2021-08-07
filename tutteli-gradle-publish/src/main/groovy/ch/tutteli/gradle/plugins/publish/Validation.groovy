package ch.tutteli.gradle.plugins.publish

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

class Validation {
    protected static void requireNotNullNorBlank(Object value, String valueDescription) {
        if (!value?.trim()) throw newIllegalState(valueDescription)
    }

    protected static void requireExtensionPropertyPresentAndNotBlank(Property<String> property, String propertyName) {
        if (!property.getOrNull()?.trim()) throw newIllegalStateForProperty(propertyName)
    }

    protected static void requireExtensionPropertyPresentNotEmpty(ListProperty<?> property, String propertyName) {
        if (!property.map { !it.isEmpty() }) throw newIllegalStateForProperty(propertyName)
    }

    protected static IllegalStateException newIllegalStateForProperty(String propertyName) {
        return newIllegalState("${PublishPlugin.EXTENSION_NAME}.$propertyName")
    }

    protected static IllegalStateException newIllegalState(String description) {
        return new IllegalStateException(
            "You need to define $description for publishing (empty or blank is considered to be undefined)"
        )
    }

    static void throwIllegalPropertyNorSystemEnvSet(Property<String> propName, Property<String> envName) {
        throw newIllegalState("property with name ${propName.get()} or System.env variable with name ${envName.get()}")
    }
}
