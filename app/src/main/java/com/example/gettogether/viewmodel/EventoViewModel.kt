package com.example.gettogether.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gettogether.model.Evento
import com.example.gettogether.model.EventoDto
import com.example.gettogether.model.Mensaje
import com.example.gettogether.repository.EventoRepository
import com.example.gettogether.repository.UsuarioRepository
import com.example.gettogether.repository.toDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventoViewModel(private val repository: EventoRepository = EventoRepository) : ViewModel() {

    private val _eventos = MutableStateFlow<List<Evento>>(emptyList())
    val eventos: StateFlow<List<Evento>> = _eventos.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        cargarEventos()
    }

    val usuario: String
        get() = UsuarioRepository.usuarioActual?.nombreUsuario ?: "invitado"

    val eventosDisponibles: List<Evento>
        get() {
            val lista = _eventos.value.ifEmpty { EventoRepository.eventos }
            return lista.filter { usuario !in it.asistentes }
        }

    val eventosConfirmados: List<Evento>
        get() {
            val lista = _eventos.value.ifEmpty { EventoRepository.eventos }
            return lista.filter { usuario in it.asistentes }
        }

    fun obtenerEvento(id: Int): Evento? {
        return _eventos.value.find { it.id == id } ?: EventoRepository.obtenerEvento(id)
    }

    fun estaConfirmado(evento: Evento): Boolean = usuario in evento.asistentes

    fun unirse(id: Int) {
        _eventos.value = _eventos.value.ifEmpty { EventoRepository.eventos }.map { e ->
            if (e.id == id && usuario !in e.asistentes && e.cuposDisponibles > 0)
                e.copy(asistentes = e.asistentes + usuario)
            else e
        }
        EventoRepository.confirmarAsistencia(id, usuario)
    }

    fun salir(id: Int) {
        _eventos.value = _eventos.value.ifEmpty { EventoRepository.eventos }.map { e ->
            if (e.id == id) e.copy(asistentes = e.asistentes - usuario) else e
        }
        EventoRepository.cancelarAsistencia(id, usuario)
    }

    fun mensajes(id: Int): List<Mensaje> = EventoRepository.mensajes(id)

    fun esMensajePropio(mensaje: Mensaje): Boolean = mensaje.autor == usuario

    fun enviarMensaje(id: Int, texto: String): Boolean {
        if (texto.isBlank()) return false
        EventoRepository.enviarMensaje(id, usuario, texto.trim())
        return true
    }

    fun cargarEventos() {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            repository.cargarEventosRemotos()
                .onSuccess { listaDto ->
                    _eventos.value = listaDto.map { it.toDomain() }
                }
                .onFailure { err ->
                    _error.value = err.message ?: "Error al cargar eventos"
                }
            _cargando.value = false
        }
    }

    fun editarEvento(id: Int, dtoActualizado: EventoDto, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _cargando.value = true
            repository.actualizarEventoRemoto(id, dtoActualizado)
                .onSuccess {
                    cargarEventos()
                    onSuccess()
                }
                .onFailure { err -> _error.value = err.message }
            _cargando.value = false
        }
    }

    fun eliminarEvento(id: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _cargando.value = true
            repository.eliminarEventoRemoto(id)
                .onSuccess {
                    _eventos.value = _eventos.value.filterNot { it.id == id }
                    onSuccess()
                }
                .onFailure { err -> _error.value = err.message }
            _cargando.value = false
        }
    }
}
