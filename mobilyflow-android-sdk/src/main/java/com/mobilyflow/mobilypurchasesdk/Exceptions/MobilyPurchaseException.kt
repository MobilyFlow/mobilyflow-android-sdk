package com.mobilyflow.mobilypurchasesdk.Exceptions


class MobilyPurchaseException(val type: Type, cause: Throwable? = null) : Exception(cause) {
    enum class Type {
        PURCHASE_ALREADY_PENDING,

        PRODUCT_UNAVAILABLE,
        NETWORK_UNAVAILABLE,

        WEBHOOK_NOT_PRECESSED,
        WEBHOOK_FAILED,

        ALREADY_PURCHASED,
        NOT_MANAGED_BY_THIS_STORE_ACCOUNT,
        STORE_ACCOUNT_ALREADY_HAVE_PURCHASE,
        RENEW_ALREADY_ON_THIS_PLAN,

        USER_CANCELED,
    }
}