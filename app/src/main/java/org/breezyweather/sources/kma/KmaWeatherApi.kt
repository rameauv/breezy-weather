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

import io.reactivex.rxjava3.core.Observable
import org.breezyweather.sources.kma.kma.*
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * KMA API
 */
interface KmaWeatherApi {
    @GET("1360000/VilageFcstInfoService_2.0/getUltraSrtFcst")
    fun getVeryShortTermForecast(
        @Query(value = "serviceKey", encoded = true) token: String?,
        @Query("numOfRows") numberOfRows: Int,
        @Query("pageNo") pageNo: Int,
        @Query("dataType") dateType: String?,
        @Query("base_date") baseDate: String?,
        @Query("base_time") baseTime: String?,
        @Query("nx") x: Int,
        @Query("ny") y: Int
    ): Observable<KmaVeryShortTermForecastResult>

    @GET("1360000/VilageFcstInfoService_2.0/getVilageFcst")
    fun getShortTermForecast(
        @Query(value = "serviceKey", encoded = true) token: String?,
        @Query("numOfRows") numberOfRows: Int,
        @Query("pageNo") pageNo: Int,
        @Query("dataType") dataType: String?,
        @Query("base_date") baseDate: String?,
        @Query("base_time") baseTime: String?,
        @Query("nx") x: Int,
        @Query("ny") y: Int
    ): Observable<KmaShortTermForecastResult>
}