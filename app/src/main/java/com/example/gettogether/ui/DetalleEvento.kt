package com.example.gettogether.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gettogether.model.Evento
import com.example.gettogether.model.EventoDto
import com.example.gettogether.viewmodel.EventoViewModel
import com.example.gettogether.viewmodel.UsuarioViewModel

@Composable
fun DetalleEvento(
    eventoId: Int,
    eventoViewModel: EventoViewModel,
    usuarioViewModel: UsuarioViewModel,
    onVolver: () -> Unit
) {
    val eventos by eventoViewModel.eventos.collectAsState()
    val cargando by eventoViewModel.cargando.collectAsState()
    val authState by usuarioViewModel.uiState.collectAsState()

    val evento = eventos.find { it.id == eventoId }
    val usuarioActual = authState.usuarioActual
    val esCreador = evento != null && usuarioActual != null && evento.creadorId == usuarioActual.id

    var mostrarDialogoEditar by remember { mutableStateOf(false) }
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }

    if (evento == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("El evento no está disponible.")
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onVolver) { Text("Volver") }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = evento.titulo,
            style = MaterialTheme.typography.headlineMedium
        )

        AssistChip(
            onClick = {},
            label = { Text(evento.categoria) }
        )

        Text(
            text = "Fecha y hora: ${evento.fecha} a las ${evento.hora}",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "Ubicación: ${evento.ubicacion}",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "Cupo máximo: ${evento.cupoMaximo} asistentes",
            style = MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider()

        Text(
            text = "Descripción",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = evento.descripcion,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Acciones exclusivas del creador (PATCH y DELETE en Xano)
        if (esCreador) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { mostrarDialogoEditar = true },
                    enabled = !cargando,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Editar")
                }

                Button(
                    onClick = { mostrarDialogoEliminar = true },
                    enabled = !cargando,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Eliminar")
                }
            }
        }

        OutlinedButton(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }

    // Diálogo PATCH /evento/{id}
    if (mostrarDialogoEditar) {
        DialogoEditarEvento(
            evento = evento,
            onDismiss = { mostrarDialogoEditar = false },
            onConfirmarEdicion = { dtoActualizado ->
                eventoViewModel.editarEvento(
                    id = evento.id,
                    dtoActualizado = dtoActualizado,
                    onSuccess = { mostrarDialogoEditar = false }
                )
            }
        )
    }

    // Diálogo de confirmación DELETE /evento/{id}
    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("Eliminar evento") },
            text = { Text("¿Seguro que deseas eliminar \"${evento.titulo}\"? Esta acción borrará el registro en Xano.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoEliminar = false
                        eventoViewModel.eliminarEvento(evento.id) {
                            onVolver()
                        }
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEliminar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun DialogoEditarEvento(
    evento: Evento,
    onDismiss: () -> Unit,
    onConfirmarEdicion: (EventoDto) -> Unit
) {
    var titulo by remember { mutableStateOf(evento.titulo) }
    var descripcion by remember { mutableStateOf(evento.descripcion) }
    var ubicacion by remember { mutableStateOf(evento.ubicacion) }
    var fecha by remember { mutableStateOf(evento.fecha) }
    var hora by remember { mutableStateOf(evento.hora) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Evento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") }
                )
                OutlinedTextField(
                    value = ubicacion,
                    onValueChange = { ubicacion = it },
                    label = { Text("Ubicación") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = fecha,
                    onValueChange = { fecha = it },
                    label = { Text("Fecha") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = hora,
                    onValueChange = { hora = it },
                    label = { Text("Hora") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmarEdicion(
                        EventoDto(
                            id = evento.id,
                            titulo = titulo.trim(),
                            descripcion = descripcion.trim(),
                            categoria = evento.categoria,
                            fecha = fecha.trim(),
                            hora = hora.trim(),
                            ubicacion = ubicacion.trim(),
                            latitud = evento.latitud,
                            longitud = evento.longitud,
                            cupoMaximo = evento.cupoMaximo,
                            imagenUrl = evento.imagenUrl.ifBlank { null },
                            creadorId = evento.creadorId
                        )
                    )
                }
            ) {
                Text("Guardar cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
