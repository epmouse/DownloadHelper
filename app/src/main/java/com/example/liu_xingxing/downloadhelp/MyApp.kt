package com.example.liu_xingxing.downloadhelp

import android.app.Application

/**
 * Created by liu_xingxing on 2017/11/27.
 */
class MyApp : Application() {
    companion object {
        lateinit var dbManager: DbManager
    }

    override fun onCreate() {
        super.onCreate()
        dbManager = DbManager.getInstance(this)
    }
}