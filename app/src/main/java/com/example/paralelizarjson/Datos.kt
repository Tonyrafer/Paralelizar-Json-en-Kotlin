package com.example.paralelizarjson

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Datos(
    @SerialName("name") val first_name: String,
    @SerialName("language") val language: String,
    @SerialName("id") val id: String,
    @SerialName("bio") val bio: String,
    @SerialName("version") val version: Float,
)
