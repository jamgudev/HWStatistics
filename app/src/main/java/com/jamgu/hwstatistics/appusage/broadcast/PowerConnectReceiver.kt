package com.jamgu.hwstatistics.appusage.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.TextUtils


/**
 * @author gujiaming.dev@bytedance.com
 * @date 2022/11/14 7:52 下午
 *
 * @description 监听手机充电状态
 */
class PowerConnectReceiver @JvmOverloads constructor(private val listener: IOnPhoneChargeStateChanged? = null): BroadcastReceiver() {

    fun registerReceiver(context: Context) {
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        context.registerReceiver(this, intentFilter)
    }

    fun unRegisterReceiver(context: Context) {
        context.unregisterReceiver(this)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        intent ?: return
        val action = intent.action
        // 获取电池当前电量
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context?.registerReceiver(null, ifilter)
        }
        val batteryPct: Float = batteryStatus?.let { batteryIntent ->
            val level: Int = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        } ?: 0F
        if (TextUtils.equals(action, Intent.ACTION_POWER_CONNECTED)) {
            // 充电
            listener?.onChargeState(batteryPct)
        } else if (TextUtils.equals(action, Intent.ACTION_POWER_DISCONNECTED)) {
            // 未充电
            listener?.onCancelChargeState(batteryPct)
        }
    }

    interface IOnPhoneChargeStateChanged {
        fun onChargeState(curBatteryState: Float)
        fun onCancelChargeState(curBatteryState: Float)
    }
}