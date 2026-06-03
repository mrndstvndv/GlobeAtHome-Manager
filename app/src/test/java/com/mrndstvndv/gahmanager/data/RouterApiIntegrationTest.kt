package com.mrndstvndv.gahmanager.data

import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.InetAddress

class RouterApiIntegrationTest {

  private val routerIp = System.getenv("ROUTER_IP") ?: RouterApi.DEFAULT_IP
  private val username = System.getenv("ROUTER_USER") ?: "admin"
  private val password = System.getenv("ROUTER_PASS") ?: ""

  private lateinit var api: RouterApi
  private var isReachable = false

  @Before
  fun setUp() {
    api = RouterApi(routerIp)
    // Check if the IP is pingable/reachable to decide whether to run live tests
    isReachable = try {
      val address = InetAddress.getByName(routerIp)
      address.isReachable(2000)
    } catch (e: Exception) {
      false
    }

    if (!isReachable) {
      println("⚠️ Live router not reachable at $routerIp. Skipping live integration tests.")
    }
  }

  @Test
  fun testConnectionAndSession() = runBlocking {
    Assume.assumeTrue("Router must be reachable to run integration tests", isReachable)

    println("🔌 Testing connection to router at $routerIp...")
    try {
      val sessionValid = api.checkSession()
      println("Session check completed. Is session valid? $sessionValid")
    } catch (e: Exception) {
      println("❌ Failed to connect to router API: ${e.message}")
      throw e
    }
  }

  @Test
  fun testLiveRouterDiagnostics() = runBlocking {
    Assume.assumeTrue("Router must be reachable to run integration tests", isReachable)
    Assume.assumeTrue("Router password must be supplied to run authenticated tests", password.isNotEmpty())

    println("🔑 Logging in to router...")
    val loggedIn = api.login(username, password)
    println("Login result: $loggedIn")
    Assume.assumeTrue("Login must succeed to run diagnostics", loggedIn)

    println("\n--- 📋 System & Radio Diagnostics ---")
    try {
      val dashboard = api.getDashboard(username, password)
      println("Dashboard State: $dashboard")

      val radio = api.getRadioStatus(username, password)
      println("Radio Status: $radio")
    } catch (e: Exception) {
      println("⚠️ Error loading dashboard/radio info: ${e.message}")
    }

    println("\n--- 💬 Probing SMS / Message API Compatibility ---")
    val smsFields = listOf(
      "sms_unread_count",
      "sms_inbox_count",
      "sms_capacity_info",
      "sms_data_total",
      "sms_page_data",
      "sms_inbox_total",
      "sms_outbox_total",
      "sms_draftbox_total"
    )

    for (field in smsFields) {
      try {
        val rawResponse = api.queryRaw(field)
        println("Field '$field' response: $rawResponse")
      } catch (e: Exception) {
        println("Field '$field' failed/unsupported: ${e.message}")
      }
    }

    println("\n--- 🌐 Probing All Potential Custom GET Fields ---")
    val queryFields = "modem_main_state,network_type,lte_band,web_signal,wan_ipaddr,ppp_status,sta_count,msisdn,imei"
    try {
      val raw = api.queryRaw(queryFields)
      println("Raw response for standard status fields: $raw")
    } catch (e: Exception) {
      println("Failed standard status query: ${e.message}")
    }

    api.close()
  }
}
