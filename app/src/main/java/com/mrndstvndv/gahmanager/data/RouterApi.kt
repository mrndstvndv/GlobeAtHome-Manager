package com.mrndstvndv.gahmanager.data

import com.mrndstvndv.gahmanager.data.band.BandLockCodec
import com.mrndstvndv.gahmanager.data.models.BandLockSnapshot
import com.mrndstvndv.gahmanager.data.models.DashboardData
import com.mrndstvndv.gahmanager.data.models.LoginResponse
import com.mrndstvndv.gahmanager.data.models.RadioStatus
import com.mrndstvndv.gahmanager.data.models.RouterResponse
import com.mrndstvndv.gahmanager.data.models.SmsMessage
import com.mrndstvndv.gahmanager.data.models.SmsResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.formUrlEncode
import java.io.IOException
import java.util.Base64
import kotlinx.serialization.json.Json

/** Ktor HTTP client for the Globe At Home B9680 router API. */
class RouterApi(private val routerIp: String = DEFAULT_IP) {
  private val baseUrl = "http://$routerIp"
  private val json = Json { ignoreUnknownKeys = true }

  private val client = HttpClient(OkHttp) {
    install(HttpCookies)
    install(HttpTimeout) {
      requestTimeoutMillis = 15_000
      connectTimeoutMillis = 10_000
    }
    install(Logging) {
      level = LogLevel.HEADERS
    }
  }

  /** Authenticate with the router. Returns true on success. */
  suspend fun login(username: String, password: String): Boolean {
    val userB64 = Base64.getEncoder().encodeToString(username.encodeToByteArray())
    val passB64 = Base64.getEncoder().encodeToString(password.encodeToByteArray())
    val body = goformSet(
      goformId = "LOGIN",
      fields = mapOf(
        "username" to userB64,
        "password" to passB64,
      ),
    )
    val loginResponse = json.decodeFromString<LoginResponse>(body)
    return loginResponse.result == "0"
  }

  /** Check if the current session is still valid. */
  suspend fun checkSession(): Boolean {
    val body = goformGet("loginfo")
    return "\"loginfo\":\"ok\"" in body
  }

  /** Ensure we're logged in, re-authenticating if necessary. */
  suspend fun ensureLoggedIn(username: String, password: String) {
    if (checkSession()) return

    val loggedIn = login(username, password)
    if (!loggedIn) throw IOException("Login failed — check credentials")
  }

  /** Fetch dashboard data (status + signal + traffic in one call). */
  suspend fun getDashboard(username: String, password: String): DashboardData {
    ensureLoggedIn(username, password)
    val raw = goformGet(
      "modem_main_state,network_type,lte_band,lte_plmn,lte_rsrq,lte_rssi1,lte_sinr," +
        "lte_pci,lte_enodebid,lte_cellid,wan_ipaddr,ppp_status," +
        "sta_count,m_sta_count,SSID1,m_SSID," +
        "realtime_tx_thrpt,realtime_rx_thrpt,realtime_tx_bytes,realtime_rx_bytes,realtime_time," +
        "monthly_tx_bytes,monthly_rx_bytes,monthly_time," +
        "cr_version,hardware_version,mac_address,msisdn,web_signal,sim_status,simcard_roam," +
        "sms_unread_count,sms_unread_num"
    )
    val response = json.decodeFromString<RouterResponse>(raw)
    return DashboardData.from(response)
  }

  /** Fetch just signal info. */
  suspend fun getSignal(username: String, password: String): RouterResponse {
    ensureLoggedIn(username, password)
    val raw = goformGet("lte_band,lte_rsrq,lte_rssi1,lte_sinr,lte_pci,lte_enodebid,lte_cellid,web_signal,network_type,simcard_roam")
    return json.decodeFromString(raw)
  }

  /** Fetch just traffic stats. */
  suspend fun getTraffic(username: String, password: String): RouterResponse {
    ensureLoggedIn(username, password)
    val raw = goformGet(
      "realtime_tx_thrpt,realtime_rx_thrpt,realtime_tx_bytes,realtime_rx_bytes,realtime_time," +
        "monthly_tx_bytes,monthly_rx_bytes,monthly_time"
    )
    return json.decodeFromString(raw)
  }

  /** Fast poll: signal + real-time traffic in one call (1s interval). */
  suspend fun getFastData(username: String, password: String): RouterResponse {
    ensureLoggedIn(username, password)
    val raw = goformGet(
      "lte_band,lte_rsrq,lte_rssi1,lte_sinr,lte_pci,lte_enodebid,lte_cellid," +
        "web_signal,network_type,simcard_roam," +
        "realtime_tx_thrpt,realtime_rx_thrpt,realtime_tx_bytes,realtime_rx_bytes,realtime_time"
    )
    return json.decodeFromString(raw)
  }

  /** Read current radio status. */
  suspend fun getRadioStatus(username: String, password: String): RadioStatus {
    ensureLoggedIn(username, password)
    val raw = goformGet("network_type,lte_band,web_signal")
    val response = json.decodeFromString<RouterResponse>(raw)
    return RadioStatus.from(response)
  }

  /** Read current LTE band lock state. */
  suspend fun getBandLockSnapshot(username: String, password: String): BandLockSnapshot {
    ensureLoggedIn(username, password)
    return loadBandLockSnapshot()
  }

  /** Apply a new LTE band lock selection. */
  suspend fun setBandLock(
    username: String,
    password: String,
    selectedBands: Set<Int>,
  ): BandLockSnapshot {
    ensureLoggedIn(username, password)

    val currentSnapshot = loadBandLockSnapshot()
    val payload = BandLockCodec.buildPayload(
      enabled = true,
      selectedBands = selectedBands,
      snapshot = currentSnapshot,
    )

    goformSet("TZ_SET_LOCK_BAND", payload.asFields())

    val updatedSnapshot = loadBandLockSnapshot()
    validateBandLock(updatedSnapshot, expectedEnabled = true, expectedBands = selectedBands)
    return updatedSnapshot
  }

  /** Disable LTE band lock while preserving the router's other lock fields. */
  suspend fun disableBandLock(username: String, password: String): BandLockSnapshot {
    ensureLoggedIn(username, password)

    val currentSnapshot = loadBandLockSnapshot()
    val payload = BandLockCodec.buildPayload(
      enabled = false,
      selectedBands = emptySet(),
      snapshot = currentSnapshot,
    )

    goformSet("TZ_SET_LOCK_BAND", payload.asFields())

    val updatedSnapshot = loadBandLockSnapshot()
    validateBandLock(updatedSnapshot, expectedEnabled = false, expectedBands = emptySet())
    return updatedSnapshot
  }

  /** Reboot the router. */
  suspend fun reboot(username: String, password: String) {
    ensureLoggedIn(username, password)
    goformSet("REBOOT_DEVICE")
  }

  /** Connect WAN. */
  suspend fun wanConnect(username: String, password: String) {
    ensureLoggedIn(username, password)
    goformSet("wan_connect")
  }

  /** Disconnect WAN. */
  suspend fun wanDisconnect(username: String, password: String) {
    ensureLoggedIn(username, password)
    goformSet("wan_disconnect")
  }

  /** Fetch list of SMS messages. */
  suspend fun getMessages(
    username: String,
    password: String,
    page: Int = 0,
    dataPerPage: Int = 500,
  ): List<SmsMessage> {
    ensureLoggedIn(username, password)
    val raw = postForm(
      path = "/goform/goform_get_cmd_process",
      fields = mapOf(
        "isTest" to "false",
        "cmd" to "sms_data_total",
        "page" to page.toString(),
        "data_per_page" to dataPerPage.toString(),
        "mem_store" to "1",
        "tags" to "10",
        "order_by" to "order by id desc"
      )
    )
    val response = json.decodeFromString<SmsResponse>(raw)
    val messages = response.messages ?: emptyList()
    return messages.sortedByDescending { it.id.toIntOrNull() ?: 0 }
  }

  /** Delete SMS messages by ID list. */
  suspend fun deleteMessages(
    username: String,
    password: String,
    ids: List<String>,
  ) {
    ensureLoggedIn(username, password)
    goformSet(
      goformId = "DELETE_SMS",
      fields = mapOf(
        "msg_id" to ids.joinToString(",")
      )
    )
  }

  /** Send an SMS message. */
  suspend fun sendSms(
    username: String,
    password: String,
    number: String,
    content: String,
  ): Boolean {
    ensureLoggedIn(username, password)
    val calendar = java.util.Calendar.getInstance()
    val year = (calendar.get(java.util.Calendar.YEAR) % 100).toString().padStart(2, '0')
    val month = (calendar.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')
    val day = calendar.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val minute = calendar.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')
    val second = calendar.get(java.util.Calendar.SECOND).toString().padStart(2, '0')
    val timeStr = "$year;$month;$day;$hour;$minute;$second;+8"

    val bytes = content.toByteArray(Charsets.UTF_16BE)
    val hexContent = bytes.joinToString("") { "%02X".format(it) }

    val rawResponse = goformSet(
      goformId = "SEND_SMS",
      fields = mapOf(
        "Number" to number,
        "sms_time" to timeStr,
        "MessageBody" to hexContent,
        "ID" to "-1",
        "user_ip" to "192.168.254.10"
      )
    )
    return "success" in rawResponse.lowercase() || "result\":\"0\"" in rawResponse
  }

  fun close() {
    client.close()
  }

  private suspend fun loadBandLockSnapshot(): BandLockSnapshot {
    val currentInfo = goformSet("TZ_GET_LOCK_BAND")
    val onceData = goformGet("tz_wcdma_bands,tz_tds_bands,tz_lock_wcdma_band,tz_lock_tds_band")
    return BandLockCodec.parseSnapshot(currentInfo, onceData)
  }

  private fun validateBandLock(
    snapshot: BandLockSnapshot,
    expectedEnabled: Boolean,
    expectedBands: Set<Int>,
  ) {
    if (!expectedEnabled) {
      if (snapshot.enabled) throw IOException("Router did not disable band lock")
      return
    }

    val normalizedBands = expectedBands.toSortedSet()
    if (!snapshot.enabled || snapshot.selectedBands.toSet() != normalizedBands) {
      throw IOException("Router did not apply requested band lock")
    }
  }

  suspend fun goformGet(cmd: String): String {
    return postForm(
      path = "/goform/goform_get_cmd_process",
      fields = mapOf(
        "isTest" to "false",
        "cmd" to cmd,
        "multi_data" to "1",
      ),
    )
  }

  private suspend fun goformSet(
    goformId: String,
    fields: Map<String, String> = emptyMap(),
  ): String {
    val formFields = linkedMapOf(
      "isTest" to "false",
      "goformId" to goformId,
    )
    formFields.putAll(fields)

    return postForm(
      path = "/goform/goform_set_cmd_process",
      fields = formFields,
    )
  }

  private suspend fun postForm(
    path: String,
    fields: Map<String, String>,
  ): String {
    val body = Parameters.build {
      fields.forEach { (key, value) ->
        append(key, value)
      }
    }.formUrlEncode()

    return client.post("$baseUrl$path") {
      setBody(body)
      contentType(ContentType.Application.FormUrlEncoded)
    }.bodyAsText()
  }

  companion object {
    const val DEFAULT_IP = "192.168.254.254"
  }
}
