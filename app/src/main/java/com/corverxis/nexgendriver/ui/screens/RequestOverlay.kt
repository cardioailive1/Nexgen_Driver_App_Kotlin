package com.corverxis.nexgendriver.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corverxis.nexgendriver.data.RideDTO
import com.corverxis.nexgendriver.ui.theme.*
import com.corverxis.nexgendriver.viewmodel.DriverViewModel
import java.util.Locale

@Composable
fun RequestOverlay(viewModel: DriverViewModel, ride: RideDTO) {
    val secondsRemaining by viewModel.requestSecondsRemaining.collectAsState()
    val fareRates by viewModel.fareRates.collectAsState()
    val distKm = ride.estDistanceKm ?: 0.0
    val estMiles = distKm * 0.621371
    val estMinutes = distKm * 2.2 // rough heuristic — no real route yet at this point
    // Same rates that will actually be charged — see FareRates / /api/fare/rates.
    // This used to be a separately made-up formula that could drift from real billing.
    val estFare = fareRates?.let { it.base + distKm * it.perKmEffective + estMinutes * it.perMin }
        ?: (2.5 + estMiles * 1.5) // last-resort fallback if rates couldn't be fetched

    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.75f))) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .background(NexgenBackground, RoundedCornerShape(20.dp))
                .border(1.dp, NexgenAccent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LinearProgressIndicator(
                progress = { secondsRemaining / 12f },
                color = NexgenAccent,
                modifier = Modifier.fillMaxWidth()
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(ride.rider ?: "Rider", color = NexgenText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "Requesting ride \u00b7 " + if (ride.dispatchMode == "matched") "matched to you" else "sent directly",
                        color = NexgenTextDim, fontSize = 11.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("$" + String.format(Locale.US, "%.2f", estFare), color = NexgenGo, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("est. fare", color = NexgenTextDim, fontSize = 11.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(NexgenGo))
                Spacer(Modifier.width(10.dp))
                Text(ride.pickupLabel ?: "Pickup", color = NexgenText, fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(NexgenStop))
                Spacer(Modifier.width(10.dp))
                Text(ride.dropLabel ?: "Drop-off", color = NexgenText, fontSize = 12.sp)
            }

            Text(
                String.format(Locale.US, "%.1f mi trip · %ds to respond", estMiles, secondsRemaining),
                color = NexgenTextDim, fontSize = 11.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { viewModel.declineRequest() },
                    colors = ButtonDefaults.buttonColors(containerColor = NexgenSurface2, contentColor = NexgenTextDim),
                    modifier = Modifier.weight(1f)
                ) { Text("Decline", fontWeight = FontWeight.SemiBold) }

                Button(
                    onClick = { viewModel.acceptRequest() },
                    colors = ButtonDefaults.buttonColors(containerColor = NexgenGo, contentColor = NexgenGoText),
                    modifier = Modifier.weight(1f)
                ) { Text("Accept", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}
