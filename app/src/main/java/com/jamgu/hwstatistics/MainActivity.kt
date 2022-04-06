package com.jamgu.hwstatistics

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.jamgu.common.page.activity.ViewBindingActivity
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.util.log.JLog
import com.jamgu.hwstatistics.databinding.ActivityMainBinding
import com.jamgu.hwstatistics.util.ExcelUtil

class MainActivity : ViewBindingActivity<ActivityMainBinding>() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private var mShowTime: Boolean = false
    private val mLoader = StatisticsLoader()

    private var mAdapter: StatisticAdapter = StatisticAdapter()
    private var mData: ArrayList<String> = ArrayList()

    private var folderLauncher: ActivityResultLauncher<Intent>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        JLog.d(TAG, "onCreate")
    }

    override fun initWidget() {
        initViews()
        
        mLoader.init(this) {
            if (mShowTime) {
                ThreadPool.runUITask {
                    mData.add(it)
                    mAdapter.notifyItemInserted(mData.size - 1)
                    if (mBinding.vRecycler.scrollState == SCROLL_STATE_IDLE) {
                        mBinding.vRecycler.scrollToPosition(mData.size - 1)
                    }
                }
            }
        }

        folderLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val uri: Uri = it.data?.data ?: return@registerForActivityResult
            JLog.i(TAG, "onActivityResult: " + "filePath：" + uri.path)
            // you can modify readExcelList, then write to excel.
            ThreadPool.runOnNonUIThread {
                val data = mLoader.getData()
                if (data.isNullOrEmpty()) {
                    showToast("Data Is Empty...")
                } else {
                    ExcelUtil.writeExcelNew(this, data, uri)
                }
            }
        }
    }

    private fun initViews() {
        mBinding.vStart.setOnClickListener {
            if (mLoader.requestedPermission(this)) {
                if (!mLoader.isStarted()) {
                    mData.clear()
                    mAdapter.notifyDataSetChanged()
                    mLoader.startNonMainThread()
                    mBinding.vStart.text = "Stop"
                } else {
                    mLoader.stop()
                    mBinding.vStart.text = "Start"

                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                    intent.type = "application/*"
                    intent.putExtra(Intent.EXTRA_TITLE, System.currentTimeMillis().toString() + ".xlsx")

                    folderLauncher?.launch(intent)
                }
            }
        }
        mAdapter.setData(mData)
        mBinding.vRecycler.adapter = mAdapter
        mBinding.vRecycler.addItemDecoration(DividerItemDecoration(this, VERTICAL))
        mBinding.vRecycler.layoutManager = LinearLayoutManager(this)

        mBinding.vTest.setOnClickListener {
            PCRatioExporter.verifyAndExport(this)
        }

        mBinding.vShowTime.setOnClickListener {
            mShowTime = !mShowTime
            mBinding.vShowTime.text = if (mShowTime) "不显示时间戳" else "显示时间戳"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        JLog.d(TAG, "onDestroy")
        mLoader.release()
    }

    override fun getViewBinding(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)

}
