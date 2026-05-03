package com.seuusuario.silentalert.data.remote.api

import com.seuusuario.silentalert.data.remote.dto.AlertPayloadDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface NotificationApi {
    @POST("api/v1/alert/dispatch")
    suspend fun dispatchAlert(@Body payload: AlertPayloadDto): Response<Unit>
}
