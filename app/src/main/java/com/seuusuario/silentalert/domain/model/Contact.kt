package com.seuusuario.silentalert.domain.model

data class Contact(
    val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String,
    val channels: Set<AlertChannel> = setOf(AlertChannel.SMS)
)
