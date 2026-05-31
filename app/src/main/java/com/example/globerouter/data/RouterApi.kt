package com.example.globerouter.data

import com.example.globerouter.data.models.BandLockInfo
import com.example.globerouter.data.models.BandLockResponse
import com.example.globerouter.data.models.DashboardData
import com.example.globerouter.data.models.LoginResponse
import com.example.globerouter.data.models.RouterResponse
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
import io.ktor.http.contentType
import java.io.IOException
import kotlinx.serialization.json.Json
import java.util.Base64

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

    val response = client.post("$baseUrl/goform/goform_set_cmd_process") {
      setBody("isTest=false&goformId=LOGIN&username=$userB64&password=$passB64")
      contentType(ContentType.Application.FormUrlEncoded)
    }

    val body = response.bodyAsText()
    val loginResp = json.decodeFromString<LoginResponse>(body)
    return loginResp.result == "0"
  }

  /** Check if the current session is still valid. */
  suspend fun checkSession(): Boolean {
    val body = queryRaw("loginfo")
    return "\"loginfo\":\"ok\"" in body
  }

  /** Ensure we're logged in, re-authenticating if necessary. */
  suspend fun ensureLoggedIn(username: String, password: String) {
    if (!checkSession()) {
      val ok = login(username, password)
      if (!ok) throw IOException("Login failed — check credentials")
    }
  }

  /** Fetch dashboard data (status + signal + traffic in one call). */
  suspend fun getDashboard(username: String, password: String): DashboardData {
    ensureLoggedIn(username, password)
    val raw = queryRaw(
      "modem_main_state,network_type,lte_plmn,lte_rsrq,lte_rssi1,lte_sinr," +
        "lte_pci,lte_enodebid,lte_cellid,wan_ipaddr,ppp_status," +
        "sta_count,m_sta_count,SSID1,m_SSID," +
        "realtime_tx_thrpt,realtime_rx_thrpt,realtime_tx_bytes,realtime_rx_bytes,realtime_time," +
        "monthly_tx_bytes,monthly_rx_bytes,monthly_time," +
        "cr_version,hardware_version,mac_address,msisdn,web_signal,sim_status,simcard_roam"
    )
    val resp = json.decodeFromString<RouterResponse>(raw)
    return DashboardData.from(resp)
  }

  /** Fetch just signal info. */
  suspend fun getSignal(username: String, password: String): RouterResponse {
    ensureLoggedIn(username, password)
    val raw = queryRaw("lte_rsrq,lte_rssi1,lte_sinr,lte_pci,lte_enodebid,lte_cellid,web_signal,network_type,simcard_roam")
    return json.decodeFromString(raw)
  }

  /** Fetch just traffic stats. */
  suspend fun getTraffic(username: String, password: String): RouterResponse {
    ensureLoggedIn(username, password)
    val raw = queryRaw(
      "realtime_tx_thrpt,realtime_rx_thrpt,realtime_tx_bytes,realtime_rx_bytes,realtime_time," +
        "monthly_tx_bytes,monthly_rx_bytes,monthly_time"
    )
    return json.decodeFromString(raw)
  }

  /** Fast poll: signal + realtime traffic in one call (1s interval). */
  suspend fun getFastData(username: String, password: String): RouterResponse {
    ensureLoggedIn(username, password)
    val raw = queryRaw(
      "lte_rsrq,lte_rssi1,lte_sinr,lte_pci,lte_enodebid,lte_cellid," +
        "web_signal,network_type,simcard_roam," +
        "realtime_tx_thrpt,realtime_rx_thrpt,realtime_tx_bytes,realtime_rx_bytes,realtime_time"
    )
    return json.decodeFromString(raw)
  }

  /** Get current LTE band lock status. */
  suspend fun getBandLock(username: String, password: String): BandLockInfo {
    ensureLoggedIn(username, password)
    val body = client.post("$baseUrl/goform/goform_set_cmd_process") {
      setBody("isTest=false&goformId=TZ_GET_LOCK_BAND")
      contentType(ContentType.Application.FormUrlEncoded)
    }.bodyAsText()
    val resp = json.decodeFromString<BandLockResponse>(body)
    return BandLockInfo.from(resp)
  }

  /** Reboot the router. */
  suspend fun reboot(username: String, password: String) {
    ensureLoggedIn(username, password)
    client.post("$baseUrl/goform/goform_set_cmd_process") {
      setBody("isTest=false&goformId=REBOOT_DEVICE")
      contentType(ContentType.Application.FormUrlEncoded)
    }
  }

  /** Connect WAN. */
  suspend fun wanConnect(username: String, password: String) {
    ensureLoggedIn(username, password)
    client.post("$baseUrl/goform/goform_set_cmd_process") {
      setBody("isTest=false&goformId=wan_connect")
      contentType(ContentType.Application.FormUrlEncoded)
    }
  }

  /** Disconnect WAN. */
  suspend fun wanDisconnect(username: String, password: String) {
    ensureLoggedIn(username, password)
    client.post("$baseUrl/goform/goform_set_cmd_process") {
      setBody("isTest=false&goformId=wan_disconnect")
      contentType(ContentType.Application.FormUrlEncoded)
    }
  }

  /** Execute an arbitrary GET command. */
  suspend fun queryRaw(cmd: String): String {
    return client.post("$baseUrl/goform/goform_get_cmd_process") {
      setBody("isTest=false&cmd=$cmd&multi_data=1")
      contentType(ContentType.Application.FormUrlEncoded)
    }.bodyAsText()
  }

  fun close() {
    client.close()
  }

  companion object {
    const val DEFAULT_IP = "192.168.254.254"
  }
}
