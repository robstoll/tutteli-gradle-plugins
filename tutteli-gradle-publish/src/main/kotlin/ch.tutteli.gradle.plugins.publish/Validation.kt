package ch.tutteli.gradle.plugins.publish

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property


fun checkNotNullNorBlank(value: Any?, valueDescription: String) {
    if ((value as? CharSequence).isNullOrBlank()) throwIllegalState(valueDescription)
}

fun checkExtensionPropertyPresentAndNotBlank(property: Property<String>, propertyName: String) {
    if (property.orNull.isNullOrBlank()) newIllegalStateForProperty(propertyName)
}

fun checkExtensionPropertyPresentNotEmpty(property: ListProperty<*>, propertyName: String) {
    if ((property.isPresent && property.get().isNotEmpty()).not()) newIllegalStateForProperty(propertyName)
}

fun newIllegalStateForProperty(propertyName: String): Nothing =
    throwIllegalState("${PublishPlugin.EXTENSION_NAME}.$propertyName")

fun throwIllegalState(description: String): Nothing =
    throw IllegalStateException("You need to define $description for publishing (empty or blank is considered to be undefined)")
