package com.jamgu.hwstatistics.upload

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.TextUtils


/**
 * @author gujiaming.dev@bytedance.com
 * @date 2022/11/14 7:52 下午
 *
 * @description 监听手机充电状态
 */
class PowerConnectReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        intent ?: return
        val action = intent.action
        if (TextUtils.equals(action, Intent.ACTION_POWER_CONNECTED)) {
            // 充电
        } else if (TextUtils.equals(action, Intent.ACTION_POWER_DISCONNECTED)) {
            // 未充电
        }
    }
}