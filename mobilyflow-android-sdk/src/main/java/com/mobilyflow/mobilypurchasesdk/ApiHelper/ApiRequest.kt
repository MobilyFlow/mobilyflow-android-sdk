package com.mobilyflow.mobilypurchasesdk.ApiHelper

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

class ApiRequest(private val method: String, private val url: String) {
    private var data: JSONObject? = null
    private var params: MutableMap<String, Any>? = null
    private var headers = mutableMapOf<String, String>()
    private var files: MutableMap<String, File>? = null

    fun addHeader(key: String, value: String): ApiRequest {
        this.headers[key] = value
        return this
    }

    fun setParams(params: Map<String, Any>): ApiRequest {
        this.params = params.toMutableMap()
        return this
    }

    fun addParam(key: String, value: Any): ApiRequest {
        if (this.params == null) {
            this.params = mutableMapOf()
        }

        this.params!![key] = value
        return this
    }

    fun setData(data: JSONObject?): ApiRequest {
        this.data = data
        return this
    }

    fun addData(key: String, value: Any): ApiRequest {
        if (this.data == null) {
            this.data = JSONObject()
        }

        this.data!!.put(key, value)
        return this
    }

    fun addFile(key: String, value: File): ApiRequest {
        if (this.files == null) {
            this.files = mutableMapOf()
        }

        this.files!![key] = value
        return this
    }

    fun buildOkHttpRequest(baseURL: String): Request {
        val requestBuilder = Request.Builder()

        val url = if (this.url.startsWith('/')) this.url.substring(1) else this.url
        val urlBuilder = baseURL.toHttpUrl().newBuilder().addPathSegments(url)

        this.params?.forEach { (key, value) -> urlBuilder.addQueryParameter(key, value.toString()) }
        this.headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }

        val method = this.method.lowercase()

        if (method == "post" || method == "put" || method == "patch") {
            val body: RequestBody

            if (this.files != null) {
                val mediaType = "application/octet-stream".toMediaTypeOrNull()
                val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM);

                this.files!!.forEach { entry ->
                    bodyBuilder.addFormDataPart(entry.key, entry.value.name, entry.value.asRequestBody(mediaType))
                }

                if (this.data != null) {
                    this.data!!.keys().forEach { key ->
                        bodyBuilder.addFormDataPart(key, this.data!!.get(key).toString())
                    }
                }

                body = bodyBuilder.build()
            } else {
                body = (if (this.data == null) JSONObject() else this.data)
                    .toString().toRequestBody("application/json".toMediaTypeOrNull())
            }

            if (this.method.lowercase() == "post") {
                requestBuilder.post(body)
            } else if (this.method.lowercase() == "put") {
                requestBuilder.put(body)
            } else if (this.method.lowercase() == "patch") {
                requestBuilder.patch(body)
            }
        }

        requestBuilder.url(urlBuilder.build())

        return requestBuilder.build()
    }
}