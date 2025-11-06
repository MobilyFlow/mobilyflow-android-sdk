package com.mobilyflow.mobilypurchasesdk.Enums

enum class TransferOwnershipStatus(val value: String) {
    PENDING("pending"),
    DELAYED("delayed"),
    ACKNOWLEDGED("acknowledged"),
    REJECTED("rejected");

    companion object {
        fun parse(value: String): TransferOwnershipStatus {
            for (it in TransferOwnershipStatus.values()) {
                if (it.value == value) {
                    return it
                }
            }
            throw IllegalArgumentException("Unknown TransferOwnershipStatus: $value")
        }
    }
}