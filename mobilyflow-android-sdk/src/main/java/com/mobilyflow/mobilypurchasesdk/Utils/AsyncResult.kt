package com.mobilyflow.mobilypurchasesdk.Utils

class AsyncResult<T> {
    private val lock = Object()

    private var _resultData: T? = null
    private var _isSet = false

    fun set(data: T) {
        synchronized(this.lock) {
            this._resultData = data
            this._isSet = true
            this.lock.notifyAll()
        }
    }

    fun waitResult() {
        synchronized(this.lock) {
            if (!this._isSet) {
                this.lock.wait()
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