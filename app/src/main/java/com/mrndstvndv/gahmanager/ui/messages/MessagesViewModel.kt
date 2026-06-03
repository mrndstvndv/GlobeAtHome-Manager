package com.mrndstvndv.gahmanager.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrndstvndv.gahmanager.data.RouterApi
import com.mrndstvndv.gahmanager.data.models.Credentials
import com.mrndstvndv.gahmanager.data.models.SmsMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MessagesUiState(
  val isLoading: Boolean = false,
  val isRefreshing: Boolean = false,
  val isSending: Boolean = false,
  val messages: List<SmsMessage> = emptyList(),
  val errorMessage: String? = null,
  val isSendSuccess: Boolean = false,
)

class MessagesViewModel : ViewModel() {
  private val api = RouterApi()

  private val _uiState = MutableStateFlow(MessagesUiState())
  val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

  fun load() {
    val state = _uiState.value
    if (state.isLoading || state.isRefreshing) return

    viewModelScope.launch {
      val hasMessages = _uiState.value.messages.isNotEmpty()
      _uiState.update {
        it.copy(
          isLoading = !hasMessages,
          isRefreshing = hasMessages,
          errorMessage = null,
        )
      }

      try {
        val messages = api.getMessages(Credentials.username, Credentials.password)
        _uiState.update {
          it.copy(
            isLoading = false,
            isRefreshing = false,
            messages = messages,
          )
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
            isLoading = false,
            isRefreshing = false,
            errorMessage = e.message ?: "Failed to load messages",
          )
        }
      }
    }
  }

  fun deleteMessage(messageId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
      try {
        api.deleteMessages(Credentials.username, Credentials.password, listOf(messageId))
        val messages = api.getMessages(Credentials.username, Credentials.password)
        _uiState.update {
          it.copy(
            isRefreshing = false,
            messages = messages,
          )
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
            isRefreshing = false,
            errorMessage = e.message ?: "Failed to delete message",
          )
        }
      }
    }
  }

  fun sendMessage(number: String, content: String, onComplete: (Boolean) -> Unit) {
    if (number.isBlank() || content.isBlank()) return
    val state = _uiState.value
    if (state.isSending) return

    viewModelScope.launch {
      _uiState.update { it.copy(isSending = true, errorMessage = null, isSendSuccess = false) }
      try {
        val success = api.sendSms(Credentials.username, Credentials.password, number.trim(), content)
        if (success) {
          _uiState.update {
            it.copy(
              isSending = false,
              isSendSuccess = true,
            )
          }
          onComplete(true)
          load()
        } else {
          _uiState.update {
            it.copy(
              isSending = false,
              errorMessage = "Failed to send message — check router response",
            )
          }
          onComplete(false)
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
            isSending = false,
            errorMessage = e.message ?: "Error sending message",
          )
        }
        onComplete(false)
      }
    }
  }

  fun clearSendSuccess() {
    _uiState.update { it.copy(isSendSuccess = false) }
  }

  override fun onCleared() {
    super.onCleared()
    api.close()
  }
}
