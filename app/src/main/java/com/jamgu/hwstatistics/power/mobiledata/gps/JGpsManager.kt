package com.jamgu.hwstatistics.power.mobiledata.gps

import android.content.Context
import android.location.LocationManager

/**
 * @author gujiaming.dev@bytedance.com
 * @date 2023/5/16 16:12
 *
 * @description
 */
object JGpsManager {

    fun isGpsEnable(context: Context?): Int {
        context ?: return 0

        val service = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val isEnable = service?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                service?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

        return if (isEnable) 1 else 0
    }

}