package com.example.gettogether.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.gettogether.viewmodel.UsuarioViewModel

@Composable
fun Registro(
    usuarioViewModel: UsuarioViewModel,
    onNavegarAInicio: () -> Unit,
    onVolverALogin: () -> Unit
) {
    val state by usuarioViewModel.uiState.collectAsState()

    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorLocal by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.operacionExitosa) {
        if (state.operacionExitosa) {
            usuarioViewModel.limpiarEstadoNavegacion()
            onNavegarAInicio()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Crear Cuenta",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre completo") },
            singleLine = true,
            enabled = !state.cargando,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            singleLine = true,
            enabled = !state.cargando,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = telefono,
            onValueChange = { telefono = it },
            label = { Text("Teléfono (opcional)") },
            singleLine = true,
            enabled = !state.cargando,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña (mín. 6 caracteres)") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !state.cargando,
            modifier = Modifier.fillMaxWidth()
        )

        val mensajeError = errorLocal ?: state.error
        if (mensajeError != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = mensajeError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                errorLocal = when {
                    nombre.isBlank() -> "Ingresa tu nombre"
                    !email.contains("@") -> "Ingresa un correo válido"
                    password.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
                    else -> null
                }
                if (errorLocal == null) {
                    usuarioViewModel.registrar(nombre, email, password, telefono)
                }
            },
            enabled = !state.cargando,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (state.cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Registrarme")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = {
                usuarioViewModel.limpiarEstadoNavegacion()
                onVolverALogin()
            },
            enabled = !state.cargando
        ) {
            Text("Ya tengo una cuenta")
        }
    }
}
