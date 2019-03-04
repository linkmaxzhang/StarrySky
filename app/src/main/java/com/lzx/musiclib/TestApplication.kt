package com.lzx.musiclib

import android.app.Application
import android.os.Environment

import com.lzx.starrysky.manager.MusicManager
import com.lzx.starrysky.notification.NotificationConstructor
import com.lzx.starrysky.playback.download.ExoDownload


/**
 * create by lzx
 * time:2018/11/9
 */
class TestApplication : Application() {


    override fun onCreate() {
        super.onCreate()
        //初始化
        MusicManager.initMusicManager(this)
        //配置通知栏

        val builder = NotificationConstructor.Builder()
        builder.isCreateSystemNotification = false
        val constructor = builder.bulid()

        MusicManager.instance.setNotificationConstructor(constructor)

        //设置缓存
        val destFileDir = Environment.getExternalStorageDirectory().absolutePath + "/11ExoCacheDir"
        ExoDownload.instance.isOpenCache = true //打开缓存开关
        ExoDownload.instance.isShowNotificationWhenDownload = true
        ExoDownload.instance.setCacheDestFileDir(destFileDir) //设置缓存文件夹
    }

    companion object {

        var ACTION_PLAY_OR_PAUSE = "ACTION_PLAY_OR_PAUSE"
        var ACTION_NEXT = "ACTION_NEXT"
        var ACTION_PRE = "ACTION_PRE"
        var ACTION_FAVORITE = "ACTION_FAVORITE"
        var ACTION_LYRICS = "ACTION_LYRICS"
    }
}
