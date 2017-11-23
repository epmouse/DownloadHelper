package com.example.liu_xingxing.downloadhelp

import android.content.Context
import android.text.TextUtils
import okhttp3.Call
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Created by stars on 2017/11/22.
 */
class DownloadManager private constructor() {
    private lateinit var e: ExecutorService
    private val downloadCalls = hashMapOf<String, Call>()//存储已经开始的下载任务，一是防止重复下载，二是用于取消下载任务

    init {
        e = Executors.newFixedThreadPool(COREPOOL_SIZE)
    }

    companion object {
        private lateinit var fileHome: String
        private var COREPOOL_SIZE: Int = 2
        fun getInstance(context: Context): DownloadManager {
            return getInstance(context, 2)
        }


        fun getInstance(context: Context, poolSize: Int): DownloadManager {
            return getInstance(context, 2, null)
        }

        fun getInstance(context: Context, saveFolder: String?): DownloadManager {
            return getInstance(context, 2, saveFolder)
        }

        fun getInstance(context: Context, poolSize: Int, saveFolder: String?): DownloadManager {
            COREPOOL_SIZE = poolSize
            setDownloadFolder(saveFolder,context)
            return Inner.instance
        }

        private fun setDownloadFolder(saveFolder: String?, context: Context) {
            fileHome = if (TextUtils.isEmpty(saveFolder)) context.getExternalFilesDir("downloadHome").absolutePath else saveFolder!!
        }

    }

    private object Inner {
        val instance = DownloadManager()
    }
    /**
     * 取消对应的下载任务，并从map中删除
     */
    fun cancelDownloadTask(url: String) {
        var call = downloadCalls[url]
        if (call != null) {
            call.cancel()
        }
        downloadCalls.remove(url)
    }

    fun download(downloadBean: DownloadBean,
                 success: (filePath: String) -> Unit,
                 progress: (path: String, total: Long, i: Long) -> Unit,
                 start: (totalLength: Long) -> Unit,
                 fail: (err: String) -> Unit) {
        e.execute {
            HttpHelper.getInstance(downloadBean.url).downLoadFileProgress(downloadBean, success, progress, start, fail)
        }
    }

    fun createDownloadBean(url: String, fileName: String): DownloadBean {
        return DownloadBean(url, HttpHelper.getInstance(url).getContentLength(),
                0, fileName, false)
    }
}

data class DownloadBean(var url: String, var totalLength: Long?,
                        var currentProgress: Long, var fileName: String,
                        var isComplete: Boolean)