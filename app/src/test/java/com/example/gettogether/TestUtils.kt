package com.example.gettogether

import com.example.gettogether.model.EventoDto
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import retrofit2.Response

/**
 * Reemplaza Dispatchers.Main en las pruebas.
 * Sin esto, viewModelScope falla en JUnit porque no existe el hilo principal de Android.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/** Respuesta HTTP de error falsa (ej: 404 de Xano), sin usar internet. */
inline fun <reified T> respuestaError(codigo: Int = 404): Response<T> = mockk {
    every { isSuccessful } returns false
    every { code() } returns codigo
    every { body() } returns null
    every { message() } returns "Error $codigo"
}

/** Evento de prueba  buscando que lo devuelva como en xano*/
fun eventoDto(id: Int = 1, titulo: String = "Asado", latitud: Double? = null) = EventoDto(
    id = id,
    titulo = titulo,
    descripcion = "Descripción $titulo",
    categoria = "Comida",
    fecha = "2026-12-01",
    hora = "20:00",
    ubicacion = "Maipú",
    latitud = latitud,
    longitud = latitud,
    cupoMaximo = 3,
    imagenUrl = null,
    creadorId = 7
)