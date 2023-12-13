package org.breezyweather.sources.kma.kma

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KmaShortTermForecastResult(
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
        var resultMsg: String,
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
        var totalCount: Int,
    )

    @Serializable
    data class Items(
        @SerialName("item")
        var item: List<Item>
    )

    @Serializable
    data class Item(
        @SerialName("baseDate")
        var baseDate: String,
        @SerialName("baseTime")
        var baseTime: String,
        @SerialName("category")
        var category: String,
        @SerialName("fcstDate")
        var fcstDate: String,
        @SerialName("fcstTime")
        var fcstTime: String,
        @SerialName("fcstValue")
        var fcstValue: String,
        @SerialName("nx")
        var nx: Int,
        @SerialName("ny")
        var ny: Int,
    )
}

