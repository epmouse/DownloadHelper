package com.example.liu_xingxing.downloadhelp

import android.content.Context
import android.text.TextUtils
import android.util.Log
import greendao.DownloadEntityDao
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.*
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by stars on 2017/11/22.
 */
class DownloadManager private constructor() {

    private val scheduleInterval = Executors.newScheduledThreadPool(2)
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

    fun getFileCurrentLength(fileName: String): Long {
        val file = File(fileHome, fileName)
        return if (file.exists()) file.length() else 0
    }

    fun download(url: String, fileName: String?, downloadCallback: MyCallback?) {
        //todo-需要考虑两种情况，1、数据库有，但是文件被删除了（重新从零下载）；2、数据库没有，但是本地存在(目前此情况直接删除本地文件，重新下载)
        //todo-需要考虑使用者接口的情况，比如使用携带参数来区别文件下载的情况。目前只考虑了在url后面添加文件名的方式
        val realFileName = fileName ?: url.substring(url.lastIndexOf("/") + 1)
        val downloadEntity = MyApp.dbManager.getDownloadEntityByUrl(url)
        if (downloadEntity == null) {//没下载过
            if (FileIsExist(realFileName)) File("$fileHome/$realFileName").delete()
            val downloadEntity1 = DownloadEntity(null, url, 0, 0, realFileName, false)

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
                    downloadEntity1.totalLength = contentLength!!
                    processDownload(downloadEntity1, downloadCallback)
                }
            })
        } else {//已下载过
            val exists = FileIsExist(downloadEntity.fileName)
            downloadEntity.currentProgress = if (exists) downloadEntity.currentProgress else 0
            if (!exists) downloadEntity.setComplete(false)
            if (downloadEntity.isComplete() && exists) {
                downloadCallback?.success("$fileHome/${downloadEntity.fileName}")
            } else {
                processDownload(downloadEntity, downloadCallback)
            }
        }
    }

    private fun processDownload(downloadEntity: DownloadEntity, downloadCallback: MyCallback?) {
        if (!downloadCalls.containsKey(downloadEntity.url)) {
            val myRunnable = MyRunnable(downloadEntity, downloadCallback)
            e.execute(myRunnable)
            downloadCalls.put(downloadEntity.url, null)//防止轮询被同一个下载线程多次调用。
            scheduleInterval.scheduleAtFixedRate({
                //每隔1s 插入一次数据库，保证强制退出的时候保存进度
                if (myRunnable.getDownloadEntity().isComplete()) {
                    throw RuntimeException("移除轮询${downloadEntity.url}")
                }
                    myRunnable.saveToDB()
//                scheduleInterval.shutdown()
                Log.e("download", "插入一次${downloadEntity.url}")
            }, 0, 1000, TimeUnit.MILLISECONDS)
        }
    }

    private fun FileIsExist(fileName: String) = File(fileHome, fileName).exists()


    class MyRunnable(private val downloadEntity: DownloadEntity, private val downloadCallback: MyCallback?) : Runnable {
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


        companion object {
            val NETWORK_ERR = 0
            val IO_GET_ERR = 1
            val IO_ERR = 2
            val CONTEENTLENGTH_ERR = 3
        }

        override fun run() {
            val arr = HttpHelper.getInstance(downloadEntity.url).syncRequestByRange(downloadEntity.currentProgress, downloadEntity.totalLength)
            downloadCalls.put(downloadEntity.url, arr[0] as Call)
            toDownload(arr[1] as Response)
        }

        private fun toDownload(response: Response?) {
            if (response == null && downloadCallback != null) {
                downloadCallback.fail(NETWORK_ERR, "网络请求失败")
                return
            }
            val body = response!!.body()
            val file = getFileByName(downloadEntity.fileName)
            val randomAccessFile = RandomAccessFile(file, "rwd")
            val buf = ByteArray(1024 * 80)
            var byteStream: InputStream? = null
            try {
                byteStream = body?.byteStream()
                if (byteStream == null) {
                    downloadCallback?.fail(IO_GET_ERR, "获取流失败")
                    return
                }
                var currentProgress: Long = downloadEntity.currentProgress
                var len = byteStream.read(buf)
                randomAccessFile.seek(downloadEntity.currentProgress)
                while (len != -1) {
                    randomAccessFile.write(buf, 0, len)
                    currentProgress += len.toLong()
                    len = byteStream.read(buf)
                    downloadCallback?.progress(file.absolutePath, currentProgress)
                    downloadEntity.currentProgress = currentProgress
                }
                downloadCallback?.success(file.absolutePath)
                cancelDownloadTask(downloadEntity.url)
                downloadEntity.setComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                byteStream?.close()
                randomAccessFile.close()
                cancelDownloadTask(downloadEntity.url)
                saveToDB()
            }

        }

        fun saveToDB() {
            if (MyApp.dbManager.getDownloadEntityByUrl(downloadEntity.url) != null)//有则更新，没有则插入
                MyApp.dbManager.getWritterDownloadEntityDao().update(downloadEntity)
            else
                MyApp.dbManager.getWritterDownloadEntityDao().insert(downloadEntity)
        }

        fun getDownloadEntity(): DownloadEntity {
            return downloadEntity
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

