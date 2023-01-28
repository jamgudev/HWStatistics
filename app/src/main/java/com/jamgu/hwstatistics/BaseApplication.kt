package com.jamgu.hwstatistics

import android.app.Activity
import android.app.Application
import com.jamgu.common.Common
import com.jamgu.hwstatistics.upload.DataSaver

/**
 * @author jamgudev
 * @date 2022/12/1 9:45 下午
 *
 * @description
 */
class BaseApplication: Application() {

    companion object {
        private const val TAG = "BaseApplication"
    }

    private val mRunningActivities = ArrayList<Class<out Activity>>()

    override fun onCreate() {
        super.onCreate()

        Common.getInstance().init(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        DataSaver.addTestTracker(this, "$TAG, onTrimMemory level = $level")
    }

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