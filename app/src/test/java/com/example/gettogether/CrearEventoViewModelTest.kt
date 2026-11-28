package com.example.gettogether

import com.example.gettogether.model.Usuario
import com.example.gettogether.repository.EventoRepository
import com.example.gettogether.repository.UsuarioRepository
import com.example.gettogether.service.EventoApi
import com.example.gettogether.service.XanoRetrofit
import com.example.gettogether.viewmodel.CrearEventoViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class CrearEventoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = mockk<EventoApi>()
    private lateinit var vm: CrearEventoViewModel

    @Before
    fun setUp() {
        mockkObject(XanoRetrofit)
        every { XanoRetrofit.eventoApi } returns api
        vm = CrearEventoViewModel()
    }

    @After
    fun tearDown() {
        UsuarioRepository.cerrarSesion()
        unmockkAll()
    }

    private fun llenarFormularioValido() {
        vm.onNombreChange("Partido de tenis")
        vm.onTipoChange("Deporte")
        vm.onDescripcionChange("Dobles en el club")
        vm.onUbicacionChange("Las Condes")
        vm.onEtiquetasChange("#tenis, deporte")
        vm.onEdadMinimaChange("20")
        vm.onEdadMaximaChange("40")
        vm.onCupoChange("4")
    }

    @Test
    fun `formulario vacio no crea evento y muestra errores`() {
        assertNull(vm.crearEvento())
        val e = vm.errores.value
        assertTrue(vm.mostrarErrores.value)
        assertFalse(e.esValido)
        assertEquals("El nombre es obligatorio", e.nombre)
        assertEquals("Selecciona un tipo de evento", e.tipo)
        assertEquals("Añade una descripción", e.descripcion)
        assertEquals("Añade una ubicación", e.ubicacion)
    }

    @Test
    fun `los errores no se muestran antes del primer intento`() {
        vm.onNombreChange("")
        assertFalse(vm.mostrarErrores.value)
        assertEquals("El nombre es obligatorio", vm.errores.value.nombre)
    }

    @Test
    fun `edad minima mayor que maxima es error`() {
        vm.onEdadMinimaChange("40")
        vm.onEdadMaximaChange("20")
        assertEquals("La edad mínima no puede ser mayor que la máxima", vm.errores.value.edad)
    }

    @Test
    fun `cupo cero o vacio es error`() {
        vm.onCupoChange("0")
        assertEquals("El cupo debe ser mayor a 0", vm.errores.value.cupo)
        vm.onCupoChange("")
        assertEquals("El cupo debe ser mayor a 0", vm.errores.value.cupo)
        vm.onCupoChange("5")
        assertNull(vm.errores.value.cupo)
    }

    @Test
    fun `imagenes maximo cinco y se pueden quitar`() {
        vm.agregarImagenes(listOf("a", "b"))
        vm.agregarImagenes(listOf("c", "d", "e", "f", "g"))
        assertEquals(CrearEventoViewModel.MAX_IMAGENES, vm.form.value.imagenes.size)
        vm.quitarImagen("a")
        assertFalse("a" in vm.form.value.imagenes)
    }

    @Test
    fun `formulario valido crea el evento a nombre del usuario`() {
        UsuarioRepository.instancia.registrar(Usuario(nombreUsuario = "tester", correo = "t${System.nanoTime()}@t.cl"))
        llenarFormularioValido()

        val evento = vm.crearEvento()

        assertNotNull(evento)
        assertEquals("Partido de tenis", evento!!.nombre)
        assertEquals("tester", evento.creador)
        assertEquals(listOf("tenis", "deporte"), evento.etiquetas)
        assertNotNull(EventoRepository.obtenerEvento(evento.id))
        assertTrue(vm.errores.value.esValido)
    }

    @Test
    fun `sin sesion el creador es invitado`() {
        UsuarioRepository.cerrarSesion()
        llenarFormularioValido()
        assertEquals("invitado", vm.crearEvento()!!.creador)
    }

    @Test
    fun `guardar en Xano ok avisa evento creado`() {
        val dto = eventoDto(id = 0)
        coEvery { api.crearEvento(dto) } returns Response.success(dto.copy(id = 50))

        var creado = false
        var error: String? = null
        vm.guardarEventoEnXano(dto, onEventoCreado = { creado = true }, onError = { error = it })

        assertTrue(creado)
        assertNull(error)
    }

    @Test
    fun `guardar en Xano con error avisa el mensaje`() {
        val dto = eventoDto(id = 0)
        coEvery { api.crearEvento(dto) } returns respuestaError(400)

        var error: String? = null
        vm.guardarEventoEnXano(dto, onEventoCreado = {}, onError = { error = it })

        assertTrue(error!!.contains("400"))
    }
}