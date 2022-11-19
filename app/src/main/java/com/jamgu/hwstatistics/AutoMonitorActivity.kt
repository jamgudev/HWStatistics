package com.jamgu.hwstatistics

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.AbsListView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jamgu.common.page.activity.ViewBindingActivity
import com.jamgu.common.thread.ThreadPool
import com.jamgu.hwstatistics.IOnDataEnough.Companion.THRESH_HALF_HOUR
import com.jamgu.hwstatistics.databinding.ActivityAutoMonitorBinding
import com.jamgu.hwstatistics.keeplive.service.AliveStrategy
import com.jamgu.hwstatistics.keeplive.service.KeepAliveService
import com.jamgu.hwstatistics.keeplive.utils.KeepLiveUtils
import com.jamgu.hwstatistics.upload.DataSaver

class AutoMonitorActivity : ViewBindingActivity<ActivityAutoMonitorBinding>() {

    private lateinit var mDataLoader: StatisticsLoader
    private var mAdapter: StatisticAdapter = StatisticAdapter()
    private var mData: ArrayList<String> = ArrayList()
    private var mShowTime: Boolean = false

    override fun isBackPressedNeedConfirm(): Boolean {
        return true
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