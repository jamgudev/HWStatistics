package com.jamgu.hwstatistics

import android.app.Activity
import android.app.Application

/**
 * @author jamgudev
 * @date 2022/12/1 9:45 下午
 *
 * @description
 */
class BaseApplication: Application() {

    private val mRunningActivities = ArrayList<Class<out Activity>>()

    fun addThisActivityToRunningActivities(cls: Class<out Activity>) {
        if (!mRunningActivities.contains(cls)) mRunningActivities.add(cls)
    }

    fun removeThisActivityFromRunningActivities(cls: Class<out Activity>) {
        if (mRunningActivities.contains(cls)) mRunningActivities.remove(cls)
    }

    fun isActivityInBackStack(cls: Class<out Activity>): Boolean {
        return mRunningActivities.contains(cls)
    }

}