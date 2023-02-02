package com.jamgu.hwstatistics.net

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
    private var data: JSONObject? = null

    fun getMsg(): String? = msg

    fun getCode(): Int = code

    fun getData(): JSONObject? = data
}