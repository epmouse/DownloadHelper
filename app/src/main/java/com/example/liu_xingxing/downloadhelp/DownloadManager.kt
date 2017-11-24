package com.example.liu_xingxing.downloadhelp

import android.content.Context
import android.text.TextUtils
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.*
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Created by stars on 2017/11/22.
 */
class DownloadManager private constructor() {
    private lateinit var e: ExecutorService
    private val set = hashSetOf<String>()

    init {
        e = Executors.newFixedThreadPool(COREPOOL_SIZE)
    }

    companion object {
        val downloadCalls = Collections.synchronizedMap(mutableMapOf<String, Call>())//存储已经开始的下载任务，一是防止重复下载，二是用于取消下载任务
        @Volatile
        private var fileHome: String? = null
        @Volatile
        private var COREPOOL_SIZE: Int = 2

        fun getInstance(context: Context): DownloadManager {
            return getInstance(context, 2)
        }

        fun getInstance(context: Context, poolSize: Int): DownloadManager {
            return getInstance(context, poolSize, null)
        }

        fun getInstance(context: Context, saveFolder: String?): DownloadManager {
            return getInstance(context, 2, saveFolder)
        }

        fun getInstance(context: Context, poolSize: Int, saveFolder: String?): DownloadManager {
            COREPOOL_SIZE = poolSize
            setDownloadFolder(saveFolder, context)
            return Inner.instance
        }

        private fun setDownloadFolder(saveFolder: String?, context: Context) {
            //没有设置过filehome地址时，防止调用参数少的方法 把用户设置的路径置为默认值
            if (fileHome == null) {//保证存储路径只首次设置有效。
                fileHome = if (TextUtils.isEmpty(saveFolder)) context.getExternalFilesDir("downloadHome").absolutePath else saveFolder!!
            }
//            else{//已设置过则替换，保证存储地址在使用过程中可修改。
//                if(!TextUtils.isEmpty(saveFolder)) fileHome = saveFolder
//            }

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

    fun getFileCurrentLength(fileName: String): Long {
        val file = File(fileHome, fileName)
        return if (file.exists()) file.length() else 0

    }

    fun download(url: String, fileName: String, downloadCallback: MyCallback?) {

        HttpHelper.getInstance(url).getContentLength(object : Callback {
            override fun onFailure(call: Call?, e: IOException?) {
                downloadCallback?.fail(MyRunnable.NETWORK_ERR, "网络请求失败")
            }

            override fun onResponse(call: Call?, response: Response?) {
                if (response == null) {
                    downloadCallback?.fail(MyRunnable.NETWORK_ERR, "网络请求失败")
                    return
                }
                val contentLength = response.body()?.contentLength()
                if (contentLength == (-1).toLong()) {
                    downloadCallback?.fail(MyRunnable.CONTEENTLENGTH_ERR, "contentlength 无法获取")
                    return
                }
                val currentLength = getFileCurrentLength(fileName)
                if (currentLength < contentLength!!){
                    if (!downloadCalls.containsKey(url)||!File(fileHome,fileName).exists()) {//防止多次下载同一文件，另外保证文件被删除后可以重新下载。
                        e.execute(MyRunnable(url, fileName, currentLength, contentLength, downloadCallback))
                    }
                } else downloadCallback?.success("$fileHome/$fileName")
            }

        })

    }

    class MyRunnable(private val url: String, private val fileName: String, private var start: Long, private var end: Long, private val downloadCallback: MyCallback?) : Runnable {
        companion object {
            val NETWORK_ERR = 0
            val IO_GET_ERR = 1
            val IO_ERR = 2
            val CONTEENTLENGTH_ERR = 3
        }

        override fun run() {
            val arr = HttpHelper.getInstance(url).syncRequestByRange(start, end)
            downloadCalls.put(url, arr[0] as Call)
            toDownload(arr[1] as Response)
        }

        private fun toDownload(response: Response?) {
            if (response == null && downloadCallback != null) {
                downloadCallback.fail(NETWORK_ERR, "网络请求失败")
                return
            }
            val body = response!!.body()
            val file = getFileByName(fileName)
            val randomAccessFile = RandomAccessFile(file, "rwd")
            val buf = ByteArray(1024 * 80)
            var byteStream: InputStream? = null
            try {
                byteStream = body?.byteStream()
                if (byteStream == null) {
                    downloadCallback?.fail(IO_GET_ERR, "获取流失败")
                    return
                }
                var currentProgress: Long = start
                var len = byteStream.read(buf)
                randomAccessFile.seek(start)
                while (len != -1) {
                    randomAccessFile.write(buf, 0, len)
                    currentProgress += len
                    len = byteStream.read(buf)
                    downloadCallback?.progress(file.absolutePath, currentProgress)
                }
                downloadCallback?.success(file.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                byteStream?.close()
                randomAccessFile.close()
            }

        }

        private fun getFileByName(fileName: String): File {
            val file = File(fileHome, fileName)
            if (!file.exists()) {
                try {
                    file.createNewFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            return file
        }

    }


}

data class DownloadBean(var url: String, var totalLength: Long?,
                        var currentProgress: Long, var fileName: String,
                        var isComplete: Boolean)

