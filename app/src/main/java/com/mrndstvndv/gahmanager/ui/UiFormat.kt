package com.mrndstvndv.gahmanager.ui

/** Format an LTE band number as "B{band}" or "—" if null. */
fun formatServingBand(band: Int?): String = band?.let { "B$it" } ?: "—"

/** Format a web signal value or "—" if null. */
fun formatSignalBars(webSignal: Int?): String = webSignal?.toString() ?: "—"
