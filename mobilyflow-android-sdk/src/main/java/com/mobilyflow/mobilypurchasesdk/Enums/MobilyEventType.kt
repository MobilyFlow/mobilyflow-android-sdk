package com.mobilyflow.mobilypurchasesdk.Enums

enum class MobilyEventType(val value: Int) {
    TEST(0),
    PURCHASE(1),
    CONSUMED(2),
    RENEW(3),
    EXPIRED(4),
    REVOKED(5),
    REFUNDED(6),
    SUBSCRIPTION_CHANGE_RENEW_PRODUCT(7),
    SUBSCRIPTION_UPGRADE(8),
    SUBSCRIPTION_EXTENDED(9),
    CHANGE_AUTO_RENEW(10),
    CHANGE_PAUSE_STATUS(11),
    GRACE_PERIOD_RESOLVED(12),
    TRANSFER_OWNERSHIP_REQUEST(13),
    TRANSFER_OWNERSHIP_ACKNOWLEDGED(14);

    companion object {
        fun parse(strValue: String): MobilyEventType {
            return when (strValue) {
                "test" -> TEST
                "purchase" -> PURCHASE
                "consumed" -> CONSUMED
                "renew" -> RENEW
                "expired" -> EXPIRED
                "revoked" -> REVOKED
                "refunded" -> REFUNDED
                "subscription-change-renew-product" -> SUBSCRIPTION_CHANGE_RENEW_PRODUCT
                "subscription-upgrade" -> SUBSCRIPTION_UPGRADE
                "subscription-extended" -> SUBSCRIPTION_EXTENDED
                "change-auto-renew" -> CHANGE_AUTO_RENEW
                "change-pause-status" -> CHANGE_PAUSE_STATUS
                "grace-period-resolved" -> GRACE_PERIOD_RESOLVED
                "transfer-ownership-request" -> TRANSFER_OWNERSHIP_REQUEST
                "transfer-ownership-acknowledged" -> TRANSFER_OWNERSHIP_ACKNOWLEDGED
                else -> throw IllegalArgumentException("Unknown Event: ${strValue}")
            }
        }
    }
}