package com.example.globerouter.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.globerouter.data.RouterApi
import com.example.globerouter.data.models.Credentials
import com.example.globerouter.data.models.DashboardData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {
  private val api = RouterApi()

  private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
  val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

  private var fastJob: Job? = null
  private var slowJob: Job? = null
  private var _currentData: DashboardData? = null

  /** Start polling — reads credentials from [Credentials]. */
  fun start() {
    fastJob?.cancel()
    slowJob?.cancel()

    // Slow loop (30s): status, monthly usage, device info
    slowJob = viewModelScope.launch {
      while (isActive) {
        try {
          val data = api.getDashboard(Credentials.username, Credentials.password)
          _currentData = data
          _uiState.value = DashboardUiState.Success(data)
        } catch (e: Exception) {
          _uiState.value = DashboardUiState.Error(e.message ?: "Unknown error")
        }
        delay(SLOW_INTERVAL_MS)
      }
    }

    // Fast loop (1s): signal + real-time traffic
    fastJob = viewModelScope.launch {
      delay(500) // let slow loop get the first snapshot
      while (isActive) {
        try {
          val fast = api.getFastData(Credentials.username, Credentials.password)
          val current = _currentData
          if (current != null) {
            val merged = current.copy(
              networkType = fast.network_type ?: current.networkType,
              rssi = fast.lte_rssi1?.toFloatOrNull()?.toInt() ?: fast.rssi?.toFloatOrNull()?.toInt() ?: current.rssi,
              rsrq = fast.lte_rsrq?.toFloatOrNull()?.toInt() ?: fast.rsrq?.toFloatOrNull()?.toInt() ?: current.rsrq,
              sinr = fast.lte_sinr?.toFloatOrNull() ?: fast.sinr?.toFloatOrNull() ?: current.sinr,
              webSignal = fast.web_signal?.toIntOrNull() ?: current.webSignal,
              realtimeTxThrpt = fast.realtime_tx_thrpt?.toLongOrNull() ?: current.realtimeTxThrpt,
              realtimeRxThrpt = fast.realtime_rx_thrpt?.toLongOrNull() ?: current.realtimeRxThrpt,
            )
            _currentData = merged
            _uiState.value = DashboardUiState.Success(merged)
          }
        } catch (_: Exception) {
          // fast poll failure is non-fatal — keep showing last known data
        }
        delay(FAST_INTERVAL_MS)
      }
    }
  }

  fun refresh() {
    viewModelScope.launch {
      try {
        val data = api.getDashboard(Credentials.username, Credentials.password)
        _currentData = data
        _uiState.value = DashboardUiState.Success(data)
      } catch (e: Exception) {
        _uiState.value = DashboardUiState.Error(e.message ?: "Unknown error")
      }
    }
  }

  fun reboot() {
    viewModelScope.launch {
      try {
        api.reboot(Credentials.username, Credentials.password)
        _uiState.value = DashboardUiState.Rebooting
      } catch (e: Exception) {
        _uiState.value = DashboardUiState.Error(e.message ?: "Reboot failed")
      }
    }
  }

  fun wanConnect() {
    viewModelScope.launch {
      try {
        api.wanConnect(Credentials.username, Credentials.password)
        refresh()
      } catch (_: Exception) { }
    }
  }

  fun wanDisconnect() {
    viewModelScope.launch {
      try {
        api.wanDisconnect(Credentials.username, Credentials.password)
        refresh()
      } catch (_: Exception) { }
    }
  }

  override fun onCleared() {
    super.onCleared()
    api.close()
  }

  companion object {
    private const val FAST_INTERVAL_MS = 500L
    private const val SLOW_INTERVAL_MS = 30_000L
  }
}

sealed interface DashboardUiState {
  data object Loading : DashboardUiState
  data class Success(val data: DashboardData) : DashboardUiState
  data class Error(val message: String) : DashboardUiState
  data object Rebooting : DashboardUiState
}
