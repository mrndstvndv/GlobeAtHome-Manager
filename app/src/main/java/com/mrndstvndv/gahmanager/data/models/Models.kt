package com.mrndstvndv.gahmanager.data.models

import kotlinx.serialization.Serializable

/** Raw flat JSON from the router's GET endpoint. */
@Serializable
data class RouterResponse(
  val modem_main_state: String? = null,
  val network_type: String? = null,
  val lte_band: String? = null,
  val sim_status: String? = null,
  val simcard_roam: String? = null,
  val lte_plmn: String? = null,
  val lte_rsrq: String? = null,
  val rsrq: String? = null,
  val lte_rssi1: String? = null,
  val rssi: String? = null,
  val lte_sinr: String? = null,
  val sinr: String? = null,
  val lte_pci: String? = null,
  val lte_enodebid: String? = null,
  val lte_cellid: String? = null,
  val wan_ipaddr: String? = null,
  val lan_ipaddr: String? = null,
  val ipv6_wan_ipaddr: String? = null,
  val ppp_status: String? = null,
  val rj45_state: String? = null,
  val sta_count: String? = null,
  val m_sta_count: String? = null,
  val SSID1: String? = null,
  val m_SSID: String? = null,
  val web_signal: String? = null,
  val realtime_tx_thrpt: String? = null,
  val realtime_rx_thrpt: String? = null,
  val realtime_tx_bytes: String? = null,
  val realtime_rx_bytes: String? = null,
  val realtime_time: String? = null,
  val monthly_tx_bytes: String? = null,
  val monthly_rx_bytes: String? = null,
  val monthly_time: String? = null,
  val cr_version: String? = null,
  val hardware_version: String? = null,
  val mac_address: String? = null,
  val msisdn: String? = null,
  val imei: String? = null,
  val loginfo: String? = null,
)

/** Parsed dashboard data derived from raw response. */
data class DashboardData(
  val connected: Boolean,
  val networkType: String,
  val servingBand: Int?,
  val wanIp: String,
  val sessionDuration: String,
  val rssi: Int,
  val rsrq: Int,
  val sinr: Float?,
  val webSignal: Int?,
  val realtimeTxThrpt: Long,
  val realtimeRxThrpt: Long,
  val monthlyTxBytes: Long,
  val monthlyRxBytes: Long,
  val monthlyTime: String,
  val staCount: Int,
  val mStaCount: Int,
  val ssid24: String,
  val ssid5: String,
  val firmware: String,
) {
  companion object {
    fun from(raw: RouterResponse): DashboardData {
      val rssi = raw.lte_rssi1?.toFloatOrNull()?.toInt() ?: raw.rssi?.toFloatOrNull()?.toInt() ?: -120
      val rsrq = raw.lte_rsrq?.toFloatOrNull()?.toInt() ?: raw.rsrq?.toFloatOrNull()?.toInt() ?: -20
      val sinr = raw.lte_sinr?.toFloatOrNull() ?: raw.sinr?.toFloatOrNull()

      return DashboardData(
        connected = raw.isWanConnected(),
        networkType = raw.network_type ?: "—",
        servingBand = raw.lte_band.toServingBand(),
        wanIp = raw.wan_ipaddr ?: "—",
        sessionDuration = formatSeconds(raw.realtime_time?.toLongOrNull()),
        rssi = rssi,
        rsrq = rsrq,
        sinr = sinr,
        webSignal = raw.web_signal?.toIntOrNull(),
        realtimeTxThrpt = raw.realtime_tx_thrpt?.toLongOrNull() ?: 0L,
        realtimeRxThrpt = raw.realtime_rx_thrpt?.toLongOrNull() ?: 0L,
        monthlyTxBytes = raw.monthly_tx_bytes?.toLongOrNull() ?: 0L,
        monthlyRxBytes = raw.monthly_rx_bytes?.toLongOrNull() ?: 0L,
        monthlyTime = formatSeconds(raw.monthly_time?.toLongOrNull()),
        staCount = raw.sta_count?.toIntOrNull() ?: 0,
        mStaCount = raw.m_sta_count?.toIntOrNull() ?: 0,
        ssid24 = raw.SSID1 ?: "—",
        ssid5 = raw.m_SSID ?: "—",
        firmware = raw.cr_version ?: "—",
      )
    }

    private fun formatSeconds(seconds: Long?): String {
      if (seconds == null) return "—"
      val days = seconds / 86400
      val hours = (seconds % 86400) / 3600
      val mins = (seconds % 3600) / 60
      return buildString {
        if (days > 0) append("${days}d ")
        if (hours > 0 || days > 0) append("${hours}h ")
        append("${mins}m")
      }
    }
  }
}

data class RadioStatus(
  val networkType: String,
  val servingBand: Int?,
  val webSignal: Int?,
) {
  companion object {
    fun from(raw: RouterResponse): RadioStatus {
      return RadioStatus(
        networkType = raw.network_type ?: "—",
        servingBand = raw.lte_band.toServingBand(),
        webSignal = raw.web_signal?.toIntOrNull(),
      )
    }
  }
}

internal fun String?.toServingBand(): Int? {
  val value = this?.trim().orEmpty()
  if (value.isBlank()) return null
  return value.toIntOrNull()
}

internal fun RouterResponse.isWanConnected(): Boolean {
  val pppStatus = ppp_status?.trim().orEmpty()
  if (pppStatus.isNotEmpty()) return pppStatus.isConnectedStatus()

  return wan_ipaddr.isUsableWanIp() || ipv6_wan_ipaddr.isUsableWanIp()
}

private fun String.isConnectedStatus(): Boolean {
  val normalized = trim().lowercase()
  if (normalized.isBlank()) return false
  if ("disconnect" in normalized) return false
  return "connected" in normalized
}

private fun String?.isUsableWanIp(): Boolean {
  val value = this?.trim().orEmpty()
  if (value.isBlank()) return false
  return value != "0.0.0.0" && value != "::" && value != "—"
}

/** Login response from the router. */
@Serializable
data class LoginResponse(
  val result: String? = null,
)
