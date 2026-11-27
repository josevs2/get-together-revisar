package com.example.gettogether.service

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object XanoRetrofit {

    private const val URL_EVENTOS = "https://x8ki-letl-twmt.n7.xano.io/api:eVoN_0_W/"
    private const val URL_USUARIOS = "https://x8ki-letl-twmt.n7.xano.io/api:T5qoFLJM/"

    private fun crear(url: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    val eventoApi: EventoApi by lazy { crear(URL_EVENTOS).create(EventoApi::class.java) }
    val usuarioApi: UsuarioApi by lazy { crear(URL_USUARIOS).create(UsuarioApi::class.java) }
}