package com.jamgu.hwstatistics.util

import java.util.ArrayList

/**
 * @author gujiaming.dev@bytedance.com
 * @date 2022/12/11 15:58
 *
 * @description
 */

/**
 * 传入newData，会将两者相加
 */
fun ArrayList<Any>.plus(newData: ArrayList<Any>?): ArrayList<Any> {
    if (newData.isNullOrEmpty() || this.isEmpty()) return this

    this.forEachIndexed { i, it ->
        if (i == 0) return@forEachIndexed
        if (it is Number) {
            if (it is Float) {
                val newVal = it.plus(newData[i].toString().toFloat())
                this[i] = newVal
            } else if (it is Int) {
                val newVal = it.plus(newData[i].toString().toInt())
                this[i] = newVal
            }
        }
    }

    return this
}

/**
 * 传入分母，会将列表内各元素分别处于它
 */
fun ArrayList<Any>.divideBy(divider: Float): ArrayList<Any> {
    if (this.isEmpty()) return this

    this.forEachIndexed { i, it ->
        if (i == 0) return@forEachIndexed

        if (it is Number) {
            if (it is Float) {
                val newVal = it / divider
                this[i] = newVal.roundToDecimals(2)
            } else if (it is Int) {
                val newVal = it / divider
                this[i] = newVal.roundToDecimals(2)
            }
        }
    }

    return this
}