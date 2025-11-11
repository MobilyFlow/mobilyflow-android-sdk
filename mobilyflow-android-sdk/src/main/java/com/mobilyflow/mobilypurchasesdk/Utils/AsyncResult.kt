package com.mobilyflow.mobilypurchasesdk.Utils

import okhttp3.internal.notifyAll
import okhttp3.internal.wait

class AsyncResult<T> {
    private var _resultData: T? = null
    private var _isSet = false

    fun set(data: T) {
        synchronized(this) {
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

    fun data(): T {
        return this._resultData!!
    }

    fun dataOptional(): T? {
        return this._resultData
    }
}