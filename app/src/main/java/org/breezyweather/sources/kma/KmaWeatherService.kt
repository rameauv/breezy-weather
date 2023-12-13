/**
 * This file is part of Breezy Weather.
 *
 * Breezy Weather is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Breezy Weather is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Breezy Weather. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.sources.kma

import android.content.Context
import android.graphics.Color
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Observable
import org.breezyweather.BuildConfig
import org.breezyweather.R
import org.breezyweather.common.basic.models.Location
import org.breezyweather.common.basic.wrappers.WeatherWrapper
import org.breezyweather.common.exceptions.ApiKeyMissingException
import org.breezyweather.common.extensions.getFormattedDate
import org.breezyweather.common.preference.EditTextPreference
import org.breezyweather.common.preference.Preference
import org.breezyweather.common.source.ConfigurableSource
import org.breezyweather.common.source.HttpSource
import org.breezyweather.common.source.MainWeatherSource
import org.breezyweather.common.source.SecondaryWeatherSourceFeature
import org.breezyweather.settings.SourceConfigStore
import retrofit2.Retrofit
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

class KmaWeatherService @Inject constructor(
    @ApplicationContext context: Context,
    client: Retrofit.Builder
) : HttpSource(), MainWeatherSource, ConfigurableSource {
    override val id = "kma"
    override val name = context.resources.getString(R.string.weather_source_kma_source_name)
    override val privacyPolicyUrl = "https://data.kma.go.kr/cmmn/static/staticPage.do?page=privacyPolicy"

    override val color = Color.rgb(0x0, 0x2f, 0x6c) // blue part of the 태극기
    override val weatherAttribution = context.resources.getString(R.string.weather_source_kma_source_name)

    private val mWeatherApi by lazy {
        client
            .baseUrl(KMA_WEATHER_BASE_URL)
            .build()
            .create(KmaWeatherApi::class.java)
    }

    override val supportedFeaturesInMain = listOf<SecondaryWeatherSourceFeature>()

    override fun requestWeather(
        context: Context,
        location: Location,
        ignoreFeatures: List<SecondaryWeatherSourceFeature>
    ): Observable<WeatherWrapper> {
        if (!isConfigured) {
            return Observable.error(ApiKeyMissingException())
        }
        val token = getApiKeyOrDefault()

        val shortTermForecastBaseDateTime = getShortTermForecastBaseDateTime()
        val veryShortTermForecastBaseDateTime = getVeryShortTermForecastBaseDateTime()
        val gridCoordinates = getGridCoordinates(location.longitude, location.latitude)

        val shortTermForecast = mWeatherApi.getShortTermForecast(
            token,
            1000,
            1,
            "JSON",
            shortTermForecastBaseDateTime.baseDate,
            shortTermForecastBaseDateTime.baseTime,
            gridCoordinates.x.toInt(),
            gridCoordinates.y.toInt()
        )

        val veryShortTermForecast = mWeatherApi.getVeryShortTermForecast(
            token,
            1000,
            1,
            "JSON",
            veryShortTermForecastBaseDateTime.baseDate,
            veryShortTermForecastBaseDateTime.baseTime,
            gridCoordinates.x.toInt(),
            gridCoordinates.y.toInt()
        )

        return Observable.zip(
            shortTermForecast,
            veryShortTermForecast,
        ) {
                shortTermForecastResult,
                veryShortTermForecastResult,
            ->
            KmaResultConverter.convert(
                context = context,
                shortTermForecastResult = shortTermForecastResult,
                veryShortTermForecastResult = veryShortTermForecastResult,
            )
        }
    }

    companion object {
        private const val KMA_WEATHER_BASE_URL = "https://apis.data.go.kr/"
    }

    private fun getVeryShortTermForecastBaseDateTime(): BaseDateTime {
        val timeZone = TimeZone.getTimeZone("Asia/Seoul")
        val baseTimeMinutes = 50

        val calendar = Calendar.getInstance(timeZone)
        val minutes = calendar[Calendar.MINUTE]
        if (minutes < baseTimeMinutes) {
            calendar.add(Calendar.HOUR, -1)
        }
        val date = calendar.time
        val hour = calendar[Calendar.HOUR_OF_DAY]

        return BaseDateTime(
            baseDate = date.getFormattedDate(timeZone, "yyyyMMdd"),
            baseTime = String.format("%02d50", hour),
        )
    }

    private fun getShortTermForecastBaseDateTime(): BaseDateTime {
        val timeZone = TimeZone.getTimeZone("Asia/Seoul")
        val baseTimes = arrayOf(2, 5, 8, 11, 14, 17, 20, 23)

        val calendar = Calendar.getInstance(timeZone)
        val hour = calendar[Calendar.HOUR_OF_DAY]

        val selectedBaseTime = baseTimes.lastOrNull { it <= hour } ?: baseTimes.last()
        calendar.add(Calendar.DATE, if (selectedBaseTime > hour) -1 else 0)
        val date = calendar.time

        return BaseDateTime(
            baseDate = date.getFormattedDate(timeZone, "yyyyMMdd"),
            baseTime = String.format("%02d00", selectedBaseTime),
        )
    }

    private fun getGridCoordinates(lon: Float, lat: Float): GridCoordinates {
        val map = LamcParameters()
        return mapConv(lon, lat, map)
    }

    private fun mapConv(lon: Float, lat: Float, map: LamcParameters): GridCoordinates {
        val res = this.lamcproj(lon, lat, map)
        return GridCoordinates(
            res.x + 1.5,
            res.y + 1.5,
        )
    }

    // Lambert conformal conic projection
    private fun lamcproj(lon: Float, lat: Float, map: LamcParameters): GridCoordinates {
        val pi = asin(1.0) * 2.0
        val degrad = pi / 180.0

        val re = map.re / map.grid
        val slat1 = map.slat1 * degrad
        val slat2 = map.slat2 * degrad
        val olon = map.olon * degrad
        val olat = map.olat * degrad

        var sn = tan(pi * 0.25 + slat2 * 0.5) / tan(pi * 0.25 + slat1 * 0.5)
        sn = ln(cos(slat1) / cos(slat2)) / ln(sn)
        var sf = tan(pi * 0.25 + slat1 * 0.5)
        sf = sf.pow(sn) * cos(slat1) / sn
        var ro = tan(pi * 0.25 + olat * 0.5)
        ro = re * sf / ro.pow(sn)

        var ra = tan(pi * 0.25 + lat * degrad * 0.5)
        ra = re * sf / ra.pow(sn)
        var theta = lon * degrad - olon
        if (theta > pi) theta -= 2.0 * pi
        if (theta < -pi) theta += 2.0 * pi
        theta *= sn
        return GridCoordinates(
            (ra * sin(theta)) + map.xo,
            (ro - ra * cos(theta)) + map.yo,
        )
    }

    // CONFIG
    private val config = SourceConfigStore(context, id)
    private var apikey: String
        set(value) {
            config.edit().putString("apikey", value).apply()
        }
        get() = config.getString("apikey", null) ?: ""

    private fun getApiKeyOrDefault(): String {
        return apikey.ifEmpty { BuildConfig.KMA_WEATHER_KEY }
    }

    override val isConfigured
        get() = getApiKeyOrDefault().isNotEmpty()

    override val isRestricted = false

    override fun getPreferences(context: Context): List<Preference> {
        return listOf(
            EditTextPreference(
                titleId = R.string.settings_weather_source_kma_api_key,
                summary = { c, content ->
                    content.ifEmpty {
                        c.getString(R.string.settings_source_default_value)
                    }
                },
                content = apikey,
                onValueChanged = {
                    apikey = it
                }
            ),
        )
    }
}

// Lambert conformal conic projection parameters
private data class LamcParameters(
    val re: Double = 6371.00877,
    val grid: Float = 5.0F,
    val slat1: Float = 30.0F,
    val slat2: Float = 60.0F,
    val olon: Float = 126.0F,
    val olat: Float = 38.0F,
    val xo: Float = 210 / grid,
    val yo: Float = 675 / grid,
)

private data class GridCoordinates(
    val x: Double,
    val y: Double,
)

private data class BaseDateTime(val baseDate: String, val baseTime: String)
