package com.mobilyflow.mobilypurchasesdk.Utils

import android.icu.text.NumberFormat
import android.icu.util.Currency
import androidx.core.os.LocaleListCompat
import com.mobilyflow.mobilypurchasesdk.Monitoring.Logger
import org.json.JSONArray
import java.security.MessageDigest
import java.util.Locale

abstract class Utils {
    companion object {
        fun microToDouble(micros: Long): Double {
            return micros.toDouble() / 1000000
        }

        fun formatPrice(price: Double, currencyCode: String): String {
            try {
                val formatter = NumberFormat.getCurrencyInstance()
                formatter.currency = Currency.getInstance(currencyCode)
                return formatter.format(price)
            } catch (e: IllegalArgumentException) {
                Logger.e("formatPrice fail for args $price $currencyCode -> fallback")
                return String.format(Locale.getDefault(), "%.2f %s", price, currencyCode)
            }
        }

        fun calcWaitWebhookTime(retry: Int): Long {
            var delay = 2.0 + retry * 0.5
            delay = delay.coerceAtMost(5.0)
            return (delay * 1000).toLong() // Convert to milliseconds
        }

        fun jsonArrayToStringArray(array: JSONArray): Array<String> {
            return Array(array.length()) { index -> array.getString(index) }
        }

        fun sha256(message: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(message.toByteArray())
            return digest.fold("") { str, it -> str + "%02x".format(it) }
        }

        fun getPreferredLanguages(languages: Array<String>?): Array<String> {
            val usedLanguages = mutableListOf<String>()

            if (languages == null) {
                val systemLanguages = LocaleListCompat.getDefault()

                for (i in 0..<systemLanguages.size()) {
                    val splitted = systemLanguages.get(i)!!.toLanguageTag().split('-')
                    usedLanguages.add(splitted[0])
                }
            } else {
                for (lang in languages) {
                    val splitted = lang.split('-')
                    usedLanguages.add(splitted[0])
                }
            }

            return usedLanguages.toTypedArray()
        }
    }
}