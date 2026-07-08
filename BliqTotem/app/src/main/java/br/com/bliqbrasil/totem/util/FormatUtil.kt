package br.com.bliqbrasil.totem.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

fun formatCurrency(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)

fun formatTime(seconds: Int): String {
    val s = abs(seconds)
    return "%02d:%02d".format(s / 60, s % 60)
}
