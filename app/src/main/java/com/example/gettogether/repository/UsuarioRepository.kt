package com.example.gettogether.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.gettogether.model.Ajustes
import com.example.gettogether.model.LoginRequest
import com.example.gettogether.model.Usuario
import com.example.gettogether.model.UsuarioDto
import com.example.gettogether.model.UsuarioEntity
import com.example.gettogether.service.UsuarioApi
import com.example.gettogether.service.XanoRetrofit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class UsuarioRepository(private val api: UsuarioApi = XanoRetrofit.usuarioApi) {

    private var _usuarioActual by mutableStateOf<Usuario?>(null)
    val usuarioActual: Usuario? get() = _usuarioActual

    private var _ajustes by mutableStateOf(Ajustes())
    val ajustes: Ajustes get() = _ajustes

    private val usuariosRegistrados = mutableListOf(
        Usuario(
            nombre = "Usuario Demo",
            nombreUsuario = "demo",
            correo = "demo@gettogether.com",
            password = "password123",
            edad = 25
        )
    )

    val usuarioActivo: Flow<UsuarioEntity?> = flowOf(null)

    fun login(correo: String, pass: String): Boolean {
        val encontrado = usuariosRegistrados.find { it.correo.equals(correo, true) && it.password == pass }
        if (encontrado != null) {
            _usuarioActual = encontrado
            return true
        }
        return false
    }

    fun registrar(usuario: Usuario) {
        usuariosRegistrados.add(usuario)
        _usuarioActual = usuario
    }

    fun existeNombreUsuario(nombreUsuario: String): Boolean {
        return usuariosRegistrados.any { it.nombreUsuario.equals(nombreUsuario, true) }
    }

    fun existeCorreo(correo: String): Boolean {
        return usuariosRegistrados.any { it.correo.equals(correo, true) }
    }

    fun actualizarPerfil(usuario: Usuario) {
        _usuarioActual = usuario
        val index = usuariosRegistrados.indexOfFirst { it.correo.equals(usuario.correo, true) }
        if (index != -1) {
            usuariosRegistrados[index] = usuario
        }
    }

    fun actualizarAjustes(nuevosAjustes: Ajustes) {
        _ajustes = nuevosAjustes
    }

    fun cerrarSesion() {
        _usuarioActual = null
    }

    suspend fun loginRemoto(email: String, pass: String): Result<UsuarioDto> {
        return try {
            val response = api.login(LoginRequest(email, pass))
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                _usuarioActual = Usuario(
                    nombre = dto.nombre,
                    nombreUsuario = dto.email.substringBefore("@"),
                    correo = dto.email,
                    telefono = dto.telefono ?: "",
                    foto = dto.avatarUrl
                )
                Result.success(dto)
            } else {
                Result.failure(Exception("Error al iniciar sesión: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registrarRemoto(nombre: String, email: String, pass: String, telefono: String?): Result<UsuarioDto> {
        return try {
            val dto = UsuarioDto(nombre = nombre, email = email, password = pass, telefono = telefono)
            val response = api.registrarUsuario(dto)
            if (response.isSuccessful && response.body() != null) {
                val creado = response.body()!!
                _usuarioActual = Usuario(
                    nombre = creado.nombre,
                    nombreUsuario = creado.email.substringBefore("@"),
                    correo = creado.email,
                    telefono = creado.telefono ?: "",
                    foto = creado.avatarUrl
                )
                Result.success(creado)
            } else {
                Result.failure(Exception("Error al registrar: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizarUsuarioRemoto(id: Int, nombre: String, telefono: String): Result<UsuarioDto> {
        return try {
            val response = api.actualizarUsuario(id, mapOf("nombre" to nombre, "telefono" to telefono))
            if (response.isSuccessful && response.body() != null) {
                val actualizado = response.body()!!
                _usuarioActual = _usuarioActual?.copy(nombre = actualizado.nombre, telefono = actualizado.telefono ?: "")
                Result.success(actualizado)
            } else {
                Result.failure(Exception("Error al actualizar: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private val defaultInstance = UsuarioRepository()

        val usuarioActual: Usuario? get() = defaultInstance.usuarioActual
        val ajustes: Ajustes get() = defaultInstance.ajustes
        val usuarioActivo: Flow<UsuarioEntity?> get() = defaultInstance.usuarioActivo

        fun login(correo: String, pass: String): Boolean = defaultInstance.login(correo, pass)
        fun registrar(usuario: Usuario) = defaultInstance.registrar(usuario)
        fun existeNombreUsuario(nombreUsuario: String): Boolean = defaultInstance.existeNombreUsuario(nombreUsuario)
        fun existeCorreo(correo: String): Boolean = defaultInstance.existeCorreo(correo)
        fun actualizarPerfil(usuario: Usuario) = defaultInstance.actualizarPerfil(usuario)
        fun actualizarAjustes(nuevosAjustes: Ajustes) = defaultInstance.actualizarAjustes(nuevosAjustes)
        fun cerrarSesion() = defaultInstance.cerrarSesion()
        suspend fun loginRemoto(email: String, pass: String) = defaultInstance.loginRemoto(email, pass)
        suspend fun registrarRemoto(nombre: String, email: String, pass: String, telefono: String?) = defaultInstance.registrarRemoto(nombre, email, pass, telefono)
        suspend fun actualizarUsuarioRemoto(id: Int, nombre: String, telefono: String) = defaultInstance.actualizarUsuarioRemoto(id, nombre, telefono)
    }
}
