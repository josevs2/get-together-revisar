package com.example.gettogether

import com.example.gettogether.model.Mensaje
import com.example.gettogether.model.Usuario
import com.example.gettogether.repository.EventoRepository
import com.example.gettogether.repository.UsuarioRepository
import com.example.gettogether.service.EventoApi
import com.example.gettogether.service.XanoRetrofit
import com.example.gettogether.viewmodel.EventoViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/** Inicio, Bandeja, unirse/salir, chat y el CRUD con Xano desde el ViewModel. */
class EventoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = mockk<EventoApi>()
    private lateinit var usuario: String

    @Before
    fun setUp() {
        mockkObject(XanoRetrofit)
        every { XanoRetrofit.eventoApi } returns api
        // Un usuario distinto por prueba, para no depender de otras pruebas
        usuario = "u" + System.nanoTime()
        UsuarioRepository.instancia.registrar(Usuario(nombreUsuario = usuario, correo = "$usuario@test.cl"))
    }

    @After
    fun tearDown() {
        UsuarioRepository.cerrarSesion()
        unmockkAll()
    }

    /** ViewModel que al crearse recibe [eventos] desde Xano. */
    private fun vmConXano(vararg ids: Int): EventoViewModel {
        coEvery { api.obtenerEventos() } returns Response.success(ids.map { eventoDto(id = it, titulo = "Evento $it") })
        return EventoViewModel()
    }

    // ---------- Carga desde Xano ----------

    @Test
    fun `al crearse carga los eventos de Xano`() {
        val vm = vmConXano(1001, 1002)
        assertEquals(listOf(1001, 1002), vm.eventos.value.map { it.id })
        assertEquals("Evento 1001", vm.eventos.value.first().nombre)
        assertFalse(vm.cargando.value)
        assertNull(vm.error.value)
    }

    @Test
    fun `si Xano responde 404 muestra los eventos de ejemplo`() {
        coEvery { api.obtenerEventos() } returns respuestaError(404)
        val vm = EventoViewModel()
        assertEquals(EventoRepository.eventos, vm.eventos.value)
        assertFalse(vm.cargando.value)
    }

    @Test
    fun `sin internet muestra los eventos de ejemplo`() {
        coEvery { api.obtenerEventos() } throws IOException("sin red")
        val vm = EventoViewModel()
        assertTrue(vm.eventos.value.any { it.nombre == "Pokemon Go" })
    }

    // ---------- Unirse / salir / Bandeja ----------

    @Test
    fun `unirse y salir de un evento`() {
        val vm = vmConXano(1001, 1002)
        assertEquals(2, vm.eventosDisponibles.size)
        assertTrue(vm.eventosConfirmados.isEmpty())

        vm.unirse(1001)
        assertTrue(vm.estaConfirmado(vm.obtenerEvento(1001)!!))
        assertEquals(listOf(1001), vm.eventosConfirmados.map { it.id })

        vm.salir(1001)
        assertFalse(vm.estaConfirmado(vm.obtenerEvento(1001)!!))
    }

    @Test
    fun `no se puede unir sin cupos ni duplicarse`() {
        val vm = vmConXano(1001) // cupo 3
        vm.unirse(1001)
        vm.unirse(1001)
        assertEquals(1, vm.obtenerEvento(1001)!!.asistentes.count { it == usuario })

        // Se llena el cupo con otros dos usuarios y un tercero ya no entra
        listOf("a", "b", "c").forEach { otro ->
            UsuarioRepository.instancia.registrar(Usuario(nombreUsuario = otro + usuario))
            vm.unirse(1001)
        }
        assertEquals(3, vm.obtenerEvento(1001)!!.asistentes.size)
        assertEquals(0, vm.obtenerEvento(1001)!!.cuposDisponibles)
    }

    @Test
    fun `obtenerEvento busca en Xano y luego en los locales`() {
        val vm = vmConXano(1001)
        assertEquals("Evento 1001", vm.obtenerEvento(1001)?.nombre)
        assertEquals("Pokemon Go", vm.obtenerEvento(1)?.nombre)
        assertNull(vm.obtenerEvento(-1))
    }

    // ---------- Chat ----------

    @Test
    fun `chat - mensaje vacio no se envia y el valido es propio`() {
        val vm = vmConXano(1001)
        assertFalse(vm.enviarMensaje(1001, "   "))
        assertTrue(vm.enviarMensaje(1001, "  hola  "))
        val mensaje = vm.mensajes(1001).last()
        assertEquals("hola", mensaje.texto)
        assertTrue(vm.esMensajePropio(mensaje))
        assertFalse(vm.esMensajePropio(Mensaje("otro", "x")))
    }

    @Test
    fun `sin sesion el usuario es invitado`() {
        UsuarioRepository.cerrarSesion()
        assertEquals("invitado", vmConXano().usuario)
    }

    // ---------- Editar / eliminar en Xano ----------

    @Test
    fun `editar evento ok recarga la lista y avisa`() {
        val vm = vmConXano(1001)
        val dto = eventoDto(id = 1001, titulo = "Editado")
        coEvery { api.actualizarEvento(1001, dto) } returns Response.success(dto)
        coEvery { api.obtenerEventos() } returns Response.success(listOf(dto))

        var avisado = false
        vm.editarEvento(1001, dto) { avisado = true }

        assertTrue(avisado)
        assertEquals("Editado", vm.obtenerEvento(1001)?.nombre)
        assertFalse(vm.cargando.value)
    }

    @Test
    fun `editar evento con error muestra el mensaje`() {
        val vm = vmConXano(1001)
        val dto = eventoDto(id = 1001)
        coEvery { api.actualizarEvento(1001, dto) } returns respuestaError(500)

        var avisado = false
        vm.editarEvento(1001, dto) { avisado = true }

        assertFalse(avisado)
        assertTrue(vm.error.value!!.contains("500"))
    }

    @Test
    fun `eliminar evento ok lo quita de la lista`() {
        val vm = vmConXano(1001, 1002)
        coEvery { api.eliminarEvento(1001) } returns Response.success(Unit)

        var avisado = false
        vm.eliminarEvento(1001) { avisado = true }

        assertTrue(avisado)
        assertEquals(listOf(1002), vm.eventos.value.map { it.id })
    }

    @Test
    fun `eliminar evento con error muestra el mensaje`() {
        val vm = vmConXano(1001)
        coEvery { api.eliminarEvento(1001) } throws IOException("sin red")

        vm.eliminarEvento(1001)

        assertEquals("sin red", vm.error.value)
        assertEquals(1, vm.eventos.value.size)
    }
}