package com.example.globerouter.data.band

import com.example.globerouter.data.models.BandLockSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BandLockCodecTest {
  @Test
  fun parseSnapshot_reads_supported_and_selected_bands_dynamically() {
    val snapshot = BandLockCodec.parseSnapshot(
      currentInfoText = """
        {
          "band_state": "yes",
          "band1": "0",
          "band3": "1",
          "band5": "0",
          "band7": "0",
          "band8": "0",
          "band28": "1",
          "band40": "0",
          "band41": "0"
        }
      """.trimIndent(),
      onceDataText = """
        {
          "tz_lock_wcdma_band": "0,0,0",
          "tz_lock_tds_band": "0,0"
        }
      """.trimIndent(),
    )

    assertTrue(snapshot.enabled)
    assertEquals(listOf(1, 3, 5, 7, 8, 28, 40, 41), snapshot.supportedBands)
    assertEquals(listOf(3, 28), snapshot.selectedBands)
    assertEquals("0,0,0", snapshot.wcdmaList)
    assertEquals("0,0", snapshot.tdsList)
  }

  @Test
  fun buildPayload_matches_cli_for_band3() {
    val payload = BandLockCodec.buildPayload(
      enabled = true,
      selectedBands = setOf(3),
      snapshot = sampleSnapshot(),
    )

    assertEquals("yes", payload.bandState)
    assertEquals("4,0,0,0,0,0", payload.bandList)
    assertEquals(1, payload.zeact)
  }

  @Test
  fun buildPayload_matches_cli_for_band28() {
    val payload = BandLockCodec.buildPayload(
      enabled = true,
      selectedBands = setOf(28),
      snapshot = sampleSnapshot(),
    )

    assertEquals("yes", payload.bandState)
    assertEquals("0,0,0,8,0,0", payload.bandList)
    assertEquals(0, payload.zeact)
  }

  @Test
  fun buildPayload_matches_cli_for_band3_and_band28() {
    val payload = BandLockCodec.buildPayload(
      enabled = true,
      selectedBands = setOf(3, 28),
      snapshot = sampleSnapshot(),
    )

    assertEquals("yes", payload.bandState)
    assertEquals("4,0,0,8,0,0", payload.bandList)
    assertEquals(1, payload.zeact)
  }

  @Test
  fun disablePayload_uses_router_selection_and_turns_lock_off() {
    val payload = BandLockCodec.buildPayload(
      enabled = false,
      selectedBands = emptySet(),
      snapshot = sampleSnapshot(enabled = true, selectedBands = listOf(3, 28)),
    )

    assertEquals("no", payload.bandState)
    assertEquals("4,0,0,8,0,0", payload.bandList)
    assertEquals(1, payload.zeact)
  }

  @Test
  fun buildPayload_rejects_unsupported_band() {
    val error = assertThrows(IllegalArgumentException::class.java) {
      BandLockCodec.buildPayload(
        enabled = true,
        selectedBands = setOf(20),
        snapshot = sampleSnapshot(),
      )
    }

    assertTrue(error.message.orEmpty().contains("Unsupported band"))
  }

  @Test
  fun parseSnapshot_defaults_to_unlocked_when_band_state_missing() {
    val snapshot = BandLockCodec.parseSnapshot(
      currentInfoText = """
        {
          "band3": "1",
          "band28": "0"
        }
      """.trimIndent(),
      onceDataText = "{}",
    )

    assertFalse(snapshot.enabled)
    assertEquals(listOf(3, 28), snapshot.supportedBands)
    assertEquals(listOf(3), snapshot.selectedBands)
    assertEquals("0,0,0", snapshot.wcdmaList)
    assertEquals("0,0", snapshot.tdsList)
  }

  private fun sampleSnapshot(
    enabled: Boolean = true,
    selectedBands: List<Int> = listOf(3, 28),
  ): BandLockSnapshot {
    return BandLockSnapshot(
      enabled = enabled,
      supportedBands = listOf(1, 3, 5, 7, 8, 28, 40, 41),
      selectedBands = selectedBands,
      wcdmaList = "0,0,0",
      tdsList = "0,0",
    )
  }
}
