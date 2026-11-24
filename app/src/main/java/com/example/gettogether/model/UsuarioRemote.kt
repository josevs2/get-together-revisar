package com.example.gettogether.model

import com.google.gson.annotations.SerializedName

data class UsuarioDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String? = null,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)
