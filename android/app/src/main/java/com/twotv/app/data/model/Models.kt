package com.twotv.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class MediaType {
    VIDEO,
    IMAGE,
    AUDIO,
    STREAM
}

@Entity(tableName = "paired_tvs")
data class PairedTv(
    @PrimaryKey val id: String, // uuid or ip:port
    val name: String,
    val ip: String,
    val port: Int,
    val pairingToken: String,
    val platform: String, // androidtv, webos, tizen, simulator, web
    val protocol: String = "http", // "http" or "ws"
    val isSelected: Boolean = false,
    val lastSeenAt: Long = System.currentTimeMillis(),
    val customName: String? = null
) {
    val displayName: String
        get() = if (!customName.isNullOrBlank()) customName else name
}


@Entity(tableName = "send_history")
data class SendHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val mediaType: MediaType,
    val saveToTv: Boolean,
    val targetTvName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true
)

@Serializable
data class MediaPayload(
    @SerialName("command") val command: String = "PLAY",
    @SerialName("mediaType") val mediaType: String,
    @SerialName("url") val url: String,
    @SerialName("title") val title: String,
    @SerialName("saveToTv") val saveToTv: Boolean = false,
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class PairingQrPayload(
    @SerialName("name") val name: String,
    @SerialName("ip") val ip: String = "127.0.0.1",
    @SerialName("port") val port: Int = 8080,
    @SerialName("pairingToken") val pairingToken: String,
    @SerialName("platform") val platform: String = "generic",
    @SerialName("protocol") val protocol: String = "http"
)

@Serializable
data class TvResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("message") val message: String
)
