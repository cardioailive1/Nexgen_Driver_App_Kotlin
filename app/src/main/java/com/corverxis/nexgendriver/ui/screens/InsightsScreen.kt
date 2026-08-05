package com.corverxis.nexgendriver.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corverxis.nexgendriver.data.DriverInsights
import com.corverxis.nexgendriver.data.RiderComment
import com.corverxis.nexgendriver.ui.theme.*
import com.corverxis.nexgendriver.viewmodel.DriverViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun InsightsScreen(viewModel: DriverViewModel) {
    val insights by viewModel.insights.collectAsState()
    val tierUpAlert by viewModel.tierUpAlert.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshInsights() }

    if (tierUpAlert != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissTierUpAlert() },
            title = { Text("\uD83C\uDF89 New Tier!") },
            text = { Text(tierUpAlert ?: "") },
            confirmButton = { TextButton(onClick = { viewModel.dismissTierUpAlert() }) { Text("Nice!") } }
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val data = insights
        if (data == null) {
            Text("Loading your stats…", color = NexgenTextDim, fontSize = 13.sp)
        } else {
            RewardTierCard(data)

            Column(
                Modifier.fillMaxWidth().background(NexgenSurface, RoundedCornerShape(20.dp)).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(starString(data.avgRating), color = NexgenAccent, fontSize = 28.sp)
                Text(
                    data.avgRating?.let { String.format("%.2f", it) } ?: "—",
                    color = NexgenAccent, fontWeight = FontWeight.Bold, fontSize = 22.sp
                )
                Text("${data.ratingCount} rating${if (data.ratingCount == 1) "" else "s"}", color = NexgenTextDim, fontSize = 11.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBox(
                    value = data.acceptanceRate?.let { "${it.toInt()}%" } ?: "—",
                    label = "Acceptance rate",
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    value = data.cancelRate?.let { "${it.toInt()}%" } ?: "—",
                    label = "Cancellation rate",
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    value = "$" + String.format("%.2f", data.totalTips),
                    label = "Tips (${data.tipCount})",
                    modifier = Modifier.weight(1f),
                    accent = true
                )
            }

            if (data.recentTips.isNotEmpty()) {
                Column {
                    Text("RECENT TIPS", color = NexgenTextDim, fontSize = 10.sp)
                    Spacer(Modifier.height(8.dp))
                    data.recentTips.forEach { tip ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(DateFormat.getDateInstance().format(Date(tip.date.toLong())), color = NexgenText, fontSize = 12.sp)
                            Text("$" + String.format("%.2f", tip.amount), color = NexgenGo, fontSize = 12.sp)
                        }
                    }
                }
            }

            Column {
                Text("RIDER COMMENTS", color = NexgenTextDim, fontSize = 10.sp)
                Spacer(Modifier.height(8.dp))
                if (data.recentComments.isEmpty()) {
                    Text("No written feedback yet.", color = NexgenTextDim, fontSize = 12.sp)
                } else {
                    data.recentComments.forEach { CommentRow(it) }
                }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: RiderComment) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(starString(comment.rating?.toDouble()), color = NexgenAccent, fontSize = 12.sp)
            Text(
                DateFormat.getDateInstance().format(Date(comment.date.toLong())),
                color = NexgenTextDim, fontSize = 10.sp
            )
        }
        comment.comment?.let {
            Text("\"$it\"", color = NexgenText, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StatBox(value: String, label: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Column(
        modifier.background(NexgenSurface, RoundedCornerShape(14.dp)).padding(12.dp)
    ) {
        Text(value, color = if (accent) NexgenAccent else NexgenText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = NexgenTextDim, fontSize = 10.sp)
    }
}

private fun starString(rating: Double?): String {
    if (rating == null) return "—"
    val rounded = Math.round(rating).toInt().coerceIn(0, 5)
    return "★".repeat(rounded) + "☆".repeat(5 - rounded)
}

private fun tierColor(tier: String?): Color = when (tier) {
    "Silver" -> Color(0xFFC0C6CC)
    "Gold" -> Color(0xFFF0B93B)
    "Premium" -> Color(0xFFB47CE5)
    "Diamond" -> NexgenAccent
    else -> NexgenTextDim
}

@Composable
private fun RewardTierCard(data: DriverInsights) {
    val nextThreshold = data.rewardTiers.find { it.name == data.nextRewardTier }?.threshold
    val prevThreshold = data.rewardTiers.find { it.name == data.rewardTier }?.threshold ?: 0
    val progress = if (nextThreshold != null && nextThreshold > prevThreshold) {
        ((data.rewardPoints - prevThreshold).toFloat() / (nextThreshold - prevThreshold).toFloat()).coerceIn(0f, 1f)
    } else 1f

    Column(
        Modifier.fillMaxWidth().background(NexgenSurface, RoundedCornerShape(20.dp)).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("REWARD TIER", color = NexgenTextDim, fontSize = 10.sp)
        Text(
            data.rewardTier ?: "Unranked",
            color = tierColor(data.rewardTier), fontWeight = FontWeight.Bold, fontSize = 24.sp
        )
        Text("${data.rewardPoints} points", color = NexgenTextDim, fontSize = 11.sp)

        Box(
            Modifier.fillMaxWidth().padding(top = 12.dp).height(6.dp)
                .background(NexgenSurface2, RoundedCornerShape(50))
        ) {
            Box(
                Modifier.fillMaxWidth(progress).fillMaxHeight()
                    .background(tierColor(data.nextRewardTier ?: data.rewardTier), RoundedCornerShape(50))
            )
        }

        val subtitle = if (data.nextRewardTier != null && data.pointsToNextTier != null) {
            "${data.pointsToNextTier} points to ${data.nextRewardTier}"
        } else {
            "Top tier reached \u2014 Diamond"
        }
        Text(subtitle, color = NexgenTextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        Text(
            "1 point per trip \u00b7 3 points during bonus hours (9pm\u20132am nightly, plus weekends 11am\u20133pm, ET)",
            color = NexgenTextDim, fontSize = 10.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}
