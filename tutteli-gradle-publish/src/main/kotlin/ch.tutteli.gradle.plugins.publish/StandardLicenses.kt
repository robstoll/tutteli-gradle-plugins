package ch.tutteli.gradle.plugins.publish

import java.net.URL

enum class StandardLicenses(val shortName: String, val longName: String, val url: URL) {
    APACHE_2_0(
        "Apache-2.0",
        "The Apache Software License, Version 2.0",
        URL("http://www.apache.org/licenses/LICENSE-2.0.txt")
    ),
    EUPL_1_2(
        "EUPL-1.2",
        "European Union Public Licence, Version 1.2",
        URL("https://joinup.ec.europa.eu/collection/eupl/eupl-text-11-12")
    );

    companion object {
        fun fromShortName(shortName: String): StandardLicenses =
            values().find { it.shortName == shortName } ?: throw IllegalArgumentException(
                "$shortName is not a valid standard license, expected one of ${values().joinToString(",")}}"
            )
    }
}
