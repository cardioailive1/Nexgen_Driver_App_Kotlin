package com.corverxis.nexgendriver.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corverxis.nexgendriver.ui.theme.*
import com.corverxis.nexgendriver.viewmodel.DriverViewModel
import com.corverxis.nexgendriver.viewmodel.TripPhase
import java.util.Locale

@Composable
fun TripScreen(viewModel: DriverViewModel) {
    val phase by viewModel.phase.collectAsState()
    val ride by viewModel.currentRide.collectAsState()
    val route by viewModel.route.collectAsState()
    val etaMinutes by viewModel.etaMinutes.collectAsState()
    val meterFare by viewModel.meterFare.collectAsState()
    val coordinate by viewModel.currentCoordinate.collectAsState()

    val phaseLabel = when (phase) {
        TripPhase.TO_PICKUP -> "TO PICKUP"
        TripPhase.READY_START -> "START TRIP"
        TripPhase.IN_TRIP -> "IN TRIP"
        TripPhase.READY_COMPLETE -> "COMPLETE TRIP"
    }
    val actionLabel = when (phase) {
        TripPhase.TO_PICKUP -> "Arrived at pickup"
        TripPhase.READY_START -> "Start trip"
        TripPhase.IN_TRIP, TripPhase.READY_COMPLETE -> "Complete trip"
    }
    val destLabel = if (phase == TripPhase.TO_PICKUP) ride?.pickupLabel ?: "Pickup" else ride?.dropLabel ?: "Drop-off"

    Column(
        Modifier.fillMaxSize().background(NexgenBackground).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("EN ROUTE", color = NexgenText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                phaseLabel, color = NexgenTextDim, fontSize = 11.sp,
                modifier = Modifier.background(NexgenSurface, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        Column(Modifier.background(NexgenSurface, RoundedCornerShape(20.dp))) {
            Row(
                Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Live route", color = NexgenTextDim, fontSize = 11.sp)
                Text("$etaMinutes min", color = NexgenAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            TripMapView(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                driverCoordinate = coordinate,
                pickup = if (phase == TripPhase.TO_PICKUP) ride?.pickup else null,
                drop = if (phase != TripPhase.TO_PICKUP) ride?.drop else null,
                route = route
            )
        }

        Row(
            Modifier.fillMaxWidth().background(NexgenSurface, RoundedCornerShape(20.dp)).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("$" + String.format(Locale.US, "%.2f", meterFare), color = NexgenGo, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text("meter running", color = NexgenTextDim, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(destLabel, color = NexgenText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("destination", color = NexgenTextDim, fontSize = 11.sp)
            }
        }

        Button(
            onClick = { viewModel.advanceTripPhase() },
            colors = ButtonDefaults.buttonColors(containerColor = NexgenAccent, contentColor = NexgenAccentText),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(actionLabel, fontWeight = FontWeight.SemiBold)
        }

        if (phase == TripPhase.TO_PICKUP) {
            var showConfirm by remember { mutableStateOf(false) }
            Button(
                onClick = { showConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = NexgenSurface2, contentColor = NexgenText),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            ) {
                Text("Cancel trip", fontWeight = FontWeight.SemiBold)
            }
            if (showConfirm) {
                AlertDialog(
                    onDismissRequest = { showConfirm = false },
                    title = { Text("Cancel this trip?") },
                    text = { Text("This counts against your cancellation rate.") },
                    confirmButton = {
                        TextButton(onClick = { showConfirm = false; viewModel.cancelTrip() }) { Text("Cancel trip", color = NexgenStop) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirm = false }) { Text("Keep trip") }
                    }
                )
            }
        }
    }
}
