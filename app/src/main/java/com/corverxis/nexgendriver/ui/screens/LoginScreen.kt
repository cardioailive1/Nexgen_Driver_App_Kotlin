package com.corverxis.nexgendriver.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corverxis.nexgendriver.R
import com.corverxis.nexgendriver.ui.theme.*
import com.corverxis.nexgendriver.viewmodel.DriverViewModel

@Composable
fun LoginScreen(viewModel: DriverViewModel) {
    var isSignup by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val error by viewModel.loginError.collectAsState()

    Box(Modifier.fillMaxSize().background(NexgenBackground), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher),
                contentDescription = "Corverxis",
                modifier = Modifier.size(72.dp)
            )
            Text(if (isSignup) "Create your account" else "Sign in", color = NexgenText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                if (isSignup)
                    "Set up your driver account. You'll complete your application (license, insurance, vehicle) after signing up."
                else
                    "Sign in to your driver account. This app uses your real GPS position and a live map once you're in.",
                color = NexgenTextDim, fontSize = 13.sp, textAlign = TextAlign.Center
            )

            if (isSignup) {
                LoginField("Your name", name, { name = it })
            }
            LoginField("Email", email, { email = it }, keyboardType = KeyboardType.Email)
            LoginField("Password", password, { password = it }, isPassword = true)

            Button(
                onClick = {
                    if (isSignup) viewModel.register(name.ifBlank { "Driver" }, email, password)
                    else viewModel.login(email, password)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NexgenAccent, contentColor = NexgenAccentText),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(if (isSignup) "Create account" else "Sign in", fontWeight = FontWeight.SemiBold)
            }

            TextButton(onClick = { isSignup = !isSignup }) {
                Text(
                    if (isSignup) "Already have an account? Sign in" else "Need an account? Sign up",
                    color = NexgenAccent, fontSize = 13.sp
                )
            }

            error?.let { Text(it, color = NexgenStop, fontSize = 12.sp, textAlign = TextAlign.Center) }
            Text("Powered by Corverxis Technologies", color = NexgenTextDim, fontSize = 10.sp)
        }
    }
}

@Composable
private fun LoginField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = NexgenTextDim) },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = NexgenSurface,
            unfocusedContainerColor = NexgenSurface,
            focusedTextColor = NexgenText,
            unfocusedTextColor = NexgenText
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
