package com.example.gettogether

import com.example.gettogether.model.RegistroForm
import com.example.gettogether.model.Usuario
import com.example.gettogether.model.UsuarioDto
import com.example.gettogether.repository.UsuarioRepository
import com.example.gettogether.service.UsuarioApi
import com.example.gettogether.viewmodel.UsuarioViewModel
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/** Login, Registro, Cuenta y Ajustes. Cada prueba usa un repositorio nuevo con una UsuarioApi falsa. */
class UsuarioViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = mockk<UsuarioApi>()
    private lateinit var repo: UsuarioRepository
    private lateinit var vm: UsuarioViewModel

    @Before
    fun setUp() {
        repo = UsuarioRepository(api)
        // Usuario propio de las pruebas (no dependen de la cuenta demo)
        repo.registrar(Usuario(nombre = "Cuenta", nombreUsuario = "cuenta", correo = "cuenta@test.cl", password = "123456"))
        repo.cerrarSesion()
        vm = UsuarioViewModel(repo)
    }

    private fun registroValido() = RegistroForm(
        nombre = "Ana", nombreUsuario = "ana", correo = "ana@test.cl", edad = "25",
        password = "secreto", confirmar = "secreto", aceptaTerminos = true
    )

    private fun entrarConCuenta() {
        vm.onLoginCorreoChange("cuenta@test.cl")
        vm.onLoginPasswordChange("123456")
        vm.iniciarSesion()
    }

    // ---------- Login con Xano (pantalla Login) ----------

    @Test
    fun `login con Xano ok`() {
        coEvery { api.login(any()) } returns
                Response.success(UsuarioDto(id = 3, nombre = "Ana", email = "ana@test.cl"))

        vm.login("ana@test.cl", "123")

        val estado = vm.uiState.value
        assertTrue(estado.operacionExitosa)
        assertFalse(estado.cargando)
        assertEquals("Ana", estado.usuarioActual?.nombre)
    }

    @Test
    fun `si Xano falla entra con la cuenta local`() {
        coEvery { api.login(any()) } throws IOException("sin red")

        vm.login("cuenta@test.cl", "123456")

        assertTrue(vm.uiState.value.operacionExitosa)
        assertEquals("cuenta", vm.usuarioActual?.nombreUsuario)
    }

    @Test
    fun `si Xano falla y la cuenta no existe muestra error`() {
        coEvery { api.login(any()) } throws IOException("sin red")

        vm.login("nadie@test.cl", "x")

        assertFalse(vm.uiState.value.operacionExitosa)
        assertEquals("sin red", vm.uiState.value.error)

        vm.limpiarEstadoNavegacion()
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `login sin mensaje de error usa el texto por defecto`() {
        coEvery { api.login(any()) } throws IOException()
        vm.login("nadie@test.cl", "x")
        assertEquals("Error al iniciar sesión", vm.uiState.value.error)
    }

    // ---------- Registro con Xano (pantalla Registro) ----------

    @Test
    fun `registro con Xano ok`() {
        coEvery { api.registrarUsuario(any()) } returns
                Response.success(UsuarioDto(id = 4, nombre = "Ana", email = "ana@test.cl"))

        vm.registrar("Ana", "ana@test.cl", "secreto", null)

        assertTrue(vm.uiState.value.operacionExitosa)
        assertEquals("ana", vm.usuarioActual?.nombreUsuario)
    }

    @Test
    fun `si Xano falla el registro se guarda local y luego permite login`() {
        coEvery { api.registrarUsuario(any()) } returns respuestaError(500)

        vm.registrar(" Ana ", "ana@test.cl", "secreto", "+569")

        assertTrue(vm.uiState.value.operacionExitosa)
        assertEquals("Ana", vm.usuarioActual?.nombre)
        vm.cerrarSesion()
        assertNull(vm.usuarioActual)
        assertTrue(repo.login("ana@test.cl", "secreto"))
    }

    // ---------- Login con formulario local ----------

    @Test
    fun `iniciarSesion valida campos vacios y datos incorrectos`() {
        assertFalse(vm.iniciarSesion())
        assertEquals("Completa correo y contraseña", vm.loginError.value)

        vm.onLoginCorreoChange("cuenta@test.cl")
        vm.onLoginPasswordChange("mala")
        assertFalse(vm.iniciarSesion())
        assertEquals("Correo o contraseña incorrectos", vm.loginError.value)
    }

    @Test
    fun `iniciarSesion correcto limpia el formulario`() {
        entrarConCuenta()
        assertNull(vm.loginError.value)
        assertEquals("", vm.login.value.correo)
        assertEquals("cuenta", vm.usuarioActual?.nombreUsuario)
    }

    // ---------- Registro con formulario local ----------

    @Test
    fun `registro vacio muestra todos los errores`() {
        assertFalse(vm.registrar())
        val e = vm.registroErrores.value
        assertTrue(vm.mostrarErroresRegistro.value)
        assertEquals("El nombre es obligatorio", e.nombre)
        assertEquals("Elige un nombre de usuario", e.nombreUsuario)
        assertEquals("Correo inválido", e.correo)
        assertEquals("Edad entre 18 y 99", e.edad)
        assertEquals("Mínimo 6 caracteres", e.password)
        assertEquals("Debes aceptar los términos", e.terminos)
    }

    @Test
    fun `registro detecta espacios, contrasenas distintas y duplicados`() {
        vm.onRegistroChange { registroValido().copy(nombreUsuario = "ana x", confirmar = "otra") }
        assertEquals("No puede tener espacios", vm.registroErrores.value.nombreUsuario)
        assertEquals("Las contraseñas no coinciden", vm.registroErrores.value.confirmar)

        vm.onRegistroChange { registroValido().copy(nombreUsuario = "CUENTA", correo = "cuenta@test.cl") }
        assertEquals("Ese nombre de usuario ya existe", vm.registroErrores.value.nombreUsuario)
        assertEquals("Ese correo ya está registrado", vm.registroErrores.value.correo)
    }

    @Test
    fun `registro valido guarda al usuario y limpia el formulario`() {
        vm.onRegistroChange { registroValido() }
        assertTrue(vm.registrar())
        assertEquals("ana", vm.usuarioActual?.nombreUsuario)
        assertEquals(RegistroForm(), vm.registro.value)
        assertFalse(vm.mostrarErroresRegistro.value)
    }

    // ---------- Cuenta ----------

    @Test
    fun `guardar perfil sin sesion da error`() {
        vm.cargarPerfil()
        assertEquals("Debes iniciar sesión", vm.guardarPerfil())
    }

    @Test
    fun `guardar perfil valida nombre, correo y edad`() {
        entrarConCuenta()
        vm.cargarPerfil()
        assertEquals("cuenta", vm.perfil.value.nombreUsuario)

        vm.onPerfilChange { it.copy(nombre = " ") }
        assertEquals("El nombre es obligatorio", vm.guardarPerfil())
        vm.onPerfilChange { it.copy(nombre = "Demo", correo = "malo") }
        assertEquals("Correo inválido", vm.guardarPerfil())
        vm.onPerfilChange { it.copy(correo = "cuenta@test.cl", edad = 10) }
        assertEquals("Edad entre 18 y 99", vm.guardarPerfil())
    }

    @Test
    fun `guardar perfil valido actualiza al usuario`() {
        entrarConCuenta()
        vm.cargarPerfil()
        vm.onPerfilChange { it.copy(intereses = "trekking") }
        assertNull(vm.guardarPerfil())
        assertEquals("trekking", vm.usuarioActual?.intereses)
    }

    // ---------- Ajustes y sesión ----------

    @Test
    fun `cambiar ajustes`() {
        vm.onAjustesChange { it.copy(notificaciones = false) }
        assertFalse(vm.ajustes.notificaciones)
    }

    @Test
    fun `cerrar sesion y sesion de Room`() {
        entrarConCuenta()
        vm.cerrarSesion()
        assertNull(vm.usuarioActual)
        assertNull(vm.sesionActiva.value)
    }

    @Test
    fun `loginSimulado usa Xano`() {
        coEvery { api.login(any()) } returns
                Response.success(UsuarioDto(id = 3, nombre = "Ana", email = "ana@test.cl"))
        vm.loginSimulado("ana@test.cl", "123")
        assertEquals("Ana", vm.usuarioActual?.nombre)
    }
}