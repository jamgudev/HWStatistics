package com.jamgu.hwstatistics.brightness

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.provider.Settings.System.SCREEN_BRIGHTNESS

/**
 * Created by jamgu on 2021/10/15
 */
object BrightnessManager {

    private const val TAG = "BrightnessManager"

    private const val DEFAULT_BRIGHTNESS_NOT_FOUND = -1
    internal const val SCREEN_ON = 1
    internal const val SCREEN_OFF = 0

    internal var SCREEN_ON_STATUS = SCREEN_ON
    private val mReceiver = ScreenReceiver()
    private var mBrightness = DEFAULT_BRIGHTNESS_NOT_FOUND

    private lateinit var mBrightnessObserver: ContentObserver

    fun registerReceiver(context: Context?) {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        intentFilter.addAction(Intent.ACTION_SCREEN_ON)

        mBrightnessObserver = object : ContentObserver(Handler(Looper.myLooper()!!)) {
            override fun onChange(selfChange: Boolean) {
                mBrightness = Settings.System.getInt(context?.contentResolver, SCREEN_BRIGHTNESS, DEFAULT_BRIGHTNESS_NOT_FOUND)
            }
        }

        context?.let {
            it.registerReceiver(mReceiver, intentFilter)
            it.contentResolver?.registerContentObserver(
                Settings.System.getUriFor(Settings.System.getUriFor(SCREEN_BRIGHTNESS).toString()),
                true,
                mBrightnessObserver
            )
        }

        mBrightness = Settings.System.getInt(context?.contentResolver, SCREEN_BRIGHTNESS, DEFAULT_BRIGHTNESS_NOT_FOUND)
    }

    fun unregisterReceiver(context: Context?) {
        context?.let {
            it.unregisterReceiver(mReceiver)
            it.contentResolver?.unregisterContentObserver(mBrightnessObserver)
        }
    }

    fun getScreenStatus(): Int {
        return SCREEN_ON_STATUS
    }

    fun getBrightness(context: Context?): Int {
        context ?: return DEFAULT_BRIGHTNESS_NOT_FOUND

        return try {
            Settings.System.getInt(context.contentResolver, SCREEN_BRIGHTNESS, DEFAULT_BRIGHTNESS_NOT_FOUND)
        } catch (e: Settings.SettingNotFoundException) {
            DEFAULT_BRIGHTNESS_NOT_FOUND
        }
    }
}

internal class ScreenReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (Intent.ACTION_SCREEN_OFF == intent?.action) {
            BrightnessManager.SCREEN_ON_STATUS = BrightnessManager.SCREEN_OFF
        } else if (Intent.ACTION_SCREEN_ON == intent?.action) {
            BrightnessManager.SCREEN_ON_STATUS = BrightnessManager.SCREEN_ON
        }
    }

}