package org.breezyweather.sources.kma.kma

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KmaVeryShortTermForecastResult(
    @SerialName("response")
    var response: Response
) {
    @Serializable
    data class Response(
        @SerialName("header")
        var header: Header,
        @SerialName("body")
        var body: Body? = null
    )

    @Serializable
    data class Header(
        @SerialName("resultCode")
        var resultCode: String,
        @SerialName("resultMsg")
        var resultMsg: String
    )

    @Serializable
    data class Body(
        @SerialName("dataType")
        var dataType: String,
        @SerialName("items")
        var items: Items,
        @SerialName("pageNo")
        var pageNo: Int,
        @SerialName("numOfRows")
        var numOfRows: Int,
        @SerialName("totalCount")
        var totalCount: Int
    )

    @Serializable
    data class Items(
        @SerialName("item")
        var item: List<Item>
    )

    @Serializable
    data class Item(
        @SerialName("baseDate")
        val baseDate: String,
        @SerialName("baseTime")
        val baseTime: String,
        @SerialName("category")
        val category: String,
        @SerialName("fcstDate")
        val fcstDate: String,
        @SerialName("fcstTime")
        val fcstTime: String,
        @SerialName("fcstValue")
        val fcstValue: String,
        @SerialName("nx")
        val nx: Int,
        @SerialName("ny")
        val ny: Int,
    )
}
