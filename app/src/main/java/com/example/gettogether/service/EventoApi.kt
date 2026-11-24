package com.example.gettogether.service

import com.example.gettogether.model.EventoDto
import retrofit2.Response
import retrofit2.http.*

interface EventoApi {

    @GET("evento")
    suspend fun obtenerEventos(): Response<List<EventoDto>>

    @GET("evento/{evento_id}")
    suspend fun obtenerEventoPorId(@Path("evento_id") id: Int): Response<EventoDto>

    @POST("evento")
    suspend fun crearEvento(@Body evento: EventoDto): Response<EventoDto>

    @PATCH("evento/{evento_id}")
    suspend fun actualizarEvento(
        @Path("evento_id") id: Int,
        @Body evento: EventoDto
    ): Response<EventoDto>

    @DELETE("evento/{evento_id}")
    suspend fun eliminarEvento(@Path("evento_id") id: Int): Response<Unit>
}