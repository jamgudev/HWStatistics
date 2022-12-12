package com.jamgu.hwstatistics.power.mobiledata.phonestate

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.PhoneStateListener.LISTEN_NONE
import android.telephony.TelephonyManager

/**
 * Created by jamgu on 2021/10/15
 */
object PhoneStateManager {

    private val PHONE_STATE_NOT_FOUND = arrayOf(-1, -1)
    private var isPhoneStateListenerReg: Boolean = false

    private val listener = PSListener()

    @Suppress("DEPRECATION")
    fun register(context: Context?) {
        val manager = context?.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        manager?.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        isPhoneStateListenerReg = manager != null
    }

    @Suppress("DEPRECATION")
    fun unregister(context: Context?) {
        val manager = context?.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        manager?.listen(listener, LISTEN_NONE)
        isPhoneStateListenerReg = false
    }

    fun getPhoneState(): Array<Int> {
        return if (isPhoneStateListenerReg)
            arrayOf(PSListener.PHONE_STATE_RINGING, PSListener.PHONE_STATE_OFF_HOOK)
        else PHONE_STATE_NOT_FOUND
    }

}