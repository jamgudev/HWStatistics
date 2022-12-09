package com.jamgu.hwstatistics

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
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
import com.jamgu.hwstatistics.keeplive.service.AliveStrategy
import com.jamgu.hwstatistics.keeplive.service.KeepAliveService
import com.jamgu.hwstatistics.util.ExcelUtil
import com.jamgu.krouter.core.router.KRouters

class MainActivity : ViewBindingActivity<ActivityMainBinding>() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private var mShowTime: Boolean = false
    private val mLoader = StatisticsLoader(this)

    private var mAdapter: StatisticAdapter = StatisticAdapter()
    private var mData: ArrayList<String> = ArrayList()

    private var folderLauncher: ActivityResultLauncher<Intent>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        JLog.d(TAG, "onCreate")
    }

    override fun initWidget() {
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

        mBinding.v2PartPower.setOnClickListener {
//            PCRatioExporter.verifyAndExport(this)
            KRouters.open(this, PART_POWER_PAGE)
        }

        mBinding.vShowTime.setOnClickListener {
            mShowTime = !mShowTime
            mBinding.vShowTime.text = if (mShowTime) "不显示时间戳" else "显示时间戳"
        }
    }

    override fun initData() {
        mLoader.init {
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
                val data = mLoader.getDataWithTitle()
                if (data.isNullOrEmpty()) {
                    showToast("Data Is Empty...")
                } else {
                    ExcelUtil.writeExcelNew(this, data, uri)
                }
            }
        }

    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onResume() {
        super.onResume()
        // 保活程序
        KeepAliveService.start(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        JLog.d(TAG, "onDestroy")
        mLoader.release()
    }

    override fun getViewBinding(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)

}
