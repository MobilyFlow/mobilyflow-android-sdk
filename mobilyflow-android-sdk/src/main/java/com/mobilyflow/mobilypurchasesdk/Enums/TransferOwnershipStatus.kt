package com.mobilyflow.mobilypurchasesdk.Enums

enum class TransferOwnershipStatus(val value: Int) {
    PENDING(0),
    ERROR(1),
    DELAYED(2),
    ACKNOWLEDGED(3),
    REJECTED(4);
}