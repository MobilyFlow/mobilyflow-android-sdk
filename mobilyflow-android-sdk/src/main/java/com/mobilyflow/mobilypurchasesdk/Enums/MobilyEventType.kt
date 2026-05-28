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
        fun parse(value: String): MobilyEventType {
            for (it in entries) {
                if (it.value == value) {
                    return it
                }
            }
            throw IllegalArgumentException("Unknown MobilyEventType: $value")
        }
    }
}
