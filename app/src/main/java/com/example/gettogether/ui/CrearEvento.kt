package com.example.gettogether.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gettogether.model.EventoDto
import com.example.gettogether.viewmodel.CrearEventoViewModel
import com.example.gettogether.viewmodel.EventoViewModel
import com.example.gettogether.viewmodel.UsuarioViewModel

@Composable
fun CrearEvento(
    crearEventoViewModel: CrearEventoViewModel,
    eventoViewModel: EventoViewModel,
    usuarioViewModel: UsuarioViewModel,
    onVolver: () -> Unit
) {
    val authState by usuarioViewModel.uiState.collectAsState()
    val usuarioActual = authState.usuarioActual

    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("Deportes") }
    var fecha by remember { mutableStateOf("2026-11-30") }
    var hora by remember { mutableStateOf("19:00") }
    var ubicacion by remember { mutableStateOf("") }
    var cupoTexto by remember { mutableStateOf("10") }
    var imagenUri by remember { mutableStateOf<String?>(null) }

    var publicando by remember { mutableStateOf(false) }
    var errorFormulario by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Nuevo Evento",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = titulo,
            onValueChange = { titulo = it },
            label = { Text("Título del evento") },
            singleLine = true,
            enabled = !publicando,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            label = { Text("Descripción") },
            enabled = !publicando,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = categoria,
            onValueChange = { categoria = it },
            label = { Text("Categoría (Deportes, Juegos, Música...)") },
            singleLine = true,
            enabled = !publicando,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = fecha,
                onValueChange = { fecha = it },
                label = { Text("Fecha (AAAA-MM-DD)") },
                singleLine = true,
                enabled = !publicando,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = hora,
                onValueChange = { hora = it },
                label = { Text("Hora (HH:MM)") },
                singleLine = true,
                enabled = !publicando,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = ubicacion,
            onValueChange = { ubicacion = it },
            label = { Text("Ubicación") },
            singleLine = true,
            enabled = !publicando,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = cupoTexto,
            onValueChange = { cupoTexto = it.filter { ch -> ch.isDigit() } },
            label = { Text("Cupo máximo") },
            singleLine = true,
            enabled = !publicando,
            modifier = Modifier.fillMaxWidth()
        )

        errorFormulario?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val cupo = cupoTexto.toIntOrNull() ?: 0
                if (titulo.isBlank() || ubicacion.isBlank() || cupo <= 0) {
                    errorFormulario = "Completa título, ubicación y un cupo mayor a 0"
                    return@Button
                }

                errorFormulario = null
                publicando = true

                val nuevoDto = EventoDto(
                    titulo = titulo.trim(),
                    descripcion = descripcion.trim(),
                    categoria = categoria.trim(),
                    fecha = fecha.trim(),
                    hora = hora.trim(),
                    ubicacion = ubicacion.trim(),
                    cupoMaximo = cupo,
                    imagenUrl = imagenUri,
                    creadorId = usuarioActual?.id ?: 1
                )

                crearEventoViewModel.guardarEventoEnXano(
                    dto = nuevoDto,
                    onEventoCreado = {
                        publicando = false
                        eventoViewModel.cargarEventos()
                        onVolver()
                    },
                    onError = { mensajeError ->
                        publicando = false
                        errorFormulario = mensajeError
                    }
                )
            },
            enabled = !publicando,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (publicando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Publicar Evento")
            }
        }

        OutlinedButton(
            onClick = onVolver,
            enabled = !publicando,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancelar")
        }
    }
}
