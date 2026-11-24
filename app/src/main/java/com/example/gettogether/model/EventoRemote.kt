package com.example.gettogether.model

import com.google.gson.annotations.SerializedName

data class EventoDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("titulo") val titulo: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("categoria") val categoria: String,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("hora") val hora: String,
    @SerializedName("ubicacion") val ubicacion: String,
    @SerializedName("latitud") val latitud: Double? = null,
    @SerializedName("longitud") val longitud: Double? = null,
    @SerializedName("cupo_maximo") val cupoMaximo: Int = 10,
    @SerializedName("imagen_url") val imagenUrl: String? = null,
    @SerializedName("creador_id") val creadorId: Int = 1
)