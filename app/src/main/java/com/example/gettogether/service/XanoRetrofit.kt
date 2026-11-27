package com.example.gettogether.service

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object XanoRetrofit {
    private const val BASE_URL = "https://x8ki-letl-twmt.n7.xano.io/api:workspace:vZpMOoRj/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val usuarioApi: UsuarioApi by lazy {
        retrofit.create(UsuarioApi::class.java)
    }

    val eventoApi: EventoApi by lazy {
        retrofit.create(EventoApi::class.java)
    }
}