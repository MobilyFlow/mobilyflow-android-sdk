package com.mobilyflow.mobilypurchasesdk.Exceptions

class MobilyException(val type: Type, cause: Throwable? = null) : Exception(cause) {
    enum class Type {
        STORE_UNAVAILABLE, // Main cause: user not logged into PlayStore
        SERVER_UNAVAILABLE,
        NO_CUSTOMER_LOGGED,
        UNKNOWN_ERROR,
        SDK_NOT_INITIALIZED,
    }
}