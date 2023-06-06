package wangdaye.com.geometricweather.weather.json.mf

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import wangdaye.com.geometricweather.common.serializer.DateSerializer
import java.util.Date

@Serializable
data class MfWarningComments(
    @SerialName("begin_time") @Serializable(DateSerializer::class) val beginTime: Date?,
    @SerialName("end_time") @Serializable(DateSerializer::class) val endTime: Date?,
    @SerialName("text_bloc_item") val textBlocItems: List<MfWarningTextBlocItem>?
)
