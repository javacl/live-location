package com.live.location.model

import androidx.annotation.Keep

@Keep
data class LiveLocationModel(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val time: Long = 0L,
    val deviceId: String = ""
)
