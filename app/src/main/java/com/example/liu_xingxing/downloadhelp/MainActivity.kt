package com.example.liu_xingxing.downloadhelp

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.security.KeyStore

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        todownload()
    }

    private fun todownload() {
        DownloadManager.COREPOOL_SIZE = 3
        btn.setOnClickListener {
            for(i in 1 until 6 ){

                DownloadManager.download("http://10.16.15.77:8080/download/$i", "$i", this.externalCacheDir.absolutePath,
                        { filePath ->
                            Log.e("download", filePath)
                        },
                        {path, total, progress ->
                                updateProgress(path,total,progress)
                            Log.e("download", "$progress")
                        },
                        { totalLength ->
                            Log.e("download", "$totalLength")
                        },
                        { err ->
                            Log.e("download", err)
                        }
                )

            }

        }

    }

    @SuppressLint("SetTextI18n")
    private fun updateProgress(path: String, total: Long, progress: Long)=with(path) {
        Handler(Looper.getMainLooper()).post {
            when(true){
                endsWith("1")-> tv_1.text="$progress/$total"
                endsWith("2")-> tv_2.text="$progress/$total"
                endsWith("3")-> tv_3.text="$progress/$total"
                endsWith("4")-> tv_4.text="$progress/$total"
                endsWith("5")-> tv_5.text="$progress/$total"
            }
        }
    }
}
