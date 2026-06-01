package com.mrndstvndv.gahmanager.data.models

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardDataTest {
  @Test
  fun `marks ppp_connected as connected`() {
    val data = DashboardData.from(
      RouterResponse(
        ppp_status = "ppp_connected",
        network_type = "LTE",
        wan_ipaddr = "10.60.88.34",
      ),
    )

    assertTrue(data.connected)
  }

  @Test
  fun `accepts connected status variants`() {
    val data = DashboardData.from(
      RouterResponse(
        ppp_status = "Connected",
        network_type = "LTE",
      ),
    )

    assertTrue(data.connected)
  }

  @Test
  fun `falls back to wan ip when ppp status is missing`() {
    val data = DashboardData.from(
      RouterResponse(
        ppp_status = null,
        wan_ipaddr = "10.60.88.34",
      ),
    )

    assertTrue(data.connected)
  }

  @Test
  fun `does not treat disconnected ppp status as connected`() {
    val data = DashboardData.from(
      RouterResponse(
        ppp_status = "ppp_disconnected",
        wan_ipaddr = "10.60.88.34",
      ),
    )

    assertFalse(data.connected)
  }
}
