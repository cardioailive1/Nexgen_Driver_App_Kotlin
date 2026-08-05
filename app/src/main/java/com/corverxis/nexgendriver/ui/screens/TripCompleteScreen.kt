package com.corverxis.nexgendriver.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corverxis.nexgendriver.ui.theme.*
import com.corverxis.nexgendriver.viewmodel.DriverViewModel
import java.util.Locale

@Composable
fun TripCompleteScreen(viewModel: DriverViewModel) {
    val result by viewModel.lastResult.collectAsState()

    val (note, noteColor) = when (result?.paymentStatus) {
        "succeeded" -> "Fare collected from rider and transferred to you." to NexgenGo
        "skipped" -> "Payment not processed — set up payouts, or the rider has no card on file." to NexgenTextDim
        else -> "Payment failed — the rider's card was declined or needs authentication." to NexgenStop
    }

    Column(
        Modifier.fillMaxSize().background(NexgenBackground).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("TRIP COMPLETE", color = NexgenText, fontWeight = FontWeight.Bold, fontSize = 16.sp)

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$" + String.format(Locale.US, "%.2f", result?.driverPayout ?: 0.0),
                color = NexgenGo, fontWeight = FontWeight.Bold, fontSize = 32.sp
            )
            Text("added to today's earnings", color = NexgenTextDim, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Text(note, color = noteColor, fontSize = 11.sp, textAlign = TextAlign.Center)
            result?.tripPoints?.let { points ->
                Spacer(Modifier.height(2.dp))
                Text(
                    if (result.isBonusTrip == true) "+$points points \u2014 bonus hours!" else "+$points point",
                    color = NexgenAccent, fontSize = 11.sp
                )
            }
        }

        Column(Modifier.background(NexgenSurface, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp)) {
            FareRow("Base fare", result?.base)
            FareRow("Distance", result?.distFare)
            result?.gasPricePerGallon?.let { gasPrice ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Gas price used", color = NexgenTextDim, fontSize = 10.sp)
                    Text("$" + String.format(Locale.US, "%.2f", gasPrice) + "/gal", color = NexgenTextDim, fontSize = 10.sp)
                }
            }
            FareRow("Time", result?.timeFare)
            FareRow("Rider paid", result?.total, bold = true)
            FareRow("Your payout (60%)", result?.driverPayout, valueColor = NexgenGo)
            FareRow("Platform fee (20%)", result?.platformFee)
            FareRow("Fees & insurance (20%)", result?.insuranceFee)
        }

        Button(
            onClick = { viewModel.continueOnline() },
            colors = ButtonDefaults.buttonColors(containerColor = NexgenAccent, contentColor = NexgenAccentText),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Back online", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FareRow(label: String, value: Double?, bold: Boolean = false, valueColor: androidx.compose.ui.graphics.Color = NexgenText) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = if (bold) NexgenText else NexgenTextDim, fontSize = 12.sp)
        Text(
            "$" + String.format(Locale.US, "%.2f", value ?: 0.0),
            color = if (bold) NexgenText else valueColor,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp
        )
    }
}
