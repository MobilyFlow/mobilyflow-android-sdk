package com.mobilyflow.mobilypurchasesdk.Exceptions


class MobilyTransferOwnershipException(val type: Type, cause: Throwable? = null) :
    Exception(cause) {

    enum class Type {
        NOTHING_TO_TRANSFER,
        TRANSFER_TO_SAME_CUSTOMER,
        ALREADY_PENDING,

        WEBHOOK_NOT_PROCESSED,
        WEBHOOK_FAILED;
    }
}