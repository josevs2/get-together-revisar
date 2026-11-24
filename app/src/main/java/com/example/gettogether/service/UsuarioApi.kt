package com.example.gettogether.service

import com.example.gettogether.model.LoginRequest
import com.example.gettogether.model.UsuarioDto
import retrofit2.Response
import retrofit2.http.*

interface UsuarioApi {

    @POST("usuario")
    suspend fun registrarUsuario(@Body usuario: UsuarioDto): Response<UsuarioDto>

    @POST("usuario/login")
    suspend fun login(@Body credenciales: LoginRequest): Response<UsuarioDto>

    @GET("usuario/{usuario_id}")
    suspend fun obtenerUsuario(@Path("usuario_id") id: Int): Response<UsuarioDto>

    @PATCH("usuario/{usuario_id}")
    suspend fun actualizarUsuario(
        @Path("usuario_id") id: Int,
        @Body datos: Map<String, String>
    ): Response<UsuarioDto>
}