package com.example.gettogether

import com.example.gettogether.model.ClimaActual
import com.example.gettogether.model.ClimaRespuesta
import com.example.gettogether.repository.ClimaRepository
import com.example.gettogether.service.ClimaApi
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** API externa Open-Meteo con una ClimaApi falsa (MockK): no usa internet. */
class ClimaRepositoryTest {

    private val api = mockk<ClimaApi>()
    private val repo = ClimaRepository(api)

    @Test
    fun `respuesta correcta se convierte en Clima`() = runTest {
        coEvery { api.obtenerClima(any(), any(), any(), any()) } returns
                ClimaRespuesta(ClimaActual(temperatura = 18.4, codigo = 2, viento = 12.0))

        val clima = repo.obtenerClima(-33.45, -70.66).getOrThrow()

        assertEquals(18.4, clima.temperatura, 0.001)
        assertEquals(12.0, clima.viento, 0.001)
        assertEquals("Parcialmente nublado", clima.descripcion)
    }

    @Test
    fun `respuesta sin datos es un error`() = runTest {
        coEvery { api.obtenerClima(any(), any(), any(), any()) } returns ClimaRespuesta(null)
        assertTrue(repo.obtenerClima(0.0, 0.0).isFailure)
    }

    @Test
    fun `sin internet devuelve error y no se cae`() = runTest {
        coEvery { api.obtenerClima(any(), any(), any(), any()) } throws IOException("sin red")
        assertTrue(repo.obtenerClima(0.0, 0.0).isFailure)
    }

    @Test
    fun `codigos WMO se traducen al espanol`() {
        assertEquals("Despejado", ClimaRepository.describirCodigo(0))
        assertEquals("Nublado", ClimaRepository.describirCodigo(3))
        assertEquals("Neblina", ClimaRepository.describirCodigo(45))
        assertEquals("Llovizna", ClimaRepository.describirCodigo(53))
        assertEquals("Lluvia", ClimaRepository.describirCodigo(63))
        assertEquals("Nieve", ClimaRepository.describirCodigo(75))
        assertEquals("Tormenta", ClimaRepository.describirCodigo(95))
        assertEquals("Sin información", ClimaRepository.describirCodigo(1234))
    }
}