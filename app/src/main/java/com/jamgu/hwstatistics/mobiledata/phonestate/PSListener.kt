package com.jamgu.hwstatistics.mobiledata.phonestate

import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager

/**
 * Created by jamgu on 2021/10/15
 */
@Suppress("DEPRECATION")
class PSListener: PhoneStateListener(){

    companion object {
        var PHONE_STATE_RINGING = 0
        var PHONE_STATE_OFF_HOOK = 0
    }

    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
        when(state) {
            TelephonyManager.CALL_STATE_IDLE -> {
                PHONE_STATE_OFF_HOOK = 0
                PHONE_STATE_RINGING = 0
            }
            // 正在接听
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                PHONE_STATE_RINGING = 0
                PHONE_STATE_OFF_HOOK = 1
            }
            // 正在响铃
            TelephonyManager.CALL_STATE_RINGING -> {
                PHONE_STATE_RINGING = 1
                PHONE_STATE_OFF_HOOK = 0
            }
            else -> {}
        }
        super.onCallStateChanged(state, phoneNumber)
    }

}