package com.jamgu.hwstatistics.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import android.os.PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED
import android.util.Log
import androidx.annotation.RequiresApi
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Created by jamgu on 2021/10/15
 */
object SystemManager {

    private const val TAG = "SystemManager"

    internal const val SYSTEM_ON = 1
    internal const val SYSTEM_OFF = 0

    internal var SYSTEM_LONG_TERM_IDLE_ON = -1
    private var mReceiver: BroadcastReceiver = SystemIdleReceiver()

    @RequiresApi(Build.VERSION_CODES.M)
    fun registerSystemReceiver(context: Context?) {
        val intentFilter = IntentFilter()
        Log.d("SystemIdleReceiver", "register")
        intentFilter.addAction(ACTION_DEVICE_IDLE_MODE_CHANGED)
        intentFilter.addAction("android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED")
        context?.registerReceiver(mReceiver, intentFilter)
    }

    fun unregisterSystemReceiver(context: Context?) {
        context?.unregisterReceiver(mReceiver)
    }

    fun getSystemOnStatus(context: Context?): Int {
        context ?: return SYSTEM_LONG_TERM_IDLE_ON
        val isLightIdleMode = isLightDeviceIdleMode(context)
        return when {
            isLightIdleMode == true -> {
                SYSTEM_OFF
            }
            SYSTEM_LONG_TERM_IDLE_ON >= 0 -> {
                SYSTEM_LONG_TERM_IDLE_ON
            }
            else -> SYSTEM_ON
        }
    }

    private fun isLightDeviceIdleMode(context: Context): Boolean? {
        var result: Boolean? = false
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        try {
            val isLightDeviceIdleModeMethod: Method = pm.javaClass.getDeclaredMethod("isLightDeviceIdleMode")
            result = isLightDeviceIdleModeMethod.invoke(pm) as? Boolean
//            Log.d("jamgu", "result = $result")
        } catch (e: IllegalAccessException) {
            Log.e(TAG, "Reflection failed for isLightDeviceIdleMode: $e", e)
        } catch (e: InvocationTargetException) {
            Log.e(TAG, "Reflection failed for isLightDeviceIdleMode: $e", e)
        } catch (e: NoSuchMethodException) {
            Log.e(TAG, "Reflection failed for isLightDeviceIdleMode: $e", e)
        }
        return result
    }

}

class SystemIdleReceiver: BroadcastReceiver() {
    // request api >= android 7.0
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceive(context: Context?, intent: Intent?) {
        val pm = context?.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        Log.d("SystemIdleReceiver", "onReceive: status = ${pm.isDeviceIdleMode}")
        SystemManager.SYSTEM_LONG_TERM_IDLE_ON = if (pm.isDeviceIdleMode) {
            SystemManager.SYSTEM_OFF
        } else SystemManager.SYSTEM_ON
    }

}
