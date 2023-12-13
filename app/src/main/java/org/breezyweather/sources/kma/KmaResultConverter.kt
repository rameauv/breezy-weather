package org.breezyweather.sources.kma

import android.content.Context
import org.breezyweather.R
import org.breezyweather.common.basic.models.weather.*
import org.breezyweather.common.basic.wrappers.HourlyWrapper
import org.breezyweather.common.basic.wrappers.WeatherWrapper
import org.breezyweather.common.exceptions.WeatherException
import org.breezyweather.sources.kma.kma.*
import java.util.*

data class KmaMainSourceData(
    val veryShortTermForecast: KmaVeryShortTermForecastResult.Body,
    val shortTermForecast: KmaShortTermForecastResult.Body,
)

class KmaResultConverter {
    companion object {

        @Throws(WeatherException::class)
        fun convert(
            context: Context,
            veryShortTermForecastResult: KmaVeryShortTermForecastResult,
            shortTermForecastResult: KmaShortTermForecastResult,
        ): WeatherWrapper {
            val mainSourceData = handleMainSourceDataErrors(
                veryShortTermForecastResult,
                shortTermForecastResult,
            )
            val hourlyForecast = getHourly(
                veryShortTermForecast = mainSourceData.veryShortTermForecast,
                shortTermForecast = mainSourceData.shortTermForecast,
                context = context,
            )
            return WeatherWrapper(
                hourlyForecast = hourlyForecast,
            )
        }

        @Throws(WeatherException::class)
        private fun handleMainSourceDataErrors(
            veryShortTermForecastResult: KmaVeryShortTermForecastResult,
            shortTermForecastResult: KmaShortTermForecastResult,
        ): KmaMainSourceData {
            val veryShortTermForecast = veryShortTermForecastResult.response.body
            val shortTermForecast = shortTermForecastResult.response.body
            if (veryShortTermForecast == null
                || shortTermForecast == null
            ) {
                throw WeatherException()
            }
            return KmaMainSourceData(
                veryShortTermForecast = veryShortTermForecast,
                shortTermForecast = shortTermForecast,
            )
        }

        private fun getHourly(
            veryShortTermForecast: KmaVeryShortTermForecastResult.Body,
            shortTermForecast: KmaShortTermForecastResult.Body,
            context: Context,
        ): List<HourlyWrapper> {
            val veryShortTermHourlyForecast =
                this.convertVeryShortTermForecast(result = veryShortTermForecast, context = context)
            val shortTermHourlyForecast =
                this.convertShortTermForecast(result = shortTermForecast, context = context)

            val lastVeryShortTermItem = veryShortTermHourlyForecast.lastOrNull()
            val startIndex = if (lastVeryShortTermItem == null) 0
            else shortTermHourlyForecast.indexOfFirst { item -> item.date > lastVeryShortTermItem.date }

            return veryShortTermHourlyForecast + shortTermHourlyForecast.subList(
                startIndex.coerceAtLeast(0), shortTermHourlyForecast.size
            )
        }

        private fun convertVeryShortTermForecast(
            result: KmaVeryShortTermForecastResult.Body,
            context: Context
        ): List<HourlyWrapper> {
            val items = result.items.item
            val hourly = ArrayList<HourlyWrapper>()
            val groupedItems = items.groupBy { it.fcstDate + it.fcstTime }
            for ((date, itemList) in groupedItems) {
                val valuesMap = itemList.associateBy { it.category }
                val t1h = valuesMap["T1H"]?.fcstValue ?: ""
                val rn1 = valuesMap["RN1"]?.fcstValue ?: ""
                val sky = valuesMap["SKY"]?.fcstValue ?: ""
                val reh = valuesMap["REH"]?.fcstValue ?: ""
                val pty = valuesMap["PTY"]?.fcstValue ?: ""
                val lgt = valuesMap["LGT"]?.fcstValue ?: ""
                val vec = valuesMap["VEC"]?.fcstValue ?: ""
                val wsd = valuesMap["WSD"]?.fcstValue ?: ""

                val hourlyItem = convertVeryShortTermItem(
                    fcstDate = date.substring(0, 8),
                    fcstTime = date.substring(8, 12),
                    t1h = t1h,
                    rn1 = rn1,
                    sky = sky,
                    vec = vec,
                    wsd = wsd,
                    pty = pty,
                    lgt = lgt,
                    reh = reh,
                    context = context,
                )

                hourly.add(hourlyItem)
            }

            return hourly
        }

        private fun convertVeryShortTermItem(
            fcstDate: String,
            fcstTime: String,
            t1h: String,
            rn1: String,
            sky: String,
            vec: String,
            wsd: String,
            pty: String,
            lgt: String,
            reh: String,
            context: Context,
        ): HourlyWrapper {
            val date = createDateFromFcstDateFcstTime(fcstDate, fcstTime)
            val rn1Value = rn1.substring(0, rn1.length - 2).toFloatOrNull() ?: 0f

            return HourlyWrapper(
                date = date,
                isDaylight = true,
                weatherText = getWeatherText(sky = sky, pty = pty, lgt = lgt, context = context),
                weatherCode = convertWeatherCode(sky = sky, pty = pty, lgt = lgt),
                temperature = Temperature(t1h.toFloatOrNull()),
                precipitation = Precipitation(rn1Value, 0.0f, rn1Value, 0.0f, 0.0f),
                precipitationProbability = PrecipitationProbability(0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
                wind = Wind(degree = vec.toFloatOrNull(), speed = wsd.toFloatOrNull()),
                relativeHumidity = reh.toFloatOrNull()
            )
        }

        private fun convertShortTermForecast(
            result: KmaShortTermForecastResult.Body,
            context: Context
        ): List<HourlyWrapper> {
            val items = result.items.item

            val hourly = ArrayList<HourlyWrapper>()
            val groupedItems = items.groupBy { it.fcstDate + it.fcstTime }

            for ((date, itemList) in groupedItems) {
                val valuesMap = itemList.associateBy { it.category }
                val pop = valuesMap["POP"]?.fcstValue ?: ""
                val pty = valuesMap["PTY"]?.fcstValue ?: ""
                val pcp = valuesMap["PCP"]?.fcstValue ?: ""
                val reh = valuesMap["REH"]?.fcstValue ?: ""
                val sno = valuesMap["SNO"]?.fcstValue ?: ""
                val sky = valuesMap["SKY"]?.fcstValue ?: ""
                val tmp = valuesMap["TMP"]?.fcstValue ?: ""
                val vec = valuesMap["VEC"]?.fcstValue ?: ""
                val wsd = valuesMap["WSD"]?.fcstValue ?: ""

                val hourlyItem = convertShortTermItem(
                    fcstDate = date.substring(0, 8),
                    fcstTime = date.substring(8, 12),
                    tmp = tmp,
                    pcp = pcp,
                    pop = pop,
                    sno = sno,
                    sky = sky,
                    vec = vec,
                    wsd = wsd,
                    pty = pty,
                    reh = reh,
                    context = context
                )

                hourly.add(hourlyItem)
            }

            return hourly
        }

        private fun convertShortTermItem(
            fcstDate: String,
            fcstTime: String,
            tmp: String,
            pcp: String,
            pop: String,
            sno: String,
            sky: String,
            vec: String,
            wsd: String,
            pty: String,
            reh: String,
            context: Context,
        ): HourlyWrapper {
            val date = createDateFromFcstDateFcstTime(fcstDate, fcstTime)
            val pcpValue = pcp.substring(0, pcp.length - 2).toFloatOrNull() ?: 0f
            val snoValue = sno.substring(0, sno.length - 2).toFloatOrNull() ?: 0f

            return HourlyWrapper(
                date = date,
                isDaylight = true,
                weatherText = getWeatherText(sky = sky, pty = pty, context = context),
                weatherCode = convertWeatherCode(sky, pty),
                temperature = Temperature(tmp.toFloatOrNull()),
                precipitation = Precipitation(pcpValue, 0.0f, pcpValue, 0.0f, 0.0f),
                precipitationProbability = PrecipitationProbability(
                    pop.toFloatOrNull(),
                    0.0f,
                    pop.toFloatOrNull(),
                    snoValue,
                    0.0f
                ),
                relativeHumidity = reh.toFloatOrNull(),
                wind = Wind(degree = vec.toFloatOrNull(), speed = wsd.toFloatOrNull())
            )
        }

        private fun convertWeatherCode(sky: String, pty: String, lgt: String = "0"): WeatherCode {
            if (lgt != "0") {
                return WeatherCode.THUNDER
            }
            if (pty != "0") {
                when (pty) {
                    "1" -> return WeatherCode.RAIN
                    "2" -> return WeatherCode.SLEET
                    "3" -> return WeatherCode.SNOW
                    "4" -> return WeatherCode.RAIN
                    "5" -> return WeatherCode.RAIN
                    "6" -> return WeatherCode.SLEET
                    "7" -> return WeatherCode.SNOW
                }
            }

            return when (sky) {
                "1" -> WeatherCode.CLEAR
                "4" -> WeatherCode.PARTLY_CLOUDY
                else -> WeatherCode.CLOUDY
            }
        }

        private fun createDateFromFcstDateFcstTime(fcstDate: String, fcstTime: String): Date {
            val timeZone = TimeZone.getTimeZone("Asia/Seoul")
            val cal = Calendar.getInstance(timeZone)
            cal.set(
                fcstDate.substring(0, 4).toInt(),
                fcstDate.substring(4, 6).toInt() - 1,
                fcstDate.substring(6, 8).toInt(),
                fcstTime.substring(0, 2).toInt(),
                fcstTime.substring(2, 4).toInt(),
                0
            )
            cal.set(Calendar.MILLISECOND, 0)
            return cal.time
        }

        private fun getWeatherText(context: Context, sky: String, pty: String, lgt: String = "0"): String {
            val weatherConditions = mutableListOf<String>()

            val lightning = if (lgt != "0") {
                weatherConditions.add(context.resources.getString(R.string.precipitation_thunderstorm))
            } else null

            val precipitation = when (pty) {
                "1" -> context.resources.getString(R.string.common_weather_text_rain)
                "2" -> context.resources.getString(R.string.common_weather_text_rain_snow_mixed)
                "3" -> context.resources.getString(R.string.common_weather_text_snow)
                "4" -> context.resources.getString(R.string.common_weather_text_rain_showers)
                "5" -> context.resources.getString(R.string.common_weather_text_rain_light)
                "6" -> context.resources.getString(R.string.common_weather_text_rain_snow_mixed_light)
                "7" -> context.resources.getString(R.string.common_weather_text_snow_light)
                else -> null
            }

            val skyCondition = when (sky) {
                "1" -> context.resources.getString(R.string.common_weather_text_clear_sky)
                "3" -> context.resources.getString(R.string.common_weather_text_cloudy)
                "4" -> context.resources.getString(R.string.common_weather_text_overcast)
                else -> null
            }

            if (precipitation != null) {
                weatherConditions.add(precipitation)
            } else if (skyCondition != null && lightning == null) {
                weatherConditions.add(skyCondition)
            }

            return weatherConditions.joinToString("/")
        }
    }
}