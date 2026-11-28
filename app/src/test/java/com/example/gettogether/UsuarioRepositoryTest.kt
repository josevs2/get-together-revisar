package com.example.gettogether

import com.example.gettogether.model.Ajustes
import com.example.gettogether.model.LoginRequest
import com.example.gettogether.model.Usuario
import com.example.gettogether.model.UsuarioDto
import com.example.gettogether.repository.UsuarioRepository
import com.example.gettogether.service.UsuarioApi
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/** Usuarios locales y las llamadas a Xano (con una UsuarioApi falsa: no usa internet). */
class UsuarioRepositoryTest {

    private val api = mockk<UsuarioApi>()
    private val repo = UsuarioRepository(api) // instancia nueva por prueba

    private val dtoAna = UsuarioDto(id = 3, nombre = "Ana", email = "ana@test.cl", telefono = null, avatarUrl = null)

    /** Usuario propio de las pruebas (no dependen de la cuenta demo). */
    private val local = Usuario(nombre = "Local", nombreUsuario = "local", correo = "local@test.cl", password = "123456")

    // ---------- Local ----------

    @Test
    fun `login local con usuario registrado`() {
        repo.registrar(local)
        repo.cerrarSesion()
        assertFalse(repo.login("local@test.cl", "mala"))
        assertNull(repo.usuarioActual)
        assertTrue(repo.login("LOCAL@test.cl", "123456"))
        assertEquals("local", repo.usuarioActual?.nombreUsuario)
    }

    @Test
    fun `registrar, existe y actualizar perfil`() {
        repo.registrar(Usuario(nombre = "Ana", nombreUsuario = "ana", correo = "ana@test.cl", password = "secreto"))
        assertTrue(repo.existeCorreo("ANA@test.cl"))
        assertTrue(repo.existeNombreUsuario("Ana"))
        assertFalse(repo.existeCorreo("otro@test.cl"))
        assertFalse(repo.existeNombreUsuario("otro"))

        repo.actualizarPerfil(repo.usuarioActual!!.copy(intereses = "trekking"))
        assertEquals("trekking", repo.usuarioActual?.intereses)

        repo.cerrarSesion()
        assertNull(repo.usuarioActual)
        assertTrue(repo.login("ana@test.cl", "secreto"))
        assertEquals("trekking", repo.usuarioActual?.intereses)
    }

    @Test
    fun `ajustes y sesion de Room`() = runTest {
        repo.actualizarAjustes(Ajustes(notificaciones = false))
        assertFalse(repo.ajustes.notificaciones)
        assertNull(repo.usuarioActivo.first())
    }

    // ---------- Login con Xano ----------

    @Test
    fun `loginRemoto ok guarda el usuario`() = runTest {
        coEvery { api.login(LoginRequest("ana@test.cl", "123")) } returns Response.success(dtoAna)

        assertTrue(repo.loginRemoto("ana@test.cl", "123").isSuccess)
        assertEquals("Ana", repo.usuarioActual?.nombre)
        assertEquals("ana", repo.usuarioActual?.nombreUsuario)
        assertEquals("", repo.usuarioActual?.telefono)
    }

    @Test
    fun `loginRemoto con error o sin internet falla`() = runTest {
        coEvery { api.login(any()) } returns respuestaError(401)
        assertTrue(repo.loginRemoto("a@b.cl", "x").isFailure)

        coEvery { api.login(any()) } throws IOException()
        assertTrue(repo.loginRemoto("a@b.cl", "x").isFailure)
        assertNull(repo.usuarioActual)
    }

    // ---------- Registro con Xano ----------

    @Test
    fun `registrarRemoto ok guarda el usuario`() = runTest {
        coEvery { api.registrarUsuario(any()) } returns Response.success(dtoAna.copy(telefono = "+569"))

        assertTrue(repo.registrarRemoto("Ana", "ana@test.cl", "123", "+569").isSuccess)
        assertEquals("+569", repo.usuarioActual?.telefono)
    }

    @Test
    fun `registrarRemoto con error o sin internet falla`() = runTest {
        coEvery { api.registrarUsuario(any()) } returns respuestaError(400)
        assertTrue(repo.registrarRemoto("Ana", "ana@test.cl", "123", null).isFailure)

        coEvery { api.registrarUsuario(any()) } throws IOException()
        assertTrue(repo.registrarRemoto("Ana", "ana@test.cl", "123", null).isFailure)
    }

    // ---------- Actualizar perfil en Xano ----------

    @Test
    fun `actualizarUsuarioRemoto ok cambia nombre y telefono`() = runTest {
        repo.registrar(local)
        coEvery { api.actualizarUsuario(1, any()) } returns
                Response.success(dtoAna.copy(nombre = "Demo Nuevo", telefono = "+56911"))

        assertTrue(repo.actualizarUsuarioRemoto(1, "Demo Nuevo", "+56911").isSuccess)
        assertEquals("Demo Nuevo", repo.usuarioActual?.nombre)
        assertEquals("+56911", repo.usuarioActual?.telefono)
    }

    @Test
    fun `actualizarUsuarioRemoto con error o sin internet falla`() = runTest {
        coEvery { api.actualizarUsuario(any(), any()) } returns respuestaError(404)
        assertTrue(repo.actualizarUsuarioRemoto(1, "x", "y").isFailure)

        coEvery { api.actualizarUsuario(any(), any()) } throws IOException()
        assertTrue(repo.actualizarUsuarioRemoto(1, "x", "y").isFailure)
    }

    // ---------- Instancia compartida (companion) ----------

    @Test
    fun `la instancia compartida es siempre la misma`() = runTest {
        assertTrue(UsuarioRepository.instancia === UsuarioRepository.instancia)

        val correo = "comp${System.nanoTime()}@test.cl"
        UsuarioRepository.registrar(Usuario(nombreUsuario = "comp", correo = correo, password = "123456"))
        assertTrue(UsuarioRepository.existeCorreo(correo))
        assertTrue(UsuarioRepository.existeNombreUsuario("comp"))
        assertEquals(correo, UsuarioRepository.usuarioActual?.correo)

        UsuarioRepository.actualizarPerfil(UsuarioRepository.usuarioActual!!.copy(nombre = "Comp"))
        assertEquals("Comp", UsuarioRepository.instancia.usuarioActual?.nombre)

        UsuarioRepository.actualizarAjustes(Ajustes(soloCercanos = true))
        assertTrue(UsuarioRepository.ajustes.soloCercanos)
        UsuarioRepository.actualizarAjustes(Ajustes())
        assertNull(UsuarioRepository.usuarioActivo.first())

        UsuarioRepository.cerrarSesion()
        assertNull(UsuarioRepository.usuarioActual)
        assertTrue(UsuarioRepository.login(correo, "123456"))
        UsuarioRepository.cerrarSesion()
    }
}