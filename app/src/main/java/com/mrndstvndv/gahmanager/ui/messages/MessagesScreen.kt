package com.mrndstvndv.gahmanager.ui.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mrndstvndv.gahmanager.data.models.SmsMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: MessagesViewModel = viewModel { MessagesViewModel() },
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  var showComposeDialog by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    viewModel.load()
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Messages") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(
            onClick = { viewModel.load() },
            enabled = !state.isLoading && !state.isRefreshing
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
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showComposeDialog = true },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      ) {
        Icon(Icons.Default.Add, contentDescription = "Compose Message")
      }
    },
    modifier = modifier,
  ) { padding ->
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
      Column(modifier = Modifier.fillMaxSize()) {
        if (state.isRefreshing || state.isSending) {
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        when {
          state.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator()
            }
          }
          state.errorMessage != null && state.messages.isEmpty() -> {
            ErrorView(
              message = state.errorMessage ?: "An error occurred",
              onRetry = { viewModel.load() }
            )
          }
          state.messages.isEmpty() -> {
            EmptyView()
          }
          else -> {
            LazyColumn(
              modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              item { Spacer(modifier = Modifier.height(8.dp)) }
              items(state.messages, key = { it.id }) { message ->
                MessageItem(
                  message = message,
                  onDelete = { viewModel.deleteMessage(message.id) }
                )
              }
              item { Spacer(modifier = Modifier.height(80.dp)) } // margin for FAB
            }
          }
        }
      }

      if (showComposeDialog) {
        ComposeMessageDialog(
          isSending = state.isSending,
          errorMessage = state.errorMessage,
          onDismiss = {
            showComposeDialog = false
            viewModel.clearSendSuccess()
          },
          onSend = { number, content ->
            viewModel.sendMessage(number, content) { success ->
              if (success) {
                showComposeDialog = false
              }
            }
          }
        )
      }
    }
  }
}

@Composable
fun MessageItem(
  message: SmsMessage,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = if (message.isUnread) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
      } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
      }
    )
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (message.isUnread) {
            Box(
              modifier = Modifier
                .padding(end = 8.dp)
                .size(8.dp)
                .align(Alignment.CenterVertically)
            )
          }
          Text(
            text = message.number,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (message.isUnread) FontWeight.Bold else FontWeight.SemiBold
          )
        }
        Text(
          text = message.displayTime,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
      ) {
        Text(
          text = message.decodedContent,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f)
        )
        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(24.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete Message",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
  }
}

@Composable
fun ErrorView(
  message: String,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(24.dp)
    ) {
      Text(
        text = "Connection Error",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.error,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(16.dp))
      Button(onClick = onRetry) {
        Text("Retry")
      }
    }
  }
}

@Composable
fun EmptyView(
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(24.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Email,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "No Messages",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Inbox is empty or requires connection",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
fun ComposeMessageDialog(
  isSending: Boolean,
  errorMessage: String?,
  onDismiss: () -> Unit,
  onSend: (String, String) -> Unit,
) {
  var number by remember { mutableStateOf("") }
  var content by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = { if (!isSending) onDismiss() },
    title = { Text("Compose Message") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = number,
          onValueChange = { number = it },
          label = { Text("Recipient Number") },
          modifier = Modifier.fillMaxWidth(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          enabled = !isSending
        )
        OutlinedTextField(
          value = content,
          onValueChange = { content = it },
          label = { Text("Message") },
          modifier = Modifier.fillMaxWidth().height(120.dp),
          maxLines = 5,
          enabled = !isSending
        )
        if (errorMessage != null) {
          Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSend(number, content) },
        enabled = !isSending && number.isNotBlank() && content.isNotBlank()
      ) {
        if (isSending) {
          CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary
          )
        } else {
          Text("Send")
        }
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        enabled = !isSending
      ) {
        Text("Cancel")
      }
    }
  )
}
