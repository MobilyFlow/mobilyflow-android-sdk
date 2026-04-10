package com.mobilyflow.mobilypurchasesdk.Enums

enum class MobilyEventType(val value: String) {
    TEST("TEST"),
    PURCHASED("PURCHASED"),
    CONSUMED("CONSUMED"),
    RENEWED("RENEWED"),
    EXPIRED("EXPIRED"),
    REVOKED("REVOKED"),
    REFUNDED("REFUNDED"),
    RENEW_PRODUCT_CHANGED("RENEW_PRODUCT_CHANGED"),
    UPGRADED("UPGRADED"),
    EXTENDED("EXTENDED"),
    AUTO_RENEW_CHANGED("AUTO_RENEW_CHANGED"),
    PAUSE_STATUS_CHANGED("PAUSE_STATUS_CHANGED"),
    GRACE_PERIOD_RESOLVED("GRACE_PERIOD_RESOLVED"),
    TRANSFER_OWNERSHIP_REQUESTED("TRANSFER_OWNERSHIP_REQUESTED"),
    TRANSFER_OWNERSHIP_ACKNOWLEDGED("TRANSFER_OWNERSHIP_ACKNOWLEDGED");

    companion object {
        // TODO: Retro-compatibility fallback
        private val legacyMap: Map<String, MobilyEventType> = mapOf(
            "test" to TEST,
            "purchase" to PURCHASED,
            "consumed" to CONSUMED,
            "renew" to RENEWED,
            "expired" to EXPIRED,
            "revoked" to REVOKED,
            "refunded" to REFUNDED,
            "subscription-change-renew-product" to RENEW_PRODUCT_CHANGED,
            "subscription-upgrade" to UPGRADED,
            "subscription-extended" to EXTENDED,
            "change-auto-renew" to AUTO_RENEW_CHANGED,
            "change-pause-status" to PAUSE_STATUS_CHANGED,
            "grace-period-resolved" to GRACE_PERIOD_RESOLVED,
            "transfer-ownership-request" to TRANSFER_OWNERSHIP_REQUESTED,
            "transfer-ownership-acknowledged" to TRANSFER_OWNERSHIP_ACKNOWLEDGED,
        )

        fun parse(value: String): MobilyEventType {
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

            throw IllegalArgumentException("Unknown MobilyEventType: $value")
        }
    }
}
