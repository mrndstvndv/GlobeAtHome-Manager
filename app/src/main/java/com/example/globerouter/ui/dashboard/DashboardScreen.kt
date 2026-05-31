package com.example.globerouter.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.globerouter.data.models.DashboardData
import com.example.globerouter.theme.Connected
import com.example.globerouter.theme.Data
import com.example.globerouter.theme.Disconnected
import com.example.globerouter.theme.SignalBad
import com.example.globerouter.theme.SignalExcellent
import com.example.globerouter.theme.SignalFair
import com.example.globerouter.theme.SignalGood
import com.example.globerouter.theme.SignalPoor
import com.example.globerouter.theme.Wifi
import com.example.globerouter.ui.main.DashboardViewModel
import com.example.globerouter.ui.main.DashboardUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
  onLogout: () -> Unit,
  onBandLock: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: DashboardViewModel = viewModel { DashboardViewModel() },
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  // Start polling on first composition
  androidx.compose.runtime.LaunchedEffect(Unit) {
    viewModel.start()
  }

  when (val s = state) {
    is DashboardUiState.Loading -> {
      Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator()
          Spacer(Modifier.height(16.dp))
          Text("Connecting to router...", style = MaterialTheme.typography.titleMedium)
        }
      }
    }
    is DashboardUiState.Success -> {
      DashboardContent(
        data = s.data,
        onRefresh = { viewModel.refresh() },
        onReboot = { viewModel.reboot() },
        onWanConnect = { viewModel.wanConnect() },
        onWanDisconnect = { viewModel.wanDisconnect() },
        onBandLock = onBandLock,
        onLogout = onLogout,
        modifier = modifier,
      )
    }
    is DashboardUiState.Error -> {
      Column(modifier = modifier.padding(16.dp)) {
        Text("Error: ${s.message}", color = SignalBad)
        Spacer(Modifier.height(8.dp))
        Button(onClick = { viewModel.refresh() }) {
          Text("Retry")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onLogout) {
          Text("Back to Login")
        }
      }
    }
    is DashboardUiState.Rebooting -> {
      Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator()
          Spacer(Modifier.height(16.dp))
          Text("Rebooting router...", style = MaterialTheme.typography.titleMedium)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DashboardContent(
  data: DashboardData,
  onRefresh: () -> Unit,
  onReboot: () -> Unit,
  onWanConnect: () -> Unit,
  onWanDisconnect: () -> Unit,
  onBandLock: () -> Unit,
  onLogout: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Globe Router") },
        actions = {
          IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
          }
          IconButton(onClick = onLogout) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
      )
    },
    modifier = modifier,
  ) { padding ->
    Column(
      modifier = Modifier
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      StatusCard(data)
      SignalCard(data)
      UsageCard(data)
      QuickActionsCard(data.connected, onReboot, onWanConnect, onWanDisconnect, onBandLock)
      BottomInfo(data)
    }
  }
}

@Composable
private fun StatusCard(data: DashboardData) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Row(
      modifier = Modifier.padding(20.dp).fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(16.dp)
          .clip(CircleShape)
          .background(if (data.connected) Connected else Disconnected),
      )
      Spacer(Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(data.networkType, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          Spacer(Modifier.width(8.dp))
          Text(
            if (data.connected) "Connected" else "Disconnected",
            color = if (data.connected) Connected else Disconnected,
            style = MaterialTheme.typography.labelMedium,
          )
        }
        Spacer(Modifier.height(4.dp))
        Text("IP: ${data.wanIp}", style = MaterialTheme.typography.bodySmall)
        Text("Up: ${data.sessionDuration}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@Composable
private fun SignalCard(data: DashboardData) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.NetworkCell, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Signal Quality", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
      }
      Spacer(Modifier.height(16.dp))

      SignalBar("RSSI", "${data.rssi} dBm", data.rssi.toFloat(), -50f, -120f, invert = true)
      Spacer(Modifier.height(12.dp))
      SignalBar("RSRQ", "${data.rsrq} dB", data.rsrq.toFloat(), -5f, -20f, invert = true)
      Spacer(Modifier.height(12.dp))
      if (data.sinr != null) {
        SignalBar("SINR", "%.1f dB".format(data.sinr), data.sinr, 30f, 0f, invert = false)
      } else {
        Text("SINR: —", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@Composable
private fun SignalBar(label: String, value: String, current: Float, good: Float, bad: Float, invert: Boolean) {
  val fraction = ((current - bad) / (good - bad)).coerceIn(0f, 1f)
  val normalized = if (invert) 1f - fraction else fraction
  val color = signalColor(normalized)

  Column {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(label, style = MaterialTheme.typography.bodySmall)
      Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
    Spacer(Modifier.height(4.dp))
    LinearProgressIndicator(
      progress = { normalized },
      modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
      color = color,
      trackColor = color.copy(alpha = 0.15f),
      strokeCap = StrokeCap.Round,
    )
  }
}

private fun signalColor(fraction: Float): Color = when {
  fraction >= 0.7f -> SignalExcellent
  fraction >= 0.5f -> SignalGood
  fraction >= 0.35f -> SignalFair
  fraction >= 0.2f -> SignalPoor
  else -> SignalBad
}

@Composable
private fun UsageCard(data: DashboardData) {
  val monthlyTotal = data.monthlyTxBytes + data.monthlyRxBytes
  val monthlyGigs = monthlyTotal / 1_000_000_000f
  val txMbps = data.realtimeTxThrpt / 1_000_000f
  val rxMbps = data.realtimeRxThrpt / 1_000_000f

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Text("Data Usage", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
      Spacer(Modifier.height(16.dp))

      Text("Monthly", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(Modifier.height(8.dp))
      val cap = 10_000_000_000f
      val frac = (monthlyTotal / cap).coerceIn(0f, 1f)
      LinearProgressIndicator(
        progress = { frac },
        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
        color = Data,
        trackColor = Data.copy(alpha = 0.15f),
        strokeCap = StrokeCap.Round,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        "%.2f GB used".format(monthlyGigs),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
      )
      Text(
        "Session time: ${data.monthlyTime}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(Modifier.height(16.dp))

      Text("Real-time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(Modifier.height(8.dp))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        SpeedBox("↓ Download", "%.1f".format(rxMbps), Wifi)
        SpeedBox("↑ Upload", "%.1f".format(txMbps), Data)
      }
    }
  }
}

@Composable
private fun SpeedBox(label: String, value: String, color: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      modifier = Modifier
        .size(72.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(color.copy(alpha = 0.12f)),
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        Text("Mbps", style = MaterialTheme.typography.labelSmall, color = color)
      }
    }
    Spacer(Modifier.height(4.dp))
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun QuickActionsCard(
  connected: Boolean,
  onReboot: () -> Unit,
  onWanConnect: () -> Unit,
  onWanDisconnect: () -> Unit,
  onBandLock: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Text("Quick Actions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
      Spacer(Modifier.height(12.dp))
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (connected) {
          OutlinedButton(onClick = onWanDisconnect) {
            Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Disconnect")
          }
        } else {
          Button(onClick = onWanConnect) {
            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Connect")
          }
        }
        OutlinedButton(onClick = onBandLock) {
          Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Band Lock")
        }
        OutlinedButton(onClick = onReboot) {
          Text("Reboot")
        }
      }
    }
  }
}

@Composable
private fun BottomInfo(data: DashboardData) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        InfoChip(Icons.Default.Wifi, "2.4G: ${data.staCount}")
        InfoChip(Icons.Default.Wifi, "5G: ${data.mStaCount}")
      }
      Spacer(Modifier.height(8.dp))
      Text("SSID: ${data.ssid24}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text("Firmware: ${data.firmware}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.width(4.dp))
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}
