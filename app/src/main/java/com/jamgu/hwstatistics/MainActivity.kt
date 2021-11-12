package com.jamgu.hwstatistics

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.jamgu.hwstatistics.databinding.ActivityMainBinding
import com.jamgu.hwstatistics.thread.ThreadPool
import com.jamgu.hwstatistics.util.ExcelUtil

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val mLoader = StatisticsLoader()
    private lateinit var binding: ActivityMainBinding

    private var mAdapter: StatisticAdapter = StatisticAdapter()
    private var mData: ArrayList<String> = ArrayList()

    private var folderLauncher: ActivityResultLauncher<Intent>? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "onCreate")

        initViews()

        mLoader.init(this) {
            ThreadPool.runUITask {
                mData.add(it)
                mAdapter.notifyItemInserted(mData.size - 1)
                if (binding.vRecycler.scrollState == SCROLL_STATE_IDLE) {
                    binding.vRecycler.scrollToPosition(mData.size - 1)
                }
            }
        }

        folderLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val uri: Uri = it.data?.data ?: return@registerForActivityResult
            Log.i(TAG, "onActivityResult: " + "filePath：" + uri.path)
            //you can modify readExcelList, then write to excel.
            ThreadPool.runOnNonUIThread {
                val data = mLoader.getData()
                if (data.isNullOrEmpty()) {
                    Toast.makeText(this, "Data Is Empty...", Toast.LENGTH_SHORT).show()
                } else {
                    ExcelUtil.writeExcelNew(this, data, uri)
                }
            }
        }
    }

    private fun initViews() {
        binding.vStart.setOnClickListener {
            if (mLoader.requestedPermission(this)) {
                if (!mLoader.isStarted()) {
//                    val intent = Intent("com.jamgu.outside")
//                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                    startActivity(intent)
                    mLoader.startNonMainThread()
                    binding.vStart.text = "Stop"
                } else {
                    mLoader.stop()
                    binding.vStart.text = "Start"

                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                    intent.type = "application/*"
                    intent.putExtra(Intent.EXTRA_TITLE, System.currentTimeMillis().toString() + ".xlsx")

                    folderLauncher?.launch(intent)
                }
            }
        }
        mAdapter.setData(mData)
        binding.vRecycler.adapter = mAdapter
        binding.vRecycler.addItemDecoration(DividerItemDecoration(this, VERTICAL))
        binding.vRecycler.layoutManager = LinearLayoutManager(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "onSaveInstanceState called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        mLoader.release()
    }

}
