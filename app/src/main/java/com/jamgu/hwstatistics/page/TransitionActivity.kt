package com.jamgu.hwstatistics.page

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jamgu.common.thread.ThreadPool
import com.jamgu.common.util.log.JLog
import com.jamgu.hwstatistics.R
import com.jamgu.hwstatistics.net.upload.DataSaver
import com.jamgu.krouter.annotation.KRouter

/**
 * @author jamgudev
 * @date 2022/11/29 10:16 下午
 *
 * @description 用于启动AutoMonitorActivity的过渡的activity
 */
@KRouter(value = [TRANSITION_PAGE])
class TransitionActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TransitionActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_one_pixel)

//        val window = window ?: return
//        window.setGravity(Gravity.START or Gravity.TOP)
//        window.attributes?.apply {
//            this.x = 0
//            this.y = 0
//            this.width = 1
//            this.height = 1
//        }

        ThreadPool.runUITask({
            val intent = Intent(this, AutoMonitorActivity::class.java)
            getIntent()?.extras?.keySet()?.forEach { key ->
                intent.putExtra(key, getIntent().getBooleanExtra(key, false))
            }
            this@TransitionActivity.startActivity(intent)
            finish()
        }, 500)

        JLog.d(TAG, "onCreate")
        DataSaver.addDebugTracker(TAG, "onCreate")
    }

    override fun onDestroy() {
        super.onDestroy()
        JLog.d(TAG, "onDestroy")
    }
}