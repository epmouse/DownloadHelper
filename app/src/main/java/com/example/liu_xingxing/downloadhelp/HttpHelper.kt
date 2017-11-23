package com.example.liu_xingxing.downloadhelp

import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


/**
 * Created by stars on 2017/11/22.
 */
class HttpHelper private constructor(private val url: String) {
    private val okhttp = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private lateinit var mUrl: String
        fun getInstance(url: String): HttpHelper {
            mUrl = url
            return Inner.instance
        }
    }

    private object Inner {
        val instance = HttpHelper(mUrl)
    }


    /**
     * @param fileUrl     文件url
     * @param destFileDir 文件存储路径
     * @param callBack
     */
    fun downLoadFileProgress(fileName: String, destFileDir: String,
                             success: (filePath: String) -> Unit,
                             progress: (path:String,total: Long, i: Long) -> Unit,
                             start: (totalLength: Long) -> Unit,
                             fail: (err: String) -> Unit
    ) {

        val dir = File(destFileDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(destFileDir, fileName)
        if (file.exists()) {
            fail("文件已经存在")
            return
        }
        val request = Request.Builder()
                .url(url)
                .build()
        val response = okhttp.newCall(request).execute()
        if (!response.isSuccessful) {
            fail("网络请求失败")
            return
        }
        val body = response.body()
        if (body == null) {
            fail("读取信息失败")
            return
        }
        var byteStream: InputStream? = null
        var fos: FileOutputStream? = null
        try {
            byteStream = body.byteStream()
            fos = FileOutputStream(file)
            val buf = ByteArray(1024 * 8)
            var currentProgress: Long = 0
            start(body.contentLength())
            var len = byteStream.read(buf)
            while (len != -1) {
                fos.write(buf, 0, len)
                len = byteStream.read(buf)
                currentProgress += len
                progress(file.absolutePath,body.contentLength(),currentProgress)
            }

            fos!!.flush()
            success(file.absolutePath)
        } catch (e: Exception) {
            fail("io异常")
            e.printStackTrace()
        } finally {
            byteStream?.close()
            fos?.close()
        }
    }

    fun getContentLength():Long?{
        val request = Request.Builder()
                .url(url)
                .build()
        return okhttp.newCall(request).execute().body()?.contentLength()

    }
}