package org.breezyweather.sources.accu.json

import kotlinx.serialization.Serializable

@Serializable
data class AccuCurrentWindGust(
    val Speed: AccuValueContainer?
)