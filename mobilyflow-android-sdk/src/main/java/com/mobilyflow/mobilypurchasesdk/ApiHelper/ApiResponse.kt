package com.mobilyflow.mobilypurchasesdk.ApiHelper

import com.mobilyflow.mobilypurchasesdk.Exceptions.MobilyException
import org.json.JSONArray
import org.json.JSONObject

class ApiResponse(val status: Int, val data: String) {

    val success: Boolean get() = (status >= 200) && (status < 300)

    fun json(): JSONObject {
        try {
            return JSONObject(this.data)
        } catch (_: Exception) {
            throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
        }
    }

    fun jsonArray(): JSONArray {
        try {
            return JSONArray(this.data)
        } catch (_: Exception) {
            throw MobilyException(MobilyException.Type.UNKNOWN_ERROR)
        }
    }

    fun string(): String {
        return this.data
    }
}