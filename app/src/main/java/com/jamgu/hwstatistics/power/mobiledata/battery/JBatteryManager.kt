package com.jamgu.hwstatistics.power.mobiledata.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/**
 * @author gujiaming.dev@bytedance.com
 * @date 2023/5/15 19:42
 *
 * @description
 */
object JBatteryManager {
    private const val POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile";

    fun getBatteryStatus(context: Context?): Int {
        val batteryIntent = getBatteryIntent(context)
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        return status
    }

    fun isBatteryCharging(context: Context?): Int {
        return if (getBatteryStatus(context) == BatteryManager.BATTERY_STATUS_CHARGING) 1 else 0
    }

    fun isCharging(context: Context?): Boolean {
        val batteryIntent = getBatteryIntent(context)
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    fun getBatteryPercent(context: Context?): Float {
        context ?: return 0F
        // 获取电池当前电量
        return getBatteryLevel(context) * 100 / getBatteryScale(context).toFloat()
    }

    fun getBatteryLevel(context: Context?): Int {
        val batteryStatus: Intent? = getBatteryIntent(context)
        return batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
    }

    fun getBatteryScale(context: Context?): Int {
        val batteryStatus: Intent? = getBatteryIntent(context)
        return batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 0) ?: 0
    }

    fun getBatteryVoltage(context: Context?): Int {
        return getBatteryIntent(context)?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
    }

    fun getBatteryCurrent(context: Context?): Long {
        val batteryManager = context?.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

        return batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: -99
    }

    private fun getBatteryIntent(context: Context?): Intent? {
        context ?: return null
        return IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
    }

    fun getBatteryCapacity(context: Context?): Float {
        context ?: return 0f
        var batteryCapacity = 0f
        try {
            val mPowerProfile = Class.forName(POWER_PROFILE_CLASS).getConstructor(Context::class.java).newInstance(context)

            batteryCapacity = Class.forName(POWER_PROFILE_CLASS)
                .getMethod("getBatteryCapacity")
                .invoke(mPowerProfile) as? Float ?: 0f

        } catch (e: Exception) {
            e.printStackTrace();
        }
        return batteryCapacity
    }

}