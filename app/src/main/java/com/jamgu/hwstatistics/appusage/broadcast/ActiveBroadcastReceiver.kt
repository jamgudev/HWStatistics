package com.jamgu.hwstatistics.appusage.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.jamgu.common.util.log.JLog
import com.jamgu.hwstatistics.R
import com.jamgu.hwstatistics.keeplive.utils.KeepLiveUtils
import com.jamgu.hwstatistics.page.TRANSITION_PAGE
import com.jamgu.hwstatistics.page.TransitionActivity
import com.jamgu.krouter.core.router.KRouterUriBuilder
import com.jamgu.krouter.core.router.KRouters

/**
 * @author gujiaming.dev@bytedance.com
 * @date 2022/11/20 8:45 下午
 *
 * @description 手机开屏、息屏、开机、关机广播接收器
 */
class ActiveBroadcastReceiver @JvmOverloads constructor(private val listener: IOnScreenStateChanged? = null) : BroadcastReceiver() {

    /**
     * 关机重启时获取开机通知，系统会调用该无参构造方法
     */
//    constructor(): this(null)

    companion object {
        private const val TAG = "ActiveBroadcastReceiver"
    }

    fun registerReceiver(context: Context) {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        intentFilter.addAction(Intent.ACTION_USER_PRESENT)
        // 8.0 后，只能通过动态注册
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intentFilter.addAction(Intent.ACTION_BOOT_COMPLETED)
            intentFilter.addAction(Intent.ACTION_SHUTDOWN)
        }
        context.registerReceiver(this, intentFilter)
    }

    fun unRegisterReceiver(context: Context) {
        context.unregisterReceiver(this)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                context?.let {
                    listener?.onPhoneBootComplete()
                    JLog.d(TAG, "ACTION_BOOT_COMPLETED")
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
                JLog.d(TAG, "ACTION_SHUTDOWN")
                listener?.onPhoneShutdown()
            }
            Intent.ACTION_SCREEN_ON -> {
                listener?.onScreenOn()
            }
            Intent.ACTION_SCREEN_OFF -> {
                listener?.onScreenOff()
            }
            Intent.ACTION_USER_PRESENT -> {
                listener?.onUserPresent()
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