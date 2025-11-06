package com.mobilyflow.mobilypurchasesdk.Enums

enum class Platform(val value: String) {
    IOS("ios"),
    ANDROID("android");

    companion object {
        fun parse(value: String): Platform {
            for (it in Platform.values()) {
                if (it.value == value) {
                    return it
                }
            }
            throw IllegalArgumentException("Unknown Platform: $value")
        }
    }
}