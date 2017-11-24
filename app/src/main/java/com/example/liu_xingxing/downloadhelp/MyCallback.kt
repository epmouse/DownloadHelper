package com.example.liu_xingxing.downloadhelp

/**
 * Created by liu_xingxing on 2017/11/24.
 */
interface MyCallback{
    fun success(filePath:String)
    fun fail(errCode:Int,errMsg:String)
    fun progress(filePath: String,progress:Long)
}