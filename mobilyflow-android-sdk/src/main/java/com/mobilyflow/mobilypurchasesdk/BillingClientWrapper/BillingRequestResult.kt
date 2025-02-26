package com.mobilyflow.mobilypurchasesdk.BillingClientWrapper

import com.android.billingclient.api.BillingResult
import okhttp3.internal.notifyAll
import okhttp3.internal.wait

class BillingRequestResult<T> {
    private var _billingResult: BillingResult? = null
    private var _resultData: T? = null
    private var _isSet = false

    fun set(billingResult: BillingResult, data: T) {
        synchronized(this) {
            this._billingResult = billingResult
            this._resultData = data
            this._isSet = true
            this.notifyAll()
        }
    }

    fun waitResult() {
        synchronized(this) {
            if (!this._isSet) {
                this.wait()
            }
        }
    }

    fun billingResult(): BillingResult {
        return this._billingResult!!
    }

    fun data(): T {
        return this._resultData!!
    }

    fun dataOptional(): T? {
        return this._resultData
    }
}