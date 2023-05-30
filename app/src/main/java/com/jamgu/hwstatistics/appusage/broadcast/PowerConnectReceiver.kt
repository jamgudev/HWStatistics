package com.jamgu.hwstatistics.appusage.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.TextUtils
import com.jamgu.hwstatistics.power.mobiledata.battery.JBatteryManager


/**
 * @author jamgudev
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
        val batteryPct: Float = JBatteryManager.getBatteryPercent(context)
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