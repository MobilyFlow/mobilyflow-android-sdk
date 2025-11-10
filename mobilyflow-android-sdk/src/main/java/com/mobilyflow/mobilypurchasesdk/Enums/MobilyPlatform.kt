package com.mobilyflow.mobilypurchasesdk.Enums

enum class MobilyPlatform(val value: String) {
    IOS("ios"),
    ANDROID("android");

    companion object {
        fun parse(value: String): MobilyPlatform {
            for (it in MobilyPlatform.values()) {
                if (it.value == value) {
                    return it
                }
            }
            throw IllegalArgumentException("Unknown Platform: $value")
        }
    }
}