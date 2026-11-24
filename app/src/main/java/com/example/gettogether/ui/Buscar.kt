package com.example.gettogether.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gettogether.model.Evento
import com.example.gettogether.viewmodel.EventoViewModel

@Composable
fun Buscar(
    eventoViewModel: EventoViewModel,
    onSeleccionarEvento: (Evento) -> Unit
) {
    val eventos by eventoViewModel.eventos.collectAsState()
    val cargando by eventoViewModel.cargando.collectAsState()
    val error by eventoViewModel.error.collectAsState()

    var textoBusqueda by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf("Todas") }
    val categorias = listOf("Todas", "Deportes", "Juegos", "Música", "Social")

    val eventosFiltrados = remember(eventos, textoBusqueda, categoriaSeleccionada) {
        eventos.filter { evento ->
            val coincideTexto = textoBusqueda.isBlank() ||
                    evento.titulo.contains(textoBusqueda, ignoreCase = true) ||
                    evento.ubicacion.contains(textoBusqueda, ignoreCase = true)
            val coincideCategoria = categoriaSeleccionada == "Todas" ||
                    evento.categoria.equals(categoriaSeleccionada, ignoreCase = true)
            coincideTexto && coincideCategoria
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = textoBusqueda,
            onValueChange = { textoBusqueda = it },
            label = { Text("Buscar por título o lugar...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            categorias.forEach { cat ->
                FilterChip(
                    selected = categoriaSeleccionada == cat,
                    onClick = { categoriaSeleccionada = cat },
                    label = { Text(cat) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            cargando -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = error ?: "Error al consultar eventos",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { eventoViewModel.cargarEventos() }) {
                        Text("Reintentar")
                    }
                }
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(eventosFiltrados, key = { it.id }) { evento ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSeleccionarEvento(evento) }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(evento.titulo, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${evento.categoria} • ${evento.ubicacion}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
