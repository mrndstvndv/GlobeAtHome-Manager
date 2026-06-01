package com.mrndstvndv.gahmanager.ui.band

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrndstvndv.gahmanager.data.RouterApi
import com.mrndstvndv.gahmanager.data.models.BandLockSnapshot
import com.mrndstvndv.gahmanager.data.models.Credentials
import com.mrndstvndv.gahmanager.data.models.RadioStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BandLockViewModel : ViewModel() {
  private val api = RouterApi()

  private val _uiState = MutableStateFlow(BandLockUiState())
  val uiState: StateFlow<BandLockUiState> = _uiState.asStateFlow()

  fun load() {
    val state = _uiState.value
    if (state.isLoading || state.isRefreshing || state.isApplying) return

    viewModelScope.launch {
      val hasSnapshot = _uiState.value.snapshot != null
      _uiState.update {
        it.copy(
          isLoading = !hasSnapshot,
          isRefreshing = hasSnapshot,
          errorMessage = null,
        )
      }

      try {
        val snapshot = api.getBandLockSnapshot(Credentials.username, Credentials.password)
        val radioStatus = api.getRadioStatus(Credentials.username, Credentials.password)
        showState(snapshot, radioStatus)
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
            isLoading = false,
            isRefreshing = false,
            isApplying = false,
            errorMessage = e.message ?: "Failed to load band lock",
          )
        }
      }
    }
  }

  fun toggleBand(band: Int) {
    val state = _uiState.value
    val snapshot = state.snapshot ?: return
    if (state.isApplying) return
    if (band !in snapshot.supportedBands) return

    _uiState.update {
      val draftBands = it.draftBands.toMutableSet()
      if (band in draftBands) {
        draftBands.remove(band)
      } else {
        draftBands.add(band)
      }
      it.copy(
        draftBands = draftBands,
        errorMessage = null,
      )
    }
  }

  fun selectAll() {
    val snapshot = _uiState.value.snapshot ?: return
    if (_uiState.value.isApplying) return

    _uiState.update {
      it.copy(
        draftBands = snapshot.supportedBands.toSet(),
        errorMessage = null,
      )
    }
  }

  fun resetDraft() {
    val snapshot = _uiState.value.snapshot ?: return
    if (_uiState.value.isApplying) return

    _uiState.update {
      it.copy(
        draftBands = snapshot.selectedBands.toSet(),
        errorMessage = null,
      )
    }
  }

  fun apply() {
    val state = _uiState.value
    if (state.snapshot == null) return
    if (state.isApplying || state.isLoading || state.isRefreshing) return
    if (state.draftBands.isEmpty()) {
      _uiState.update { it.copy(errorMessage = "Select at least one LTE band") }
      return
    }

    viewModelScope.launch {
      _uiState.update {
        it.copy(
          isApplying = true,
          errorMessage = null,
        )
      }

      try {
        val updated = api.setBandLock(
          username = Credentials.username,
          password = Credentials.password,
          selectedBands = state.draftBands,
        )
        val radioStatus = api.getRadioStatus(Credentials.username, Credentials.password)
        showState(updated, radioStatus)
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
            isApplying = false,
            errorMessage = e.message ?: "Failed to apply band lock",
          )
        }
      }
    }
  }

  fun disable() {
    val state = _uiState.value
    val snapshot = state.snapshot ?: return
    if (state.isApplying || state.isLoading || state.isRefreshing || !snapshot.enabled) return

    viewModelScope.launch {
      _uiState.update {
        it.copy(
          isApplying = true,
          errorMessage = null,
        )
      }

      try {
        val updated = api.disableBandLock(Credentials.username, Credentials.password)
        val radioStatus = api.getRadioStatus(Credentials.username, Credentials.password)
        showState(updated, radioStatus)
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
            isApplying = false,
            errorMessage = e.message ?: "Failed to disable band lock",
          )
        }
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    api.close()
  }

  private fun showState(
    snapshot: BandLockSnapshot,
    radioStatus: RadioStatus,
  ) {
    _uiState.value = BandLockUiState(
      isLoading = false,
      isRefreshing = false,
      isApplying = false,
      snapshot = snapshot,
      radioStatus = radioStatus,
      draftBands = snapshot.selectedBands.toSet(),
      errorMessage = null,
    )
  }
}

data class BandLockUiState(
  val isLoading: Boolean = false,
  val isRefreshing: Boolean = false,
  val isApplying: Boolean = false,
  val snapshot: BandLockSnapshot? = null,
  val radioStatus: RadioStatus? = null,
  val draftBands: Set<Int> = emptySet(),
  val errorMessage: String? = null,
) {
  val enabled: Boolean
    get() = snapshot?.enabled == true

  val supportedBands: List<Int>
    get() = snapshot?.supportedBands.orEmpty()

  val currentBands: Set<Int>
    get() = snapshot?.selectedBands?.toSet().orEmpty()

  val hasDraftChanges: Boolean
    get() = snapshot != null && draftBands != currentBands

  val canApply: Boolean
    get() {
      val snapshot = snapshot ?: return false
      if (isLoading || isRefreshing || isApplying || draftBands.isEmpty()) return false
      return !snapshot.enabled || draftBands != snapshot.selectedBands.toSet()
    }

  val canDisable: Boolean
    get() = enabled && !isLoading && !isRefreshing && !isApplying
}
