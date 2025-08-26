package com.jcmateus.kalisfit.ui.screens.auth_profile

import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    // Es mejor instanciar ViewModels usando los constructores de Hilt o androidx.lifecycle.viewmodel.compose.viewModel()
    // val viewModel: AuthViewModel = viewModel() // Si estás usando Hilt o la librería de ViewModel de Compose
    val viewModel = remember { AuthViewModel() } // Tu forma actual

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    val showFields = remember { mutableStateOf(true) }

    // Estado para manejar el error del campo email
    var emailError by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        loading = true // Indicar carga para el flujo de Google Sign-In también
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.result // Puede lanzar una excepción si la tarea falló
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { authResult ->
                    if (authResult.isSuccessful) {
                        viewModel.saveUserIfNew(
                            nombre = account.displayName ?: "",
                            email = account.email ?: "" // El email de Google se asume válido
                        ) {
                            loading = false
                            showSuccessDialog = true
                        }
                    } else {
                        loading = false
                        Toast.makeText(context, "Error de inicio de sesión con Google: ${authResult.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        } catch (e: Exception) {
            loading = false
            Toast.makeText(context, "Google Sign In cancelado o falló: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { /* No permitir cerrar por clic fuera */ },
            title = { Text("¡Registro exitoso!") },
            text = { Text("Tu cuenta ha sido creada correctamente. Serás redirigido...") },
            confirmButton = {
                TextButton(onClick = {
                    showSuccessDialog = false // Ocultar diálogo
                    onRegisterSuccess()
                }) {
                    Text("Continuar")
                }
            }
        )
        // La navegación ya se maneja en el LaunchedEffect o podría ser solo con el botón
        LaunchedEffect(Unit) {
            delay(2000) // Un poco más de tiempo para leer el mensaje
            if (showSuccessDialog) { // Solo navegar si el diálogo todavía está visible (el usuario no hizo clic en continuar)
                onRegisterSuccess()
            }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visible = showFields.value, enter = fadeIn() + expandVertically()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(180.dp)
                    )

                    Text("Crear cuenta", style = MaterialTheme.typography.displayLarge)

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre completo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = null // Limpiar error al escribir
                        },
                        label = { Text("Correo electrónico") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
                        isError = emailError != null, // Indicar si hay error
                        supportingText = { // Mostrar mensaje de error debajo del campo
                            if (emailError != null) {
                                Text(emailError!!, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val icon = if (passwordVisible)
                                Icons.Filled.Visibility
                            else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = icon, contentDescription = "Mostrar contraseña")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            // --- VALIDACIÓN DEL EMAIL AQUÍ ---
                            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                emailError = "Formato de correo inválido"
                                return@Button // No continuar si el email no es válido
                            }
                            // Opcional: Validar que la contraseña no esté vacía, etc.
                            if (password.length < 6) { // Firebase requiere mínimo 6 caracteres
                                Toast.makeText(context, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (name.isBlank()){
                                Toast.makeText(context, "El nombre no puede estar vacío", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            loading = true
                            emailError = null // Limpiar error si la validación pasó
                            viewModel.register(email, password, name, "", listOf()) { success, message ->
                                loading = false
                                if (success) {
                                    showSuccessDialog = true
                                } else {
                                    Toast.makeText(context, "Error: $message", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (loading && !showSuccessDialog) { // Mostrar indicador de carga solo para el botón de email/pass
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Registrarse")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            // No es necesario validar el email aquí ya que Google lo proporciona
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(context.getString(R.string.default_web_client_id)) // Asegúrate que este string existe en tu R.string
                                .requestEmail()
                                .build()
                            val client = GoogleSignIn.getClient(context, gso)
                            launcher.launch(client.signInIntent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading // Deshabilitar si cualquier operación de carga está en curso
                    ) {
                        // Podrías mostrar un indicador de carga aquí también si 'loading' es true
                        // debido a un intento de registro con Google.
                        Icon(
                            painter = painterResource(id = R.drawable.ic_google), // Asegúrate que este drawable existe
                            contentDescription = "Google",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Registrarse con Google")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = onNavigateToLogin, enabled = !loading) {
                        Text("¿Ya tienes cuenta? Inicia sesión")
                    }
                }
            }
        }
        // Indicador de carga general centrado (opcional si ya tienes en botones)
        if (loading && !showSuccessDialog) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // CircularProgressIndicator() // Podrías tener un overlay de carga más general
            }
        }
    }
}
