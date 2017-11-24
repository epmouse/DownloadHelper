package com.example.liu_xingxing.downloadhelp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    lateinit var handler: Handler
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        handler = Handler(Looper.getMainLooper())
        initDownload()
    }

    private fun initDownload() {
        val baseUrl = "http://10.16.15.77:8080/download/"
        tv_1.setOnClickListener { toDownload("${baseUrl}1.mp3", "1.mp3", tv_1) }
        tv_2.setOnClickListener { toDownload("${baseUrl}2.mp3", "2.mp3", tv_2) }
        tv_3.setOnClickListener { toDownload("${baseUrl}3.mp3", "3.mp3", tv_3) }
        tv_4.setOnClickListener { toDownload("${baseUrl}4.mp3", "4.mp3", tv_4) }
        tv_5.setOnClickListener { toDownload("${baseUrl}5.mp3", "5.mp3", tv_5) }
    }

    private fun toDownload(url: String, name: String, textView: TextView) {
        DownloadManager.getInstance(this.applicationContext).download(
                url,
                name,
                object : MyCallback {
                    override fun success(filePath: String) {
                        handler.post {
                            textView.text = "完成"
                        }
                    }
                    override fun fail(errCode: Int, errMsg: String) {
                    }

                    override fun progress(filePath: String, progress: Long) {
                        handler.post {
                            textView.text = "$progress"
                        }
                    }

                }
        )
    }
}
