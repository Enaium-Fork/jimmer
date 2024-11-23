package org.babyfish.jimmer.sql.kt.model.embedded

import org.babyfish.jimmer.sql.Embeddable

@Embeddable
interface Location {

    val host: String

    /**
     * Network port
     */
    val port: Int?
}