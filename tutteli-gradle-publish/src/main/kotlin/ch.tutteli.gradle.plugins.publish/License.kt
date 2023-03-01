package ch.tutteli.gradle.plugins.publish

data class License(
    var shortName: String,
    var longName: String,
    var url: String,
    var distribution: String = PublishPluginExtension.DEFAULT_DISTRIBUTION
) : Comparable<License> {

    constructor(standardLicense: StandardLicenses, distribution: String = PublishPluginExtension.DEFAULT_DISTRIBUTION) :
        this(standardLicense.shortName, standardLicense.longName, standardLicense.url.toString(), distribution)

    override fun compareTo(other: License): Int = compareValuesBy(this, other,
        { it.shortName },
        { it.longName },
        { it.url },
        { it.distribution }
    )
}
