package com.jamgu.hwstatistics

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jamgu.common.page.activity.ViewBindingActivity
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.util.log.JLog
import com.jamgu.common.util.preference.PreferenceUtil
import com.jamgu.hwstatistics.appusage.AppUsageDataLoader
import com.jamgu.hwstatistics.appusage.AppUsageRecord
import com.jamgu.hwstatistics.appusage.timeStamp2SimpleDateString
import com.jamgu.hwstatistics.databinding.ActivityAutoMonitorBinding
import com.jamgu.hwstatistics.keeplive.service.KeepAliveService
import com.jamgu.krouter.annotation.KRouter
import com.jamgu.krouter.core.router.KRouterUriBuilder
import com.jamgu.krouter.core.router.KRouters

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

            override fun onSessionEnd(session: AppUsageRecord.SingleSessionRecord) {
            }
        })
    }
    private var mStartTime: Long? = null
    private var mAdapter: StatisticAdapter = StatisticAdapter()
    private var mData: ArrayList<String> = ArrayList()
    private var mShowTime: Boolean = false
    private var isStartFromNotification: Boolean = false

    override fun isBackPressedNeedConfirm(): Boolean {
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAppUsageDataLoader.onCreate()

        isStartFromNotification =
            intent.extras?.getBoolean(AUTO_MONITOR_START_FROM_NOTIFICATION) ?: false
        if (isStartFromNotification || mAppUsageDataLoader.isStarted()) {
            ThreadPool.runUITask({
                if (isStartFromNotification) {
                    mBinding.vStart.text = getString(R.string.already_started)
                    mBinding.vStart.isEnabled = false
                }
                if (!mAppUsageDataLoader.isStarted()) {
                    mAppUsageDataLoader.start()
                    mStartTime = System.currentTimeMillis()
                }
            }, 400)
        }
        JLog.d(TAG, "onCreate isStartFromBoot = $isStartFromNotification")

        // 加入任务栈
        (applicationContext as? BaseApplication)?.addThisActivityToRunningActivities(this.javaClass)

        // 保活前台服务
        KeepAliveService.start(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        JLog.d(TAG, "onDestroy")
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
            mData.add(duration.timeStamp2SimpleDateString())
            mAdapter.notifyItemInserted(mData.size - 1)
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