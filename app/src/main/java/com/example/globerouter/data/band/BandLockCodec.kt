package com.example.globerouter.data.band

import com.example.globerouter.data.models.BandLockSetPayload
import com.example.globerouter.data.models.BandLockSnapshot
import kotlin.math.ceil
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object BandLockCodec {
  private val json = Json { ignoreUnknownKeys = true }
  private const val DEFAULT_WCDMA_LIST = "0,0,0"
  private const val DEFAULT_TDS_LIST = "0,0"

  fun parseSnapshot(currentInfoText: String, onceDataText: String): BandLockSnapshot {
    val currentInfo = parseJsonObject(currentInfoText)
    val onceData = parseJsonObject(onceDataText)
    val supportedBands = currentInfo.keys.mapNotNull(::parseBandNumber).sorted()
    val selectedBands = supportedBands.filter { band -> currentInfo.stringValue("band$band") == "1" }

    return BandLockSnapshot(
      enabled = currentInfo.stringValue("band_state") == "yes",
      supportedBands = supportedBands,
      selectedBands = selectedBands,
      wcdmaList = onceData.stringValue("tz_lock_wcdma_band") ?: DEFAULT_WCDMA_LIST,
      tdsList = onceData.stringValue("tz_lock_tds_band") ?: DEFAULT_TDS_LIST,
    )
  }

  fun buildPayload(
    enabled: Boolean,
    selectedBands: Set<Int>,
    snapshot: BandLockSnapshot,
  ): BandLockSetPayload {
    require(snapshot.supportedBands.isNotEmpty()) { "No LTE bands reported by router" }

    val normalizedBands = selectedBands.distinct().sorted()
    normalizedBands.forEach { band ->
      require(band in snapshot.supportedBands) { "Unsupported band: $band" }
    }
    require(!enabled || normalizedBands.isNotEmpty()) { "Select at least one LTE band" }

    val bandsForPayload = if (enabled) normalizedBands else snapshot.selectedBands
    val (bandList, zeact) = buildBandList(bandsForPayload, snapshot.supportedBands)

    return BandLockSetPayload(
      bandState = if (enabled) "yes" else "no",
      bandList = bandList,
      wcdmaList = snapshot.wcdmaList.ifBlank { DEFAULT_WCDMA_LIST },
      tdsList = snapshot.tdsList.ifBlank { DEFAULT_TDS_LIST },
      zeact = zeact,
    )
  }

  private fun parseJsonObject(text: String): JsonObject {
    return json.parseToJsonElement(text).jsonObject
  }

  private fun parseBandNumber(key: String): Int? {
    if (!key.startsWith("band")) return null

    val suffix = key.removePrefix("band")
    if (suffix.isEmpty() || suffix.any { !it.isDigit() }) return null
    return suffix.toIntOrNull()
  }

  private fun buildBandList(selectedBands: List<Int>, supportedBands: List<Int>): Pair<String, Int> {
    val maxBand = supportedBands.maxOrNull() ?: return "" to 0
    val bitLength = ceil(maxBand / 8.0).toInt() * 8
    val bits = MutableList(bitLength) { '0' }

    selectedBands.forEach { band ->
      bits[bitLength - band] = '1'
    }

    val bitString = bits.joinToString(separator = "")
    val byteValues = (0 until bitLength step 8)
      .map { start -> bitString.substring(start, start + 8).toInt(radix = 2).toString() }
    val bandList = byteValues.asReversed().joinToString(separator = ",")

    val lower = jsSubstring(bitString, bitString.length - 25)
    val upper = jsSubstring(bitString, bitString.length - 43, bitString.length - 32)
    val zeact = when {
      '1' !in lower -> 0
      '1' !in upper -> 1
      else -> 2
    }

    return bandList to zeact
  }

  private fun jsSubstring(value: String, start: Int, end: Int? = null): String {
    val boundedStart = start.coerceAtLeast(0)
    val boundedEnd = (end ?: value.length).coerceAtLeast(0).coerceAtMost(value.length)
    val from = minOf(boundedStart, boundedEnd)
    val to = maxOf(boundedStart, boundedEnd).coerceAtMost(value.length)
    return value.substring(from, to)
  }

  private fun JsonObject.stringValue(key: String): String? {
    return this[key]?.jsonPrimitive?.contentOrNull
  }
}
