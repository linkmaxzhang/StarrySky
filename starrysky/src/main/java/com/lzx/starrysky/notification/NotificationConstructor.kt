package com.lzx.starrysky.notification

import android.app.PendingIntent

/**
 * 通知栏构建者，可设置各种通知栏配置
 */
class NotificationConstructor private constructor(builder: Builder) {

    var isCreateSystemNotification = true   //是否使用系统通知栏
    val isNotificationCanClearBySystemBtn: Boolean //是否让通知栏当暂停的时候可以滑动清除
    val targetClass: String? //通知栏点击转跳界面
    val contentTitle: String? //通知栏标题
    val contentText: String?  //通知栏内容
    val nextIntent: PendingIntent? //下一首按钮 PendingIntent
    val preIntent: PendingIntent? //上一首按钮 PendingIntent
    val closeIntent: PendingIntent? //关闭按钮 PendingIntent
    val favoriteIntent: PendingIntent? //喜欢或收藏按钮 PendingIntent
    val lyricsIntent: PendingIntent? //桌面歌词按钮 PendingIntent
    val playIntent: PendingIntent? //播放按钮 PendingIntent
    val pauseIntent: PendingIntent? // 暂停按钮 PendingIntent
    val playOrPauseIntent: PendingIntent? // 播放/暂停按钮 PendingIntent
    val stopIntent: PendingIntent? //停止按钮 PendingIntent
    val downloadIntent: PendingIntent? //下载按钮 PendingIntent
    val pendingIntentMode: Int //通知栏点击模式
    val isSystemNotificationShowTime: Boolean //系统通知栏是否显示时间

    var skipPreviousDrawableRes = -1 //上一首的drawable res
    var skipPreviousTitle = "" //上一首的 title
    var skipNextDrawableRes = -1 //下一首的drawable res
    var skipNextTitle = "" //下一首的 title
    var labelPause = ""
    var pauseDrawableRes = -1
    var labelPlay = ""
    var playDrawableRes = -1
    var smallIconRes = -1

    init {
        isCreateSystemNotification = builder.isCreateSystemNotification
        isNotificationCanClearBySystemBtn = builder.isNotificationCanClearBySystemBtn
        targetClass = builder.targetClass
        contentTitle = builder.contentTitle
        contentText = builder.contentText
        nextIntent = builder.nextIntent
        preIntent = builder.preIntent
        closeIntent = builder.closeIntent
        favoriteIntent = builder.favoriteIntent
        lyricsIntent = builder.lyricsIntent
        playIntent = builder.playIntent
        pauseIntent = builder.pauseIntent
        playOrPauseIntent = builder.playOrPauseIntent
        stopIntent = builder.stopIntent
        downloadIntent = builder.downloadIntent
        pendingIntentMode = builder.pendingIntentMode
        isSystemNotificationShowTime = builder.isSystemNotificationShowTime
        skipPreviousDrawableRes = builder.skipPreviousDrawableRes
        skipPreviousTitle = builder.skipPreviousTitle
        skipNextDrawableRes = builder.skipNextDrawableRes
        skipNextTitle = builder.skipNextTitle
        labelPause = builder.labelPause
        pauseDrawableRes = builder.pauseDrawableRes
        labelPlay = builder.labelPlay
        playDrawableRes = builder.playDrawableRes
        smallIconRes = builder.smallIconRes
    }

    class Builder {
         var isCreateSystemNotification = true   //是否使用系统通知栏
         var isNotificationCanClearBySystemBtn: Boolean = false //是否让通知栏当暂停的时候可以滑动清除
         var targetClass: String? = null //通知栏点击转跳界面
         var contentTitle: String? = null //通知栏标题
         var contentText: String? = null  //通知栏内容
         var nextIntent: PendingIntent? = null //下一首按钮 PendingIntent
         var preIntent: PendingIntent? = null //上一首按钮 PendingIntent
         var closeIntent: PendingIntent? = null //关闭按钮 PendingIntent
         var favoriteIntent: PendingIntent? = null //喜欢或收藏按钮 PendingIntent
         var lyricsIntent: PendingIntent? = null //桌面歌词按钮 PendingIntent
         var playIntent: PendingIntent? = null //播放按钮 PendingIntent
         var pauseIntent: PendingIntent? = null // 暂停按钮 PendingIntent
         var playOrPauseIntent: PendingIntent? = null // 播放/暂停按钮 PendingIntent
         var stopIntent: PendingIntent? = null //停止按钮 PendingIntent
         var downloadIntent: PendingIntent? = null //下载按钮 PendingIntent
         var pendingIntentMode: Int = 0 //通知栏点击模式
         var isSystemNotificationShowTime: Boolean = false //系统通知栏是否显示时间

         var skipPreviousDrawableRes = -1 //上一首的drawable res
         var skipPreviousTitle = "" //上一首的 title
         var skipNextDrawableRes = -1 //下一首的drawable res
         var skipNextTitle = "" //下一首的 title
         var labelPause = ""
         var pauseDrawableRes = -1
         var labelPlay = ""
         var playDrawableRes = -1
         var smallIconRes = -1



        fun bulid(): NotificationConstructor {
            return NotificationConstructor(this)
        }
    }

    companion object {

        val MODE_ACTIVITY = 0
        val MODE_BROADCAST = 1
        val MODE_SERVICE = 2
    }
}
