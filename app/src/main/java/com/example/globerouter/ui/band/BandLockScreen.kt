package com.example.globerouter.ui.band

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandLockScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: BandLockViewModel = viewModel { BandLockViewModel() },
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(Unit) {
    viewModel.load()
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Band Lock") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(
            onClick = { viewModel.load() },
            enabled = !state.isApplying && !state.isRefreshing,
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
    when {
      state.snapshot == null && state.errorMessage == null -> {
        LoadingState(
          modifier = Modifier
            .padding(padding)
            .fillMaxSize(),
        )
      }

      state.snapshot == null -> {
        ErrorState(
          message = state.errorMessage ?: "Failed to load band lock",
          onRetry = { viewModel.load() },
          modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(16.dp),
        )
      }

      else -> {
        BandLockContent(
          state = state,
          onToggleBand = viewModel::toggleBand,
          onSelectAll = viewModel::selectAll,
          onReset = viewModel::resetDraft,
          onApply = viewModel::apply,
          onDisable = viewModel::disable,
          modifier = Modifier
            .padding(padding)
            .fillMaxSize(),
        )
      }
    }
  }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      CircularProgressIndicator()
      Spacer(Modifier.height(16.dp))
      Text("Loading band lock…", style = MaterialTheme.typography.titleMedium)
    }
  }
}

@Composable
private fun ErrorState(
  message: String,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(message, color = MaterialTheme.colorScheme.error)
    Spacer(Modifier.height(12.dp))
    Button(onClick = onRetry) {
      Text("Retry")
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BandLockContent(
  state: BandLockUiState,
  onToggleBand: (Int) -> Unit,
  onSelectAll: () -> Unit,
  onReset: () -> Unit,
  onApply: () -> Unit,
  onDisable: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val snapshot = state.snapshot ?: return

  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    if (state.isRefreshing || state.isApplying) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = if (snapshot.enabled) Icons.Default.Lock else Icons.Default.LockOpen,
            contentDescription = null,
          )
          Spacer(Modifier.width(8.dp))
          Text(
            text = if (snapshot.enabled) "Lock enabled" else "Lock disabled",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
          )
        }
        Spacer(Modifier.height(12.dp))
        Text(
          text = "Configured bands: ${formatBands(snapshot.selectedBands)}",
          style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text = "Supported bands: ${formatBands(snapshot.supportedBands)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.hasDraftChanges) {
          Spacer(Modifier.height(8.dp))
          Text(
            text = "Unsaved draft: ${formatBands(state.draftBands)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }

    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(
          text = "Select LTE bands",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
          text = "Apply lock enables the selected LTE bands. Disable turns LTE band locking off.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          state.supportedBands.forEach { band ->
            FilterChip(
              selected = band in state.draftBands,
              onClick = { onToggleBand(band) },
              label = { Text("B$band") },
              enabled = !state.isApplying && !state.isRefreshing,
            )
          }
        }

        Spacer(Modifier.height(16.dp))
        Text(
          text = if (state.draftBands.isEmpty()) {
            "Select at least one LTE band before applying a lock."
          } else {
            "Draft selection: ${formatBands(state.draftBands)}"
          },
          style = MaterialTheme.typography.bodySmall,
          color = if (state.draftBands.isEmpty()) {
            MaterialTheme.colorScheme.error
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
        )

        Spacer(Modifier.height(16.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(onClick = onApply, enabled = state.canApply) {
            Text(if (state.enabled) "Update lock" else "Apply lock")
          }
          OutlinedButton(onClick = onDisable, enabled = state.canDisable) {
            Text("Disable")
          }
          OutlinedButton(
            onClick = onSelectAll,
            enabled = !state.isApplying && !state.isRefreshing && state.supportedBands.isNotEmpty(),
          ) {
            Text("Select all")
          }
          OutlinedButton(
            onClick = onReset,
            enabled = !state.isApplying && !state.isRefreshing && state.hasDraftChanges,
          ) {
            Text("Reset")
          }
        }
      }
    }

    if (state.errorMessage != null) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
      ) {
        Text(
          text = state.errorMessage,
          modifier = Modifier.padding(16.dp),
          color = MaterialTheme.colorScheme.onErrorContainer,
          style = MaterialTheme.typography.bodySmall,
        )
      }
    }
  }
}

private fun formatBands(bands: Collection<Int>): String {
  if (bands.isEmpty()) return "None"
  return bands.sorted().joinToString(separator = ", ") { band -> "B$band" }
}
