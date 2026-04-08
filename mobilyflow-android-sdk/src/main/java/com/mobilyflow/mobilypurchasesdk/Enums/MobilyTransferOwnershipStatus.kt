package com.mobilyflow.mobilypurchasesdk.Enums


enum class MobilyTransferOwnershipStatus(val value: String) {
    PENDING("PENDING"),
    DELAYED("DELAYED"),
    ACKNOWLEDGED("ACKNOWLEDGED"),
    REJECTED("REJECTED"),
    ERROR("ERROR");

    companion object {
        // TODO: Retro-compatibility fallback
        private val legacyMap: Map<String, MobilyTransferOwnershipStatus> = mapOf(
            "pending" to PENDING,
            "delayed" to DELAYED,
            "acknowledged" to ACKNOWLEDGED,
            "rejected" to REJECTED,
            "error" to ERROR
        )

        fun parse(value: String): MobilyTransferOwnershipStatus {
            for (it in entries) {
                if (it.value == value) {
                    return it
                }
            }

            // TODO: Retro-compatibility fallback
            val legacy = legacyMap[value]
            if (legacy != null) {
                return legacy
            }
            // -----------------------------

            throw IllegalArgumentException("Unknown TransferOwnershipStatus: $value")
        }
    }
}