package com.jamgu.hwstatistics.net

import com.google.gson.annotations.SerializedName
import org.json.JSONObject

/**
 * @author jamgudev
 * @date 2023/2/1 10:08 上午
 *
 * @description
 */
class RspModel {
    private var msg: String? = null
    private var code: Int = -1
    private var data: RspData? = null

    fun getMsg(): String? = msg

    fun getCode(): Int = code

    fun getData(): RspData? = data

    class RspData {
        @SerializedName("pa_threshold")
        private var paThreshold: Long? = null

        fun getPaThreshold(): Long? = paThreshold

    }

}