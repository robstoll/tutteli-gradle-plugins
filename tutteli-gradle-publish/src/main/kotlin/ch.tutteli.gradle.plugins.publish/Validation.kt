package ch.tutteli.gradle.plugins.publish

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property


fun checkNotNullNorBlank(value: Any?, valueDescription: String) {
    if ((value as? CharSequence).isNullOrBlank()) throwIllegalStateException(valueDescription)
}

fun checkExtensionPropertyPresentAndNotBlank(property: Property<String>, propertyName: String) {
    if (property.orNull.isNullOrBlank()) throwIllegalStateExceptionForProperty(propertyName)
}

fun checkExtensionPropertyPresentNotEmpty(property: ListProperty<*>, propertyName: String) {
    if ((property.isPresent && property.get().isNotEmpty()).not()) throwIllegalStateExceptionForProperty(propertyName)
}

fun throwIllegalStateExceptionForProperty(propertyName: String): Nothing =
    throwIllegalStateException("${PublishPlugin.EXTENSION_NAME}.$propertyName")

fun throwIllegalStateException(description: String): Nothing =
    throw IllegalStateException("You need to define $description for publishing (empty or blank is considered to be undefined)")
