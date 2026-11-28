package com.example.gettogether

import com.example.gettogether.model.EventoForm
import com.example.gettogether.model.FiltrosBusqueda
import com.example.gettogether.repository.EventoRepository
import com.example.gettogether.repository.toDomain
import com.example.gettogether.service.EventoApi
import com.example.gettogether.service.XanoRetrofit
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/** Datos locales, buscador y las llamadas CRUD a Xano (con una EventoApi falsa: no usa internet). */
class EventoRepositoryTest {

    private val api = mockk<EventoApi>()

    @Before
    fun setUp() {
        mockkObject(XanoRetrofit)
        every { XanoRetrofit.eventoApi } returns api
    }

    @After
    fun tearDown() = unmockkAll()

    private fun nuevoEvento(cupo: String = "10") = EventoRepository.crearEvento(
        EventoForm(nombre = " Prueba ", tipo = "Otro", descripcion = "d", ubicacion = "u",
            etiquetas = "#uno, dos, ", cupo = cupo),
        creador = "organizador"
    )

    // ---------- Datos locales ----------

    @Test
    fun `crearEvento limpia datos y agrega al creador`() {
        val e = nuevoEvento()
        assertEquals("Prueba", e.nombre)
        assertEquals("Prueba", e.titulo)
        assertEquals("Otro", e.categoria)
        assertEquals(listOf("uno", "dos"), e.etiquetas)
        assertEquals(listOf("organizador"), e.asistentes)
        assertEquals(e, EventoRepository.obtenerEvento(e.id))
    }

    @Test
    fun `eventos de ejemplo tienen titulo y categoria`() {
        val pokemon = EventoRepository.obtenerEvento(1)!!
        assertEquals("Pokemon Go", pokemon.titulo)
        assertEquals("Juegos", pokemon.categoria)
    }

    @Test
    fun `confirmar y cancelar asistencia`() {
        val e = nuevoEvento()
        EventoRepository.confirmarAsistencia(e.id, "ana")
        EventoRepository.confirmarAsistencia(e.id, "ana") // no se duplica
        assertEquals(2, EventoRepository.obtenerEvento(e.id)!!.asistentes.size)
        assertTrue(EventoRepository.eventosConfirmados("ana").any { it.id == e.id })

        EventoRepository.cancelarAsistencia(e.id, "ana")
        assertTrue(EventoRepository.eventosDisponibles("ana").any { it.id == e.id })
    }

    @Test
    fun `sin cupos no se puede confirmar`() {
        val e = nuevoEvento(cupo = "1") // el organizador ocupa el único cupo
        EventoRepository.confirmarAsistencia(e.id, "ana")
        assertFalse("ana" in EventoRepository.obtenerEvento(e.id)!!.asistentes)
    }

    @Test
    fun `mensajes del chat`() {
        val e = nuevoEvento()
        assertTrue(EventoRepository.mensajes(e.id).isEmpty())
        EventoRepository.enviarMensaje(e.id, "ana", "hola")
        assertEquals("hola", EventoRepository.mensajes(e.id).last().texto)
        assertTrue(EventoRepository.mensajes(1).isNotEmpty())
    }

    @Test
    fun `evento inexistente es null`() {
        assertNull(EventoRepository.obtenerEvento(-1))
    }

    // ---------- Buscador ----------

    @Test
    fun `buscar por texto, tipo y ubicacion`() {
        assertTrue(EventoRepository.buscar(FiltrosBusqueda(texto = "POKEMON")).any { it.id == 1 })
        assertTrue(EventoRepository.buscar(FiltrosBusqueda(tipo = "Juegos")).all { it.tipo == "Juegos" })
        assertTrue(EventoRepository.buscar(FiltrosBusqueda(ubicacion = "Ñuñoa")).any { it.id == 2 })
        assertTrue(EventoRepository.buscar(FiltrosBusqueda(ubicacion = "metro macul")).any { it.id == 1 })
        assertTrue(EventoRepository.buscar(FiltrosBusqueda(texto = "zzzz-no-existe")).isEmpty())
    }

    @Test
    fun `buscar por etiqueta, cupo, confirmados y edad`() {
        assertTrue(EventoRepository.buscar(FiltrosBusqueda(etiqueta = "#catan")).any { it.id == 3 })
        assertTrue(EventoRepository.buscar(FiltrosBusqueda(cupoMaximo = 10f)).all { it.cupo <= 10 })
        assertTrue(EventoRepository.buscar(FiltrosBusqueda(minConfirmados = 4f)).all { it.asistentes.size >= 4 })
        assertTrue(EventoRepository.buscar(FiltrosBusqueda(edadDesde = 40f)).none { it.id == 3 })
        assertTrue(EventoRepository.buscar(FiltrosBusqueda(edadDesde = 30f, edadHasta = 32f)).any { it.id == 3 })
    }

    // ---------- Conversión Xano -> app ----------

    @Test
    fun `toDomain convierte el evento de Xano`() {
        val e = eventoDto(id = 5, titulo = "Asado", latitud = -33.0).toDomain()
        assertEquals(5, e.id)
        assertEquals("Asado", e.nombre)
        assertEquals("Asado", e.titulo)
        assertEquals("Comida", e.tipo)
        assertEquals(3, e.cupo)
        assertEquals(-33.0, e.latitud!!, 0.0001)
        assertEquals("", e.imagenUrl)
    }

    @Test
    fun `toDomain usa coordenadas por defecto si vienen null`() {
        val e = eventoDto(latitud = null).toDomain()
        assertNotNull(e.latitud)
        assertNotNull(e.longitud)
    }

    // ---------- CRUD remoto (Xano) ----------

    @Test
    fun `cargarEventosRemotos ok, error HTTP y sin internet`() = runTest {
        coEvery { api.obtenerEventos() } returns Response.success(listOf(eventoDto()))
        assertEquals(1, EventoRepository.cargarEventosRemotos().getOrThrow().size)

        coEvery { api.obtenerEventos() } returns respuestaError(404)
        assertTrue(EventoRepository.cargarEventosRemotos().exceptionOrNull()!!.message!!.contains("404"))

        coEvery { api.obtenerEventos() } throws IOException("sin red")
        assertTrue(EventoRepository.cargarEventosRemotos().isFailure)
    }

    @Test
    fun `obtenerEventoPorIdRemoto ok, error HTTP y sin internet`() = runTest {
        coEvery { api.obtenerEventoPorId(1) } returns Response.success(eventoDto())
        assertEquals("Asado", EventoRepository.obtenerEventoPorIdRemoto(1).getOrThrow().titulo)

        coEvery { api.obtenerEventoPorId(1) } returns respuestaError(404)
        assertTrue(EventoRepository.obtenerEventoPorIdRemoto(1).isFailure)

        coEvery { api.obtenerEventoPorId(1) } throws IOException()
        assertTrue(EventoRepository.obtenerEventoPorIdRemoto(1).isFailure)
    }

    @Test
    fun `publicarEventoRemoto ok, error HTTP y sin internet`() = runTest {
        val dto = eventoDto(id = 0)
        coEvery { api.crearEvento(dto) } returns Response.success(dto.copy(id = 99))
        assertEquals(99, EventoRepository.publicarEventoRemoto(dto).getOrThrow().id)

        coEvery { api.crearEvento(dto) } returns respuestaError(400)
        assertTrue(EventoRepository.publicarEventoRemoto(dto).isFailure)

        coEvery { api.crearEvento(dto) } throws IOException()
        assertTrue(EventoRepository.publicarEventoRemoto(dto).isFailure)
    }

    @Test
    fun `actualizarEventoRemoto ok, error HTTP y sin internet`() = runTest {
        val dto = eventoDto(titulo = "Editado")
        coEvery { api.actualizarEvento(1, dto) } returns Response.success(dto)
        assertEquals("Editado", EventoRepository.actualizarEventoRemoto(1, dto).getOrThrow().titulo)

        coEvery { api.actualizarEvento(1, dto) } returns respuestaError(500)
        assertTrue(EventoRepository.actualizarEventoRemoto(1, dto).isFailure)

        coEvery { api.actualizarEvento(1, dto) } throws IOException()
        assertTrue(EventoRepository.actualizarEventoRemoto(1, dto).isFailure)
    }

    @Test
    fun `eliminarEventoRemoto ok, error HTTP y sin internet`() = runTest {
        coEvery { api.eliminarEvento(1) } returns Response.success(Unit)
        assertTrue(EventoRepository.eliminarEventoRemoto(1).isSuccess)

        coEvery { api.eliminarEvento(1) } returns respuestaError(404)
        assertTrue(EventoRepository.eliminarEventoRemoto(1).isFailure)

        coEvery { api.eliminarEvento(1) } throws IOException()
        assertTrue(EventoRepository.eliminarEventoRemoto(1).isFailure)
    }
}