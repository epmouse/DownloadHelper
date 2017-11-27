package com.example.liu_xingxing.downloadhelp

import android.content.Context
import greendao.DaoMaster
import greendao.DaoSession
import greendao.DownloadEntityDao

/**
 * Created by liu_xingxing on 2017/11/27.
 */
class DbManager private constructor(context: Context) {
    private val dbName = "download_db"
    private lateinit var openHelper: DaoMaster.DevOpenHelper

    init {
        openHelper = DaoMaster.DevOpenHelper(context, dbName, null)
    }

    companion object {
        @Volatile
        private var mInstance: DbManager? = null

        fun getInstance(context: Context): DbManager{
            if (mInstance == null) {
                synchronized(DbManager::class.java) {
                    if (mInstance == null) {
                        mInstance = DbManager(context)
                    }
                }
            }
            return mInstance!!
        }
    }
    private fun getWritterSession(): DaoSession {
        return DaoMaster(openHelper.writableDatabase).newSession()
    }

    private fun getReaderSession(): DaoSession {
        return DaoMaster(openHelper.readableDatabase).newSession()
    }

    fun getWritterDownloadEntityDao(): DownloadEntityDao {
        return getWritterSession().downloadEntityDao
    }

    fun getReaderDownloadEntityDao(): DownloadEntityDao {
        return getReaderSession().downloadEntityDao
    }

    fun queryAllDownloadEntity():List<DownloadEntity>{
       return getReaderDownloadEntityDao().queryBuilder().list()
    }
    fun getDownloadEntityByUrl(url:String):DownloadEntity?{
      return  getReaderDownloadEntityDao().queryBuilder().where(DownloadEntityDao.Properties.Url.eq(url)).build().unique()
    }

}