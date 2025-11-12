package com.mobilyflow.mobilypurchasesdk.BillingClientWrapper

import com.android.billingclient.api.BillingResult

class BillingRequestResult<T> {
    private val lock = Object()

    private var _billingResult: BillingResult? = null
    private var _resultData: T? = null
    private var _isSet = false

    fun set(billingResult: BillingResult, data: T) {
        synchronized(this.lock) {
            this._billingResult = billingResult
            this._resultData = data
            this._isSet = true
            this.lock.notifyAll()
        }
    }

    fun waitResult() {
        synchronized(this) {
            if (!this._isSet) {
                this.lock.wait()
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