package com.seuusuario.silentalert.data.remote.dto

data class AlertPayloadDto(
    val message: String,
    val recipients: List<RecipientDto>
)

data class RecipientDto(
    val name: String,
    val phone: String,
    val email: String,
    val channels: List<String>
)
