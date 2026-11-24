package com.example.gettogether.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gettogether.model.Evento
import com.example.gettogether.model.EventoDto
import com.example.gettogether.model.EventoForm
import com.example.gettogether.model.EventoFormError
import com.example.gettogether.repository.EventoRepository
import com.example.gettogether.repository.UsuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CrearEventoViewModel : ViewModel() {

    companion object {
        const val MAX_IMAGENES = 5
    }

    private val _form = MutableStateFlow(EventoForm())
    val form: StateFlow<EventoForm> = _form.asStateFlow()

    private val _errores = MutableStateFlow(validar(EventoForm()))
    val errores: StateFlow<EventoFormError> = _errores.asStateFlow()

    private val _mostrarErrores = MutableStateFlow(false)
    val mostrarErrores: StateFlow<Boolean> = _mostrarErrores.asStateFlow()

    fun onNombreChange(v: String) {
        _form.value = _form.value.copy(nombre = v)
        _errores.value = validar(_form.value)
    }

    fun onTipoChange(v: String) {
        _form.value = _form.value.copy(tipo = v)
        _errores.value = validar(_form.value)
    }

    fun onDescripcionChange(v: String) {
        _form.value = _form.value.copy(descripcion = v)
        _errores.value = validar(_form.value)
    }

    fun onUbicacionChange(v: String) {
        _form.value = _form.value.copy(ubicacion = v)
        _errores.value = validar(_form.value)
    }

    fun onEtiquetasChange(v: String) {
        _form.value = _form.value.copy(etiquetas = v)
        _errores.value = validar(_form.value)
    }

    fun onEdadMinimaChange(v: String) {
        _form.value = _form.value.copy(edadMinima = v)
        _errores.value = validar(_form.value)
    }

    fun onEdadMaximaChange(v: String) {
        _form.value = _form.value.copy(edadMaxima = v)
        _errores.value = validar(_form.value)
    }

    fun onCupoChange(v: String) {
        _form.value = _form.value.copy(cupo = v)
        _errores.value = validar(_form.value)
    }

    fun agregarImagenes(nuevas: List<String>) {
        val actuales = _form.value.imagenes
        val combinadas = (actuales + nuevas).take(MAX_IMAGENES)
        _form.value = _form.value.copy(imagenes = combinadas)
    }

    fun quitarImagen(uri: String) {
        _form.value = _form.value.copy(imagenes = _form.value.imagenes - uri)
    }

    private fun validar(f: EventoForm): EventoFormError {
        val edadMin = f.edadMinima.toIntOrNull() ?: 0
        val edadMax = f.edadMaxima.toIntOrNull() ?: 99
        val cupoInt = f.cupo.toIntOrNull() ?: 0
        return EventoFormError(
            nombre = if (f.nombre.isBlank()) "El nombre es obligatorio" else null,
            tipo = if (f.tipo.isBlank()) "Selecciona un tipo de evento" else null,
            descripcion = if (f.descripcion.isBlank()) "Añade una descripción" else null,
            ubicacion = if (f.ubicacion.isBlank()) "Añade una ubicación" else null,
            edad = if (edadMin > edadMax) "La edad mínima no puede ser mayor que la máxima" else null,
            cupo = if (cupoInt <= 0) "El cupo debe ser mayor a 0" else null
        )
    }

    fun crearEvento(): Evento? {
        val errs = validar(_form.value)
        _errores.value = errs
        _mostrarErrores.value = true
        if (!errs.esValido) return null

        val creador = UsuarioRepository.usuarioActual?.nombreUsuario ?: "invitado"
        val evento = EventoRepository.crearEvento(_form.value, creador)
        return evento
    }

    fun guardarEventoEnXano(
        dto: EventoDto,
        onEventoCreado: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            EventoRepository.publicarEventoRemoto(dto)
                .onSuccess { onEventoCreado() }
                .onFailure { err -> onError(err.message ?: "Error al publicar evento") }
        }
    }
}
