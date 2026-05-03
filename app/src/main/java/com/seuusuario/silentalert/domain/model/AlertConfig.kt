package com.seuusuario.silentalert.domain.model

data class AlertConfig(
    val panicPassword: String,
    val message: String = "ALERTA: Estou em perigo. Esta mensagem foi enviada automaticamente.",
    val contacts: List<Contact> = emptyList()
)
