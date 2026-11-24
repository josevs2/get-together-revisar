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
fun Login(
    usuarioViewModel: UsuarioViewModel,
    onNavegarAInicio: () -> Unit,
    onNavegarARegistro: () -> Unit
) {
    val state by usuarioViewModel.uiState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Navega a Inicio apenas Xano confirme el login y limpia el flag
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
            text = "Iniciar Sesión",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            singleLine = true,
            enabled = !state.cargando,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !state.cargando,
            modifier = Modifier.fillMaxWidth()
        )

        // Mensaje de error de Xano (credenciales inválidas o error de red)
        state.error?.let { mensajeError ->
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
            onClick = { usuarioViewModel.login(email, password) },
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
                Text("Ingresar")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = {
                usuarioViewModel.limpiarEstadoNavegacion()
                onNavegarARegistro()
            },
            enabled = !state.cargando
        ) {
            Text("¿No tienes cuenta? Regístrate")
        }
    }
}
