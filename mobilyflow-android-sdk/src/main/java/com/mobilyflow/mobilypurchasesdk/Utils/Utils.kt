package com.mobilyflow.mobilypurchasesdk.Utils

import android.icu.text.NumberFormat
import android.icu.util.Currency
import android.os.Build
import androidx.core.os.LocaleListCompat
import com.mobilyflow.mobilypurchasesdk.Monitoring.Logger
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.json.JSONArray
import java.security.MessageDigest
import java.util.Locale
import java.text.NumberFormat as LegacyNumberFormat
import java.util.Currency as LegacyCurrency

abstract class Utils {
    companion object {
        fun microToMillis(micros: Long): Int {
            return (micros / 1000L).toInt()
        }

        fun formatPrice(priceMillis: Int, currencyCode: String): String {
            val price = priceMillis / 1000.0
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val formatter = NumberFormat.getCurrencyInstance()
                    formatter.currency = Currency.getInstance(currencyCode)
                    return formatter.format(price)
                } else {
                    val formatter = LegacyNumberFormat.getCurrencyInstance()
                    formatter.currency = LegacyCurrency.getInstance(currencyCode)
                    return formatter.format(price)
                }
            } catch (e: IllegalArgumentException) {
                Logger.e("formatPrice fail for args $priceMillis $currencyCode -> fallback")
                return String.format(Locale.getDefault(), "%.2f %s", price, currencyCode)
            }
        }

        fun calcWaitWebhookTime(retry: Int): Long {
            var delay = 2.0 + retry * 0.5
            delay = delay.coerceAtMost(5.0)
            return (delay * 1000).toLong() // Convert to milliseconds
        }

        fun jsonArrayToStringArray(array: JSONArray?): Array<String> {
            return if (array == null) {
                arrayOf()
            } else {
                Array(array.length()) { index -> array.getString(index) }
            }
        }

        fun sha256(message: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(message.toByteArray())
            return digest.fold("") { str, it -> str + "%02x".format(it) }
        }

        fun getPreferredLocales(locales: Array<String>?): Array<String> {
            if (locales == null) {
                val usedLocale = mutableListOf<String>()
                val systemLocales = LocaleListCompat.getDefault()

                for (i in 0..<systemLocales.size()) {
                    val locale = systemLocales.get(i)!!.toLanguageTag()
                    usedLocale.add(locale)
                }
                return usedLocale.toTypedArray()
            } else {
                return locales
            }
        }

        fun parseDate(isoDate: String): LocalDateTime {
            return Instant.parse(isoDate).toLocalDateTime(TimeZone.UTC)
        }
    }
}