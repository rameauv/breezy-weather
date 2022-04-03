package wangdaye.com.geometricweather.main

import android.content.Context
import wangdaye.com.geometricweather.common.basic.models.Location
import wangdaye.com.geometricweather.common.utils.helpers.AsyncHelper
import wangdaye.com.geometricweather.db.DatabaseHelper
import wangdaye.com.geometricweather.location.LocationHelper
import wangdaye.com.geometricweather.weather.WeatherHelper
import wangdaye.com.geometricweather.weather.WeatherHelper.OnRequestWeatherListener
import java.util.concurrent.Executors
import javax.inject.Inject

class MainActivityRepository @Inject constructor(
    private val locationHelper: LocationHelper,
    private val weatherHelper: WeatherHelper
) {
    private val singleThreadExecutor = Executors.newSingleThreadExecutor()

    interface WeatherRequestCallback {
        fun onCompleted(
            location: Location,
            locationFailed: Boolean?,
            weatherRequestFailed: Boolean
        )
    }

    fun destroy() {
        cancelWeatherRequest()
    }

    fun initLocations(context: Context, formattedId: String): List<Location> {
        val list = DatabaseHelper.getInstance(context).readLocationList()

        list.firstOrNull {
            it.formattedId == formattedId
        }?.let {
            it.weather = DatabaseHelper.getInstance(context).readWeather(it)
            return list
        }

        list[0].weather = DatabaseHelper.getInstance(context).readWeather(list[0])
        return list
    }

    fun getWeatherCacheForLocations(
        context: Context,
        oldList: List<Location>,
        ignoredFormattedId: String,
        callback: AsyncHelper.Callback<List<Location>>
    ) {
        AsyncHelper.runOnExecutor({ emitter ->
            val newList = ArrayList(oldList)
            newList.forEach {
                if (it.formattedId != ignoredFormattedId) {
                    it.weather = DatabaseHelper.getInstance(context).readWeather(it)
                }
            }

            emitter.send(newList, true)
        }, callback, singleThreadExecutor)
    }

    fun writeLocation(context: Context, location: Location) {
        AsyncHelper.runOnExecutor({
            DatabaseHelper.getInstance(context).writeLocation(location)
            location.weather?.let {
                DatabaseHelper.getInstance(context).writeWeather(location, it)
            }
        }, singleThreadExecutor)
    }

    fun writeLocationList(context: Context, locationList: List<Location>) {
        AsyncHelper.runOnExecutor({ 
            DatabaseHelper.getInstance(context).writeLocationList(locationList)
        }, singleThreadExecutor)
    }

    fun writeLocationList(context: Context, locationList: List<Location>, newIndex: Int) {
        AsyncHelper.runOnExecutor({
            DatabaseHelper.getInstance(context).writeLocationList(locationList)

            locationList[newIndex].weather?.let {
                DatabaseHelper.getInstance(context).writeWeather(locationList[newIndex], it)
            }
        }, singleThreadExecutor)
    }

    fun deleteLocation(context: Context, location: Location) {
        AsyncHelper.runOnExecutor({
            DatabaseHelper.getInstance(context).deleteLocation(location)
            DatabaseHelper.getInstance(context).deleteWeather(location)
        }, singleThreadExecutor)
    }

    fun getWeather(
        context: Context,
        location: Location,
        locate: Boolean,
        callback: WeatherRequestCallback,
    ) {
        if (locate) {
            ensureValidLocationInformation(context, location, callback)
        } else {
            getWeatherWithValidLocationInformation(context, location, null, callback)
        }
    }

    private fun ensureValidLocationInformation(
        context: Context,
        location: Location,
        callback: WeatherRequestCallback,
    ) = locationHelper.requestLocation(
        context,
        location,
        false,
        object : LocationHelper.OnRequestLocationListener {

            override fun requestLocationSuccess(requestLocation: Location) {
                if (requestLocation.formattedId != location.formattedId) {
                    return
                }
                getWeatherWithValidLocationInformation(
                    context,
                    requestLocation,
                    false,
                    callback
                )
            }

            override fun requestLocationFailed(requestLocation: Location) {
                if (requestLocation.formattedId != location.formattedId) {
                    return
                }
                if (requestLocation.isUsable) {
                    getWeatherWithValidLocationInformation(
                        context,
                        requestLocation,
                        true,
                        callback
                    )
                }
            }
        }
    )

    private fun getWeatherWithValidLocationInformation(
        context: Context,
        location: Location,
        locationFailed: Boolean?,
        callback: WeatherRequestCallback,
    ) {
        weatherHelper.requestWeather(context, location, object : OnRequestWeatherListener {

            override fun requestWeatherSuccess(requestLocation: Location) {
                if (requestLocation.formattedId != location.formattedId) {
                    return
                }
                callback.onCompleted(
                    requestLocation,
                    locationFailed = locationFailed,
                    weatherRequestFailed = false
                )
            }

            override fun requestWeatherFailed(requestLocation: Location) {
                if (requestLocation.formattedId != location.formattedId) {
                    return
                }
                callback.onCompleted(
                    requestLocation,
                    locationFailed = locationFailed,
                    weatherRequestFailed = true
                )
            }
        })
    }

    fun getLocatePermissionList(context: Context) = locationHelper
        .getPermissions(context)
        .toList()

    fun cancelWeatherRequest() {
        locationHelper.cancel()
        weatherHelper.cancel()
    }
}