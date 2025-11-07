package com.mobilyflow.mobilypurchasesdk.Enums

enum class MobilyEventType(val value: String) {
    TEST("test"),
    PURCHASE("purchase"),
    CONSUMED("consumed"),
    RENEW("renew"),
    EXPIRED("expired"),
    REVOKED("revoked"),
    REFUNDED("refunded"),
    SUBSCRIPTION_CHANGE_RENEW_PRODUCT("subscription-change-renew-product"),
    SUBSCRIPTION_UPGRADE("subscription-upgrade"),
    SUBSCRIPTION_EXTENDED("subscription-extended"),
    CHANGE_AUTO_RENEW("change-auto-renew"),
    CHANGE_PAUSE_STATUS("change-pause-status"),
    GRACE_PERIOD_RESOLVED("grace-period-resolved"),
    TRANSFER_OWNERSHIP_REQUEST("transfer-ownership-request"),
    TRANSFER_OWNERSHIP_ACKNOWLEDGED("transfer-ownership-acknowledged");

    companion object {
        fun parse(value: String): MobilyEventType {
            for (it in MobilyEventType.values()) {
                if (it.value == value) {
                    return it
                }
            }
            throw IllegalArgumentException("Unknown MobilyEventType: $value")
        }
    }
}
