package com.corverxis.nexgendriver.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corverxis.nexgendriver.data.TripHistoryItem
import com.corverxis.nexgendriver.ui.theme.*
import com.corverxis.nexgendriver.viewmodel.DriverViewModel
import java.util.Locale

private enum class Tab { HOME, INSIGHTS, EARNINGS, ACCOUNT }

@Composable
fun DashboardScreen(viewModel: DriverViewModel) {
    var tab by remember { mutableStateOf(Tab.HOME) }
    val incomingRequest by viewModel.incomingRequest.collectAsState()

    Box(Modifier.fillMaxSize().background(NexgenBackground)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("NEXGEN.", color = NexgenText, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                Row(
                    Modifier.background(NexgenSurface, RoundedCornerShape(50)).padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(26.dp).clip(CircleShape).background(NexgenAccent.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) { Text(viewModel.driverName.take(1).uppercase(), color = NexgenText, fontSize = 12.sp) }
                    Spacer(Modifier.width(6.dp))
                    Text(viewModel.driverName, color = NexgenTextDim, fontSize = 12.sp)
                }
            }

            Box(Modifier.weight(1f)) {
                when (tab) {
                    Tab.HOME -> HomeTab(viewModel)
                    Tab.INSIGHTS -> InsightsScreen(viewModel)
                    Tab.EARNINGS -> EarningsTab(viewModel)
                    Tab.ACCOUNT -> AccountTab(viewModel)
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavButton("Home", tab == Tab.HOME) { tab = Tab.HOME }
                NavButton("Insights", tab == Tab.INSIGHTS) { tab = Tab.INSIGHTS }
                NavButton("Earnings", tab == Tab.EARNINGS) { tab = Tab.EARNINGS }
                NavButton("Account", tab == Tab.ACCOUNT) { tab = Tab.ACCOUNT }
            }
        }

        incomingRequest?.let { ride ->
            RequestOverlay(viewModel = viewModel, ride = ride)
        }
    }
}

@Composable
private fun NavButton(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(label, color = if (selected) NexgenAccent else NexgenTextDim, fontSize = 12.sp)
    }
}

@Composable
private fun HomeTab(viewModel: DriverViewModel) {
    val online by viewModel.online.collectAsState()
    val coordinate by viewModel.currentCoordinate.collectAsState()
    val earnings by viewModel.earnings.collectAsState()
    val trips by viewModel.trips.collectAsState()
    val onlineSeconds by viewModel.onlineSeconds.collectAsState()
    val history by viewModel.history.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            Modifier.fillMaxWidth().background(NexgenSurface, RoundedCornerShape(20.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(if (online) "YOU'RE ONLINE" else "YOU'RE OFFLINE", color = NexgenTextDim, fontSize = 11.sp)
            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .size(170.dp)
                    .clip(CircleShape)
                    .background(if (online) NexgenGo else NexgenSurface2)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        viewModel.toggleOnline()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (online) "ONLINE" else "GO",
                        color = if (online) NexgenGoText else NexgenText,
                        fontWeight = FontWeight.Bold, fontSize = 20.sp
                    )
                    Text(
                        if (online) "tap to stop" else "tap to start",
                        color = if (online) NexgenGoText else NexgenTextDim, fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            if (coordinate == null) {
                Text("Waiting for GPS fix…", color = NexgenStop, fontSize = 11.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox("$" + String.format(Locale.US, "%.2f", earnings), "Today's earnings", accent = true, modifier = Modifier.weight(1f))
            StatBox("$trips", "Trips completed", modifier = Modifier.weight(1f))
            val m = onlineSeconds / 60
            val s = onlineSeconds % 60
            StatBox(String.format(Locale.US, "%d:%02d", m, s), "Online time", modifier = Modifier.weight(1f))
        }

        Column {
            Text("RECENT TRIPS", color = NexgenTextDim, fontSize = 10.sp)
            Spacer(Modifier.height(8.dp))
            if (history.isEmpty()) {
                Text("No trips yet — go online to start receiving requests.", color = NexgenTextDim, fontSize = 12.sp)
            } else {
                history.take(6).forEach { HistoryRow(it) }
            }
        }
    }
}

@Composable
private fun EarningsTab(viewModel: DriverViewModel) {
    val earnings by viewModel.earnings.collectAsState()
    val history by viewModel.history.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("EARNINGS", color = NexgenText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        StatBox("$" + String.format(Locale.US, "%.2f", earnings), "Total earned this session", accent = true, modifier = Modifier.fillMaxWidth())
        Text("TRIP HISTORY", color = NexgenTextDim, fontSize = 10.sp)
        if (history.isEmpty()) {
            Text("No trips yet this session.", color = NexgenTextDim, fontSize = 12.sp)
        } else {
            history.forEach { HistoryRow(it) }
        }
    }
}

@Composable
private fun HistoryRow(item: TripHistoryItem) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("${item.pickupLabel ?: ""} → ${item.dropLabel ?: ""}", color = NexgenText, fontSize = 12.sp)
        Text("$" + String.format(Locale.US, "%.2f", item.driverPayout ?: item.fare), color = NexgenGo, fontSize = 12.sp)
    }
}

@Composable
private fun StatBox(value: String, label: String, accent: Boolean = false, modifier: Modifier = Modifier) {
    Column(
        modifier.background(NexgenSurface, RoundedCornerShape(14.dp)).padding(12.dp)
    ) {
        Text(value, color = if (accent) NexgenAccent else NexgenText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = NexgenTextDim, fontSize = 10.sp)
    }
}
