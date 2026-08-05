package com.corverxis.nexgendriver.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corverxis.nexgendriver.ui.theme.*
import com.corverxis.nexgendriver.viewmodel.DriverViewModel
import kotlinx.coroutines.launch

@Composable
fun AccountTab(viewModel: DriverViewModel) {
    val payoutsEnabled by viewModel.payoutsEnabled.collectAsState()
    val payoutsConnected by viewModel.payoutsConnected.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isOnboarding by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refreshPayoutStatus() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("ACCOUNT", color = NexgenText, fontWeight = FontWeight.Bold, fontSize = 18.sp)

        Row(
            Modifier.fillMaxWidth().background(NexgenSurface, RoundedCornerShape(14.dp)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(NexgenAccent.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) { Text(viewModel.driverName.take(1).uppercase(), color = NexgenText, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(10.dp))
            Text(viewModel.driverName, color = NexgenText)
        }

        Column(
            Modifier.fillMaxWidth().background(NexgenSurface, RoundedCornerShape(14.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("PAYOUTS", color = NexgenTextDim, fontSize = 10.sp)

            if (payoutsEnabled) {
                Text("Payouts enabled ✓", color = NexgenGo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    "Fares you earn are transferred to your bank account automatically.",
                    color = NexgenTextDim, fontSize = 11.sp
                )
            } else {
                Text(
                    if (payoutsConnected) "You've started onboarding but haven't finished — complete it to get paid."
                    else "Set up payouts to get paid for the trips you complete.",
                    color = NexgenTextDim, fontSize = 12.sp
                )
                Button(
                    onClick = {
                        isOnboarding = true
                        scope.launch {
                            val url = viewModel.startPayoutOnboarding()
                            if (url != null) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                            isOnboarding = false
                        }
                    },
                    enabled = !isOnboarding,
                    colors = ButtonDefaults.buttonColors(containerColor = NexgenAccent, contentColor = NexgenAccentText),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (payoutsConnected) "Finish payout setup" else "Set up payouts", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Text(
            "Vehicle info and support live here in a production build.",
            color = NexgenTextDim, fontSize = 11.sp
        )

        ChangePasswordCard(viewModel)

        Button(
            onClick = { viewModel.logOut() },
            colors = ButtonDefaults.buttonColors(containerColor = NexgenSurface2, contentColor = NexgenText),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log out", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ChangePasswordCard(viewModel: DriverViewModel) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxWidth().background(NexgenSurface, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("CHANGE PASSWORD", color = NexgenTextDim, fontSize = 10.sp)
        OutlinedTextField(
            value = currentPassword, onValueChange = { currentPassword = it },
            placeholder = { Text("Current password", color = NexgenTextDim) },
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = NexgenBackground, unfocusedContainerColor = NexgenBackground, focusedTextColor = NexgenText, unfocusedTextColor = NexgenText),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = newPassword, onValueChange = { newPassword = it },
            placeholder = { Text("New password (min 8 characters)", color = NexgenTextDim) },
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = NexgenBackground, unfocusedContainerColor = NexgenBackground, focusedTextColor = NexgenText, unfocusedTextColor = NexgenText),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                isSaving = true
                viewModel.changePassword(currentPassword, newPassword) { error ->
                    message = error ?: "Password updated."
                    isSaving = false
                }
            },
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = NexgenAccent, contentColor = NexgenAccentText),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSaving) "Updating…" else "Update password", fontWeight = FontWeight.SemiBold)
        }
        message?.let { Text(it, color = NexgenTextDim, fontSize = 11.sp) }
    }
}
