package com.jamgu.hwstatistics.keeplive.service.screen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.material.progressindicator.LinearProgressIndicatorSpec
import com.jamgu.common.util.log.JLog
import com.jamgu.hwstatistics.*
import com.jamgu.hwstatistics.keeplive.utils.KeepLiveUtils
import com.jamgu.krouter.core.router.KRouterUriBuilder
import com.jamgu.krouter.core.router.KRouters

/**
 * @author gujiaming.dev@bytedance.com
 * @date 2022/11/20 8:45 下午
 *
 * @description 手机开屏、息屏、开机、关机广播接收器
 */
class ActiveBroadcastReceiver(val listener: IOnScreenStateChanged) : BroadcastReceiver() {

    companion object {
        private const val TAG = "ActiveBroadcastReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                context?.let {
                    listener.onPhoneBootComplete()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        KeepLiveUtils.startCallActivityVersionHigh(
                            context,
                            R.string.click_to_start, TransitionActivity::class.java
                        )
                    } else {
                        startActivityVersionLow(context)
                    }
                }
            }
            Intent.ACTION_SHUTDOWN -> {
                listener.onPhoneShutdown()
            }
            Intent.ACTION_SCREEN_ON -> {
                listener.onScreenOn()
            }
            Intent.ACTION_SCREEN_OFF -> {
                listener.onScreenOff()
            }
            Intent.ACTION_USER_PRESENT -> {
                listener.onUserPresent()
            }
        }
    }

    /**
     * 8.0 以下实现
     */
    private fun startActivityVersionLow(context: Context) {
        val params = KRouterUriBuilder(TRANSITION_PAGE)
            .build()
        KRouters.open(context, params)
    }

    open class SimpleStateChanged: IOnScreenStateChanged {
        override fun onPhoneBootComplete() {
        }

        override fun onPhoneShutdown() {
        }

        override fun onScreenOn() {
        }

        override fun onScreenOff() {
        }

        override fun onUserPresent() {
        }

    }

    interface IOnScreenStateChanged {

        fun onPhoneBootComplete()

        fun onPhoneShutdown()

        fun onScreenOn()

        fun onScreenOff()

        fun onUserPresent()

    }

}