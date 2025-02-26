package com.mobilyflow.mobilypurchasesdk.ApiHelper

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class ApiHelper(val baseURL: String, val defaultHeaders: Map<String, String>?) {
    val API: OkHttpClient

    init {
        this.API = OkHttpClient.Builder().callTimeout(30L, TimeUnit.SECONDS).build()
    }

    fun request(req: ApiRequest): ApiResponse {
        this.defaultHeaders?.forEach { (key, value) -> req.addHeader(key, value) }

        return this.API.newCall(req.buildOkHttpRequest(baseURL)).execute()
            .use { res -> ApiResponse(res.code, res.body!!.string()) }
    }
}