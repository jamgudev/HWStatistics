package com.jamgu.hwstatistics.page

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.os.Bundle
import androidx.core.content.getSystemService
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jamgu.common.page.activity.ViewBindingActivity
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.util.log.JLog
import com.jamgu.common.util.preference.PreferenceUtil
import com.jamgu.hwstatistics.BaseApplication
import com.jamgu.hwstatistics.R
import com.jamgu.hwstatistics.appusage.AppUsageDataLoader
import com.jamgu.hwstatistics.appusage.UsageRecord
import com.jamgu.hwstatistics.databinding.ActivityAutoMonitorBinding
import com.jamgu.hwstatistics.keeplive.service.KeepAliveService
import com.jamgu.hwstatistics.net.upload.DataSaver
import com.jamgu.hwstatistics.net.upload.DataUploader
import com.jamgu.hwstatistics.power.StatisticAdapter
import com.jamgu.hwstatistics.util.timeStamp2DateStringWithMills
import com.jamgu.hwstatistics.util.timeStamp2SimpleDateString
import com.jamgu.krouter.annotation.KRouter
import com.jamgu.krouter.core.router.KRouterUriBuilder
import com.jamgu.krouter.core.router.KRouters
import java.io.File


@KRouter(value = [AUTO_MONITOR_PAGE], booleanParams = [AUTO_MONITOR_START_FROM_NOTIFICATION])
class AutoMonitorActivity : ViewBindingActivity<ActivityAutoMonitorBinding>() {

    companion object {
        private const val TAG = "AutoMonitorActivity"
        private const val MONITOR_INIT = "monitor_init"
    }

    private val mAppUsageDataLoader: AppUsageDataLoader = AppUsageDataLoader(this).apply {
        setOnSessionListener(object : AppUsageDataLoader.IOnUserSessionListener {
            override fun onSessionBegin() {
            }

            override fun onSessionEnd(session: UsageRecord.SingleSessionRecord) {
            }
        })
    }
    private var mStartTime: Long? = null
    private var mAdapter: StatisticAdapter = StatisticAdapter()
    private var mData: ArrayList<String> = ArrayList()
    private var mShowTime: Boolean = false
    private var mKeepLiveServiceOpen: Boolean = false

    override fun isBackPressedNeedConfirm(): Boolean {
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAppUsageDataLoader.onCreate()

        val isStartFromNotification =
            intent.extras?.getBoolean(AUTO_MONITOR_START_FROM_NOTIFICATION) ?: false

        val startFromBoot =
            intent.extras?.getBoolean(AUTO_MONITOR_START_FROM_BOOT) ?: false

        val startFromKilled =
            intent.extras?.getBoolean(AUTO_MONITOR_START_FROM_KILLED) ?: false

        val startFromInit =
            intent.extras?.getBoolean(AUTO_MONITOR_START_FROM_INIT) ?: false

        val enableStart = !startFromInit || startFromKilled || startFromBoot || isStartFromNotification

        if (enableStart || mAppUsageDataLoader.isStarted()) {
            ThreadPool.runUITask({
                if (enableStart) {
                    mBinding.vStart.text = getString(R.string.already_started)
                    mBinding.vStart.isEnabled = false
                }
                if (!mAppUsageDataLoader.isStarted()) {
                    mAppUsageDataLoader.start()
                    mStartTime = System.currentTimeMillis()
                }
            }, 400)
        }
        JLog.d(TAG, "onCreate isStartFromBoot = $isStartFromNotification, isStarted = ${mAppUsageDataLoader.isStarted()}")

        DataSaver.addDebugTracker(this, "onCreate startFromNotif = $isStartFromNotification, " +
                "startFromInit = $startFromInit, " +
                "startFromKilled = $startFromKilled, " +
                "startFromBoot = $startFromBoot, " +
                "isStarted = ${mAppUsageDataLoader.isStarted()}")

        // 加入任务栈
        (applicationContext as? BaseApplication)?.addThisActivityToRunningActivities(this.javaClass)

    }

    override fun onDestroy() {
        super.onDestroy()
        JLog.d(TAG, "onDestroy")
        val activityManager = getSystemService<ActivityManager>()
        val memState = activityManager?.runningAppProcesses?.first()?.let {
             ActivityManager.getMyMemoryState(it)
        } ?: 0
        DataSaver.addInfoTracker(this, "$TAG onDestroy called, memState = $memState")
        DataSaver.flushTestData(this)
        mAppUsageDataLoader.onDestroy()

        (applicationContext as? BaseApplication)?.removeThisActivityFromRunningActivities(this.javaClass)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun initWidget() {
        super.initWidget()

        mAdapter.setData(mData)
        mBinding.vRecycler.adapter = mAdapter
        mBinding.vRecycler.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))
        mBinding.vRecycler.layoutManager = LinearLayoutManager(this)

        mBinding.vShowTime.setOnClickListener {
            mShowTime = !mShowTime
            mBinding.vShowTime.text = if (mShowTime) "不显示时间戳" else "显示时间戳"
        }
        mBinding.vStart.setOnClickListener {
            if (!mAppUsageDataLoader.isStarted()) {
                mData.clear()
                mAdapter.notifyDataSetChanged()
                mAppUsageDataLoader.start()
                mStartTime = System.currentTimeMillis()
                mBinding.vStart.text = getString(R.string.already_started)
                mBinding.vStart.isEnabled = false
            }
        }
    }

    /**
     * OnLowMemory是Android提供的API，在系统内存不足，
     * 所有后台程序（优先级为background的进程，不是指后台运行的进程）都被杀死时，系统会调用OnLowMemory。
     */
    override fun onLowMemory() {
        super.onLowMemory()
        JLog.d(TAG, "onLowMemory")
        DataSaver.addDebugTracker(this, "$TAG onLowMemory called.")
    }

    /**
     *
    TRIM_MEMORY_COMPLETE：内存不足，并且该进程在后台进程列表最后一个，马上就要被清理
    TRIM_MEMORY_MODERATE：内存不足，并且该进程在后台进程列表的中部。
    TRIM_MEMORY_BACKGROUND：内存不足，并且该进程是后台进程。
    TRIM_MEMORY_UI_HIDDEN：内存不足，并且该进程的UI已经不可见了。
    以上4个是4.0增加
    TRIM_MEMORY_RUNNING_CRITICAL：内存不足(后台进程不足3个)，这个回调的下一个阶段就是 onLowMemory
    TRIM_MEMORY_RUNNING_LOW：内存不足(后台进程不足5个)，并且该进程优先级比较高，需要清理内存
    TRIM_MEMORY_RUNNING_MODERATE：内存不足(后台进程超过5个)，并且该进程优先级比较高，需要清理内存
    以上3个是4.1增加

    通过一键清理后，OnLowMemory不会被触发，而OnTrimMemory会被触发一次(有些手机如一加，不会触发), level = 80
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        mAppUsageDataLoader.onTrimMemory(level)
    }

    override fun onResume() {
        super.onResume()
        val preference = PreferenceUtil.getCachePreference(this, 0)
        val isInit = preference.getBoolean(MONITOR_INIT, false)
        if (!isInit) {
            ThreadPool.runUITask({
                KRouters.open(this, KRouterUriBuilder().appendAuthority(INIT_PAGE).build())
            }, 400)
            preference.edit().putBoolean(MONITOR_INIT, true).apply()
        }

        mStartTime?.let { startTime ->
            val duration = System.currentTimeMillis() - startTime
            mData.add("Session Duration:" + duration.timeStamp2SimpleDateString())
            mAdapter.notifyItemInserted(mData.size - 1)
        }

        // 保活前台服务
        if (!mKeepLiveServiceOpen && isInit) {
            mKeepLiveServiceOpen = true
            KeepAliveService.start(this)
        }
    }

    /**
     * 当用户按下Home键 app处于后台，此时会调用onSaveInstanceState 方法
    当用户按下电源键时，会调用onSaveInstanceState 方法
    当Activity进行横竖屏切换的时候也会调用onSaveInstanceState 方法
    从BActivity跳转到CActivity的时候 BActivity也会调用onSaveInstanceState 方法
     */
//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//        JLog.d(TAG, "onSaveInstanceState")
//        saveData()
//        // 打开拉起通知
//        KeepLiveUtils.startCallActivityVersionHigh(
//            this,
//            R.string.app_being_killed_reboot, AutoMonitorActivity::class.java
//        )
//    }

    override fun getViewBinding(): ActivityAutoMonitorBinding =
        ActivityAutoMonitorBinding.inflate(layoutInflater)
}