package com.mobilyflow.mobilypurchasesdk.Enums

enum class MobilyTransferOwnershipStatus(val value: String) {
    PENDING("pending"),
    DELAYED("delayed"),
    ACKNOWLEDGED("acknowledged"),
    REJECTED("rejected");

    companion object {
        fun parse(value: String): MobilyTransferOwnershipStatus {
            for (it in MobilyTransferOwnershipStatus.values()) {
                if (it.value == value) {
                    return it
                }
            }
            throw IllegalArgumentException("Unknown TransferOwnershipStatus: $value")
        }
    }
}