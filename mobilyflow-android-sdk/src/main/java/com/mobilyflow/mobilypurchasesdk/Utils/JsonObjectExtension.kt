package com.mobilyflow.mobilypurchasesdk.Utils

import org.json.JSONObject

/**
 * Returns the value mapped by name if it exists, coercing it if necessary.
 * If no such mapping exists or is null, return null.
 */
fun JSONObject.optStringNull(key: String): String? {
    if (this.has(key) && !this.isNull(key)) {
        return this.getString(key)
    } else {
        return null
    }
}
