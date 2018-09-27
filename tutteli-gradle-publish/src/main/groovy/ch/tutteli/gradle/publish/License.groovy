package ch.tutteli.gradle.publish

import groovy.transform.Sortable
import groovy.transform.ToString

import javax.inject.Inject


enum StandardLicenses {
    APACHE_2_0('Apache-2.0', 'The Apache Software License, Version 2.0', 'http://www.apache.org/licenses/LICENSE-2.0.txt'),
    EUPL_1_2('EUPL-1.2', 'European Union Public Licence, Version 1.2', 'https://joinup.ec.europa.eu/collection/eupl/eupl-text-11-12')
    ;

    String shortName
    String longName
    String url

    StandardLicenses(String shortName, String longName, String url) {
        this.shortName = shortName
        this.longName = longName
        this.url = url
    }

    static StandardLicenses fromShortName(String shortName) {
        return values().find { it.shortName == shortName }
    }
}

interface License {
    String getShortName()

    String getLongName()

    String getUrl()

    String getDistribution()
}

@Sortable
@ToString
class LicenseImpl implements License {
    String shortName
    String longName
    String url
    String distribution

    @Inject
    LicenseImpl() {}

    LicenseImpl(StandardLicenses standardLicenses, String distribution) {
        shortName = standardLicenses.shortName
        longName = standardLicenses.longName
        url = standardLicenses.url
        this.distribution = distribution
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        LicenseImpl license = (LicenseImpl) o

        if (shortName != license.shortName) return false
        if (longName != license.longName) return false
        if (url != license.url) return false
        if (distribution != license.distribution) return false

        return true
    }

    int hashCode() {
        int result
        result = (shortName != null ? shortName.hashCode() : 0)
        result = 31 * result + (longName != null ? longName.hashCode() : 0)
        result = 31 * result + (url != null ? url.hashCode() : 0)
        result = 31 * result + (distribution != null ? distribution.hashCode() : 0)
        return result
    }
}
