package com.mobilyflow.mobilypurchasesdk.BillingClientWrapper

class BillingClientException(val code: Int, message: String) : Exception(message) {}