package com.example.gettogether

import com.example.gettogether.model.Clima
import com.example.gettogether.model.ClimaEstado
import com.example.gettogether.repository.ClimaRepository
import com.example.gettogether.viewmodel.ClimaViewModel
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ClimaViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo = mockk<ClimaRepository>()

    @Test
    fun `estado inicial es cargando`() {
        assertEquals(ClimaEstado.Cargando, ClimaViewModel(repo).estado.value)
    }

    @Test
    fun `carga exitosa muestra el clima`() {
        val clima = Clima(temperatura = 20.0, viento = 5.0, descripcion = "Despejado")
        coEvery { repo.obtenerClima(any(), any()) } returns Result.success(clima)

        val vm = ClimaViewModel(repo)
        vm.cargar(-33.45, -70.66)

        assertEquals(ClimaEstado.Exito(clima), vm.estado.value)
    }

    @Test
    fun `error de red muestra mensaje de error`() {
        coEvery { repo.obtenerClima(any(), any()) } returns Result.failure(IOException())

        val vm = ClimaViewModel(repo)
        vm.cargar(-33.45, -70.66)

        val estado = vm.estado.value
        assertTrue(estado is ClimaEstado.Error)
        assertEquals("No se pudo obtener el clima", (estado as ClimaEstado.Error).mensaje)
    }
}