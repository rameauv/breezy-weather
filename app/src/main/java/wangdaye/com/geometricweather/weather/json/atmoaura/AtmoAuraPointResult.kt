package wangdaye.com.geometricweather.weather.json.atmoaura

import kotlinx.serialization.Serializable

/**
 * Atmo Aura
 */
@Serializable
data class AtmoAuraPointResult(
    val polluants: List<AtmoAuraPointPolluant>?
)