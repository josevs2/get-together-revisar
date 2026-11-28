package com.example.gettogether

import com.example.gettogether.model.FiltrosBusqueda
import com.example.gettogether.viewmodel.BuscarViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuscarViewModelTest {

    private val vm = BuscarViewModel()

    @Test
    fun `resultados aplica los filtros`() {
        assertTrue(vm.resultados(FiltrosBusqueda()).map { it.id }.containsAll((1..7).toList()))
        assertTrue(vm.resultados(FiltrosBusqueda(tipo = "Juegos")).all { it.tipo == "Juegos" })
    }

    @Test
    fun `actualizar cambia los filtros`() {
        vm.actualizar { it.copy(tipo = "Deporte") }
        assertEquals("Deporte", vm.filtros.value.tipo)
    }

    @Test
    fun `limpiar filtros mantiene el texto`() {
        vm.actualizar { it.copy(texto = "pichanga", tipo = "Deporte", etiqueta = "futbol") }
        vm.limpiarFiltros()
        assertEquals(FiltrosBusqueda(texto = "pichanga"), vm.filtros.value)
    }
}