package com.jamgu.hwstatistics

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.AbsListView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jamgu.common.page.activity.ViewBindingActivity
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.util.log.JLog
import com.jamgu.hwstatistics.IOnDataEnough.Companion.THRESH_HALF_HOUR
import com.jamgu.hwstatistics.databinding.ActivityAutoMonitorBinding
import com.jamgu.hwstatistics.keeplive.service.AliveStrategy
import com.jamgu.hwstatistics.keeplive.service.KeepAliveService
import com.jamgu.hwstatistics.keeplive.service.screen.ActiveBroadcastReceiver
import com.jamgu.hwstatistics.keeplive.utils.KeepLiveUtils
import com.jamgu.hwstatistics.upload.DataSaver
import com.jamgu.krouter.annotation.KRouter

@KRouter(value = [AUTO_MONITOR_PAGE], booleanParams = [AUTO_MONITOR_START_FROM_BOOT])
class AutoMonitorActivity : ViewBindingActivity<ActivityAutoMonitorBinding>() {

    companion object {
        private const val TAG = "AutoMonitorActivity"
    }

    private lateinit var mDataLoader: StatisticsLoader
    private var mAdapter: StatisticAdapter = StatisticAdapter()
    private var mData: ArrayList<String> = ArrayList()
    private var mShowTime: Boolean = false
    private var isStartFromBoot: Boolean = false

    private var activeBroadcastReceiver: ActiveBroadcastReceiver? = null

    override fun isBackPressedNeedConfirm(): Boolean {
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 注册开机、关机、解锁广播
        if (activeBroadcastReceiver == null) {
            activeBroadcastReceiver = ActiveBroadcastReceiver()
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        intentFilter.addAction(Intent.ACTION_USER_PRESENT)
        // 8.0 后，只能通过动态注册
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intentFilter.addAction(Intent.ACTION_BOOT_COMPLETED)
            intentFilter.addAction(Intent.ACTION_SHUTDOWN)
        }
        registerReceiver(activeBroadcastReceiver, intentFilter)

        isStartFromBoot = intent.extras?.getBoolean(AUTO_MONITOR_START_FROM_BOOT) ?: false
        if (isStartFromBoot) {
            ThreadPool.runUITask({
                if (isStartFromBoot) {
                    mBinding.vStart.text = "已经启动，请您将App退至后台"
                    mBinding.vStart.isEnabled = false
                }
                mDataLoader.startNonMainThread()
            }, 400)
        }
        JLog.d(TAG, "onCreate isStartFromBoot = $isStartFromBoot")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(activeBroadcastReceiver)
    }

    override fun initData() {
        super.initData()
        mDataLoader = StatisticsLoader()
        mDataLoader.setOnDataEnoughListener(THRESH_HALF_HOUR, object : IOnDataEnough {
            override fun onDataEnough() {
                saveData()
            }
        })
        mDataLoader.init(this) {
            if (mShowTime) {
                ThreadPool.runUITask {
                    mData.add(it)
                    mAdapter.notifyItemInserted(mData.size - 1)
                    if (mBinding.vRecycler.scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                        mBinding.vRecycler.scrollToPosition(mData.size - 1)
                    }
                }
            }
        }
    }

    private fun saveData() {
        val data = ArrayList(mDataLoader.getRawData())
        DataSaver.save(this@AutoMonitorActivity, data)
        mDataLoader.clearData()
        if (mShowTime) {
            ThreadPool.runUITask {
                mData.clear()
                mAdapter.notifyDataSetChanged()
            }
        }
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
            if (mDataLoader.requestedPermission(this)) {
                if (!mDataLoader.isStarted()) {
                    mData.clear()
                    mAdapter.notifyDataSetChanged()
                    mDataLoader.startNonMainThread()
                    mBinding.vStart.text = "Stop"
                } else {
                    mDataLoader.stop()
                    saveData()
                    mBinding.vStart.text = "Start"
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        KeepAliveService.start(this, AliveStrategy.ALL)
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        val endData = ArrayList<Any>().apply {
            add(0, "${System.currentTimeMillis()} BEING KILLED")
        }
        mDataLoader.getRawData().add(endData)
        saveData()
    }

    override fun getViewBinding(): ActivityAutoMonitorBinding = ActivityAutoMonitorBinding.inflate(layoutInflater)
}