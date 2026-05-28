package com.mobilyflow.mobilypurchasesdk.Enums


enum class MobilyTransferOwnershipStatus(val value: String) {
    PENDING("PENDING"),
    DELAYED("DELAYED"),
    ACKNOWLEDGED("ACKNOWLEDGED"),
    REJECTED("REJECTED"),
    ERROR("ERROR");

    companion object {
        fun parse(value: String): MobilyTransferOwnershipStatus {
            for (it in entries) {
                if (it.value == value) {
                    return it
                }
            }
            throw IllegalArgumentException("Unknown TransferOwnershipStatus: $value")
        }
    }
}