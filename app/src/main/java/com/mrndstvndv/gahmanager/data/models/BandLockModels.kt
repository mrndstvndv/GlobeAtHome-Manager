package com.mrndstvndv.gahmanager.data.models

/** Parsed LTE band lock state from the router. */
data class BandLockSnapshot(
  val enabled: Boolean,
  val supportedBands: List<Int>,
  val selectedBands: List<Int>,
  val wcdmaList: String,
  val tdsList: String,
)

/** Form payload for TZ_SET_LOCK_BAND. */
data class BandLockSetPayload(
  val bandState: String,
  val bandList: String,
  val wcdmaList: String,
  val tdsList: String,
  val zeact: Int,
) {
  fun asFields(): Map<String, String> {
    return linkedMapOf(
      "band_state" to bandState,
      "band_list" to bandList,
      "wcdma_list" to wcdmaList,
      "tds_list" to tdsList,
      "zeact" to zeact.toString(),
    )
  }
}
