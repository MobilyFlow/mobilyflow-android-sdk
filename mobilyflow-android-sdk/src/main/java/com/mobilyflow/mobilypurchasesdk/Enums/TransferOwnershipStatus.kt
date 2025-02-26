package com.mobilyflow.mobilypurchasesdk.Enums

enum class TransferOwnershipStatus(val value: String) {
    PENDING("pending"),
    DELAYED("delayed"),
    ACKNOWLEDGED("acknowledged"),
    REJECTED("rejected"),
    ERROR("error");
}