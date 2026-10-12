package com.mobilyflow.mobilypurchasesdk.Enums

import com.mobilyflow.mobilypurchasesdk.Monitoring.Logger

enum class MobilyStore(val value: String) {
    APP_STORE("app-store"),
    PLAY_STORE("play-store");

    companion object {
        /** Returns null (instead of throwing) for an unknown value, so stores added backend-side don't break installed SDKs. */
        fun parse(value: String?): MobilyStore? {
            if (value == null) {
                return null
            }
            for (it in entries) {
                if (it.value == value) {
                    return it
                }
            }
            Logger.w("[MobilyStore] Unknown store: $value")
            return null
        }
    }
}
