package com.lzx.starrysky.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.RemoteException
import android.support.v4.app.NotificationCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.lzx.starrysky.MusicService
import com.lzx.starrysky.R
import com.lzx.starrysky.model.MusicProvider
import com.lzx.starrysky.model.SongInfo
import com.lzx.starrysky.notification.factory.INotification
import com.lzx.starrysky.notification.utils.NotificationColorUtils
import com.lzx.starrysky.notification.utils.NotificationUtils


/**
 * 自定义通知栏
 */
class CustomNotification @Throws(RemoteException::class)
constructor(private val mService: MusicService, private val mConstructor: NotificationConstructor) : BroadcastReceiver(), INotification {

    private val mRemoteView: RemoteViews
    private val mBigRemoteView: RemoteViews?

    private var mPlayOrPauseIntent: PendingIntent? = null
    private var mPlayIntent: PendingIntent? = null
    private var mPauseIntent: PendingIntent? = null
    private var mStopIntent: PendingIntent? = null
    private var mNextIntent: PendingIntent? = null
    private var mPreviousIntent: PendingIntent? = null
    private var mFavoriteIntent: PendingIntent? = null
    private var mLyricsIntent: PendingIntent? = null
    private var mDownloadIntent: PendingIntent? = null
    private var mCloseIntent: PendingIntent? = null
    private var mSessionToken: MediaSessionCompat.Token? = null
    private var mController: MediaControllerCompat? = null
    private var mTransportControls: MediaControllerCompat.TransportControls? = null
    private var mPlaybackState: PlaybackStateCompat? = null
    private var mMetadata: MediaMetadataCompat? = null

    private val mNotificationManager: NotificationManager?
    private val packageName: String
    private var mStarted = false
    private var mNotification: Notification? = null

    private val res: Resources
    private val mColorUtils: NotificationColorUtils

    init {

        updateSessionToken()

        mNotificationManager = mService.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        packageName = mService.applicationContext.packageName
        res = mService.applicationContext.resources
        mColorUtils = NotificationColorUtils()

        setStopIntent(mConstructor.stopIntent)
        setNextPendingIntent(mConstructor.nextIntent)
        setPrePendingIntent(mConstructor.preIntent)
        setPlayPendingIntent(mConstructor.playIntent)
        setPausePendingIntent(mConstructor.pauseIntent)
        setFavoritePendingIntent(mConstructor.favoriteIntent)
        setLyricsPendingIntent(mConstructor.lyricsIntent)
        setDownloadPendingIntent(mConstructor.downloadIntent)
        setClosePendingIntent(mConstructor.closeIntent)
        setPlayOrPauseIntent(mConstructor.playOrPauseIntent)

        mRemoteView = createRemoteViews(false)
        mBigRemoteView = createRemoteViews(true)

        mNotificationManager.cancelAll()
    }

    private val mCb = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            mPlaybackState = state
            if (state!!.state == PlaybackStateCompat.STATE_STOPPED || state.state == PlaybackStateCompat.STATE_NONE) {
                stopNotification()
            } else {
                val notification = createNotification()
                if (notification != null) {
                    mNotificationManager!!.notify(INotification.NOTIFICATION_ID, notification)
                }
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            mMetadata = metadata
            val notification = createNotification()
            if (notification != null) {
                mNotificationManager!!.notify(INotification.NOTIFICATION_ID, notification)
            }
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            try {
                updateSessionToken()
            } catch (e: RemoteException) {
                e.printStackTrace()
            }

        }
    }

    @Throws(RemoteException::class)
    private fun updateSessionToken() {
        val freshToken = mService.sessionToken
        if (mSessionToken == null && freshToken != null || mSessionToken != null && mSessionToken != freshToken) {
            if (mController != null) {
                mController!!.unregisterCallback(mCb)
            }
            mSessionToken = freshToken
            if (mSessionToken != null) {
                mController = MediaControllerCompat(mService, mSessionToken!!)
                mTransportControls = mController!!.transportControls
                if (mStarted) {
                    mController!!.registerCallback(mCb)
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        when (action) {
            INotification.ACTION_PAUSE -> mTransportControls!!.pause()
            INotification.ACTION_PLAY -> mTransportControls!!.play()
            INotification.ACTION_PLAY_OR_PAUSE -> if (mPlaybackState!!.state == PlaybackStateCompat.STATE_PLAYING) {
                mTransportControls!!.pause()
            } else {
                mTransportControls!!.play()
            }
            INotification.ACTION_NEXT -> mTransportControls!!.skipToNext()
            INotification.ACTION_PREV -> mTransportControls!!.skipToPrevious()
            INotification.ACTION_CLOSE -> stopNotification()
            else -> {
            }
        }
    }

    override fun startNotification() {
        if (!mStarted) {
            mMetadata = mController!!.metadata
            mPlaybackState = mController!!.playbackState

            // The notification must be updated after setting started to true
            val notification = createNotification()
            if (notification != null) {
                mController!!.registerCallback(mCb)
                val filter = IntentFilter()
                filter.addAction(INotification.ACTION_NEXT)
                filter.addAction(INotification.ACTION_PAUSE)
                filter.addAction(INotification.ACTION_PLAY)
                filter.addAction(INotification.ACTION_PREV)
                filter.addAction(INotification.ACTION_PLAY_OR_PAUSE)
                filter.addAction(INotification.ACTION_CLOSE)

                mService.registerReceiver(this, filter)

                mService.startForeground(INotification.NOTIFICATION_ID, notification)
                mStarted = true
            }
        }
    }

    override fun stopNotification() {
        if (mStarted) {
            mStarted = false
            mController!!.unregisterCallback(mCb)
            try {
                mNotificationManager!!.cancel(INotification.NOTIFICATION_ID)
                mService.unregisterReceiver(this)
            } catch (ex: IllegalArgumentException) {
                ex.printStackTrace()
            }

            mService.stopForeground(true)
        }
    }

    private fun createNotification(): Notification? {
        if (mMetadata == null || mPlaybackState == null) {
            return null
        }
        val description = mMetadata!!.description

        val songId = mMetadata!!.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
        val smallIcon = if (mConstructor.smallIconRes != -1) mConstructor.smallIconRes else R.drawable.ic_notification
        //适配8.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.createNotificationChannel(mService, mNotificationManager!!)
        }
        val notificationBuilder = NotificationCompat.Builder(mService, INotification.CHANNEL_ID)
        notificationBuilder
                .setOnlyAlertOnce(true)
                .setSmallIcon(smallIcon)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(description.title) //歌名
                .setContentText(description.subtitle) //艺术家
        //setContentIntent
        if (!TextUtils.isEmpty(mConstructor.targetClass)) {
            val clazz = NotificationUtils.getTargetClass(mConstructor.targetClass!!)
            if (clazz != null) {
                notificationBuilder.setContentIntent(NotificationUtils.createContentIntent(mService, mConstructor, songId, null, clazz))
            }
        }
        //setCustomContentView and setCustomBigContentView
        if (Build.VERSION.SDK_INT >= 24) {
            notificationBuilder.setCustomContentView(mRemoteView)
            notificationBuilder.setCustomBigContentView(mBigRemoteView)
        }

        setNotificationPlaybackState(notificationBuilder)

        //create Notification
        mNotification = notificationBuilder.build()
        mNotification!!.contentView = mRemoteView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mNotification!!.bigContentView = mBigRemoteView
        }
        var songInfo: SongInfo? = null
        val songInfos = MusicProvider.instance.songInfos
        for (info in songInfos) {
            if (info.songId == songId) {
                songInfo = info
                break
            }
        }
        updateRemoteViewUI(mNotification!!, songInfo, smallIcon)

        return mNotification
    }

    private fun setNotificationPlaybackState(builder: NotificationCompat.Builder) {
        if (mPlaybackState == null || !mStarted) {
            mService.stopForeground(true)
            return
        }
        builder.setOngoing(mPlaybackState!!.state == PlaybackStateCompat.STATE_PLAYING)
    }

    /**
     * 创建RemoteViews
     */
    private fun createRemoteViews(isBigRemoteViews: Boolean): RemoteViews {
        val remoteView: RemoteViews
        if (isBigRemoteViews) {
            remoteView = RemoteViews(packageName, getResourceId(INotification.LAYOUT_NOTIFY_BIG_PLAY, "layout"))
        } else {
            remoteView = RemoteViews(packageName, getResourceId(INotification.LAYOUT_NOTIFY_PLAY, "layout"))
        }
        if (mPlayIntent != null) {
            remoteView.setOnClickPendingIntent(getResourceId(INotification.ID_IMG_NOTIFY_PLAY, "id"), mPlayIntent)
        }
        if (mPauseIntent != null) {
            remoteView.setOnClickPendingIntent(getResourceId(INotification.ID_IMG_NOTIFY_PAUSE, "id"), mPauseIntent)
        }
        if (mStopIntent != null) {
            remoteView.setOnClickPendingIntent(getResourceId(INotification.ID_IMG_NOTIFY_STOP, "id"), mStopIntent)
        }
        if (mFavoriteIntent != null) {
            remoteView.setOnClickPendingIntent(getResourceId(INotification.ID_IMG_NOTIFY_FAVORITE, "id"), mFavoriteIntent)
        }
        if (mLyricsIntent != null) {
            remoteView.setOnClickPendingIntent(getResourceId(INotification.ID_IMG_NOTIFY_LYRICS, "id"), mLyricsIntent)
        }
        if (mDownloadIntent != null) {
            remoteView.setOnClickPendingIntent(getResourceId(INotification.ID_IMG_NOTIFY_DOWNLOAD, "id"), mDownloadIntent)
        }
        if (mNextIntent != null) {
            remoteView.setOnClickPendingIntent(getResourceId(INotification.ID_IMG_NOTIFY_NEXT, "id"), mNextIntent)
        }
        if (mPreviousIntent != null) {
            remoteView.setOnClickPendingIntent(getResourceId(INotification.ID_IMG_NOTIFY_PRE, "id"), mPreviousIntent)
        }
        if (mCloseIntent != null) {
            remoteView.setOnClickPendingIntent(getResourceId(INotification.ID_IMG_NOTIFY_CLOSE, "id"), mCloseIntent)
        }
        if (mPlayOrPauseIntent != null) {
            remoteView.setOnClickPendingIntent(getResourceId(INotification.ID_IMG_NOTIFY_PLAY_OR_PAUSE, "id"), mPlayOrPauseIntent)
        }
        return remoteView
    }

    /**
     * 更新RemoteViews
     */
    private fun updateRemoteViewUI(notification: Notification, songInfo: SongInfo?, smallIcon: Int) {
        val isDark = mColorUtils.isDarkNotificationBar(mService, notification)

        var art: Bitmap? = mMetadata!!.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)
        val artistName = if (songInfo != null) songInfo.artist else ""
        val songName = if (songInfo != null) songInfo.songName else ""
        //设置文字内容
        mRemoteView.setTextViewText(getResourceId(INotification.ID_TXT_NOTIFY_SONGNAME, "id"), songName)
        mRemoteView.setTextViewText(getResourceId(INotification.ID_TXT_NOTIFY_ARTISTNAME, "id"), artistName)
        //设置播放暂停按钮

        if (mPlaybackState!!.state == PlaybackStateCompat.STATE_PLAYING) {
            mRemoteView.setImageViewResource(getResourceId(INotification.ID_IMG_NOTIFY_PLAY_OR_PAUSE, "id"),
                    getResourceId(if (isDark)
                        INotification.DRAWABLE_NOTIFY_BTN_DARK_PAUSE_SELECTOR
                    else
                        INotification.DRAWABLE_NOTIFY_BTN_LIGHT_PAUSE_SELECTOR, "drawable"))
        } else {
            mRemoteView.setImageViewResource(getResourceId(INotification.ID_IMG_NOTIFY_PLAY_OR_PAUSE, "id"),
                    getResourceId(if (isDark)
                        INotification.DRAWABLE_NOTIFY_BTN_DARK_PLAY_SELECTOR
                    else
                        INotification.DRAWABLE_NOTIFY_BTN_LIGHT_PLAY_SELECTOR, "drawable"))
        }

        //大布局
        //设置文字内容
        mBigRemoteView!!.setTextViewText(getResourceId(INotification.ID_TXT_NOTIFY_SONGNAME, "id"), songName)
        mBigRemoteView.setTextViewText(getResourceId(INotification.ID_TXT_NOTIFY_ARTISTNAME, "id"), artistName)
        //设置播放暂停按钮
        if (mPlaybackState!!.state == PlaybackStateCompat.STATE_PLAYING) {
            mBigRemoteView.setImageViewResource(getResourceId(INotification.ID_IMG_NOTIFY_PLAY_OR_PAUSE, "id"),
                    getResourceId(if (isDark)
                        INotification.DRAWABLE_NOTIFY_BTN_DARK_PAUSE_SELECTOR
                    else
                        INotification.DRAWABLE_NOTIFY_BTN_LIGHT_PAUSE_SELECTOR, "drawable"))
        } else {
            mBigRemoteView.setImageViewResource(getResourceId(INotification.ID_IMG_NOTIFY_PLAY_OR_PAUSE, "id"),
                    getResourceId(if (isDark)
                        INotification.DRAWABLE_NOTIFY_BTN_DARK_PLAY_SELECTOR
                    else
                        INotification.DRAWABLE_NOTIFY_BTN_LIGHT_PLAY_SELECTOR, "drawable"))
        }
        //设置喜欢或收藏按钮
        mBigRemoteView.setImageViewResource(getResourceId(INotification.ID_IMG_NOTIFY_FAVORITE, "id"),
                getResourceId(if (isDark)
                    INotification.DRAWABLE_NOTIFY_BTN_DARK_FAVORITE
                else
                    INotification.DRAWABLE_NOTIFY_BTN_LIGHT_FAVORITE, "drawable"))
        //设置歌词按钮
        mBigRemoteView.setImageViewResource(getResourceId(INotification.ID_IMG_NOTIFY_LYRICS, "id"),
                getResourceId(if (isDark)
                    INotification.DRAWABLE_NOTIFY_BTN_DARK_LYRICS
                else
                    INotification.DRAWABLE_NOTIFY_BTN_LIGHT_LYRICS, "drawable"))
        //设置下载按钮
        mBigRemoteView.setImageViewResource(getResourceId(INotification.ID_IMG_NOTIFY_DOWNLOAD, "id"),
                getResourceId(if (isDark)
                    INotification.DRAWABLE_NOTIFY_BTN_DARK_DOWNLOAD
                else
                    INotification.DRAWABLE_NOTIFY_BTN_LIGHT_DOWNLOAD, "drawable"))

        //上一首下一首按钮
        val hasNextSong = mPlaybackState!!.actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L
        val hasPreSong = mPlaybackState!!.actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L
        disableNextBtn(hasNextSong, isDark)
        disablePreviousBtn(hasPreSong, isDark)

        //封面
        var fetchArtUrl: String? = null
        if (art == null) {
            fetchArtUrl = mMetadata!!.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
            if (TextUtils.isEmpty(fetchArtUrl)) {
                art = BitmapFactory.decodeResource(mService.resources, R.drawable.default_art)
            }
        }
        mRemoteView.setImageViewBitmap(getResourceId(INotification.ID_IMG_NOTIFY_ICON, "id"), art)
        mBigRemoteView.setImageViewBitmap(getResourceId(INotification.ID_IMG_NOTIFY_ICON, "id"), art)
        mNotificationManager!!.notify(INotification.NOTIFICATION_ID, notification)

        if (fetchArtUrl != null) {
            fetchBitmapFromURLAsync(fetchArtUrl, notification)
        }
    }

    /**
     * 加载封面
     */
    private fun fetchBitmapFromURLAsync(fetchArtUrl: String, notification: Notification) {
        Glide.with(mService).applyDefaultRequestOptions(
                RequestOptions()
                        .fallback(R.drawable.default_art)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE))
                .asBitmap().load(fetchArtUrl).into(object : SimpleTarget<Bitmap>(144, 144) {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        mRemoteView.setImageViewBitmap(getResourceId(INotification.ID_IMG_NOTIFY_ICON, "id"), resource)
                        mBigRemoteView!!.setImageViewBitmap(getResourceId(INotification.ID_IMG_NOTIFY_ICON, "id"), resource)
                        mNotificationManager!!.notify(INotification.NOTIFICATION_ID, notification)
                    }
                })
    }

    /**
     * 下一首按钮样式
     */
    private fun disableNextBtn(disable: Boolean, isDark: Boolean) {
        val res: Int
        if (disable) {
            res = this.getResourceId(if (isDark)
                INotification.DRAWABLE_NOTIFY_BTN_DARK_NEXT_PRESSED
            else
                INotification.DRAWABLE_NOTIFY_BTN_LIGHT_NEXT_PRESSED, "drawable")
        } else {
            res = this.getResourceId(if (isDark)
                INotification.DRAWABLE_NOTIFY_BTN_DARK_NEXT_SELECTOR
            else
                INotification.DRAWABLE_NOTIFY_BTN_LIGHT_NEXT_SELECTOR, "drawable")
        }
        mRemoteView.setImageViewResource(getResourceId(INotification.ID_IMG_NOTIFY_NEXT, "id"), res)
        mBigRemoteView!!.setImageViewResource(getResourceId(INotification.ID_IMG_NOTIFY_NEXT, "id"), res)
    }

    /**
     * 上一首按钮样式
     */
    private fun disablePreviousBtn(disable: Boolean, isDark: Boolean) {
        val res: Int = if (disable) {
            this.getResourceId(if (isDark)
                INotification.DRAWABLE_NOTIFY_BTN_DARK_PREV_PRESSED
            else
                INotification.DRAWABLE_NOTIFY_BTN_LIGHT_PREV_PRESSED, "drawable")
        } else {
            this.getResourceId(if (isDark)
                INotification.DRAWABLE_NOTIFY_BTN_DARK_PREV_SELECTOR
            else
                INotification.DRAWABLE_NOTIFY_BTN_LIGHT_PREV_SELECTOR, "drawable")
        }
        mRemoteView.setImageViewResource(getResourceId(INotification.ID_IMG_NOTIFY_PRE, "id"), res)
        mBigRemoteView?.setImageViewResource(getResourceId(INotification.ID_IMG_NOTIFY_PRE, "id"), res)
    }

    /**
     * 更新喜欢或收藏按钮样式
     */
    override fun updateFavoriteUI(isFavorite: Boolean) {
        if (mNotification == null) {
            return
        }
        val isDark = mColorUtils.isDarkNotificationBar(mService, mNotification!!)
        //喜欢或收藏按钮选中时样式
        if (isFavorite) {
            mBigRemoteView!!.setImageViewResource(getResourceId(INotification.ID_IMG_NOTIFY_FAVORITE, "id"),
                    getResourceId(INotification.DRAWABLE_NOTIFY_BTN_FAVORITE, "drawable"))
        } else {
            //喜欢或收藏按钮没选中时样式
            mBigRemoteView!!.setImageViewResource(getResourceId(INotification.ID_IMG_NOTIFY_FAVORITE, "id"),
                    getResourceId(if (isDark)
                        INotification.DRAWABLE_NOTIFY_BTN_DARK_FAVORITE
                    else
                        INotification.DRAWABLE_NOTIFY_BTN_LIGHT_FAVORITE, "drawable"))
        }
        mNotificationManager!!.notify(INotification.NOTIFICATION_ID, mNotification)
    }

    /**
     * 更新歌词按钮UI
     */
    override fun updateLyricsUI(isChecked: Boolean) {
        if (mNotification == null) {
            return
        }
        val isDark = mColorUtils.isDarkNotificationBar(mService, mNotification!!)
        //歌词按钮选中时样式
        if (isChecked) {
            mBigRemoteView!!.setImageViewResource(getResourceId(INotification.ID_IMG_NOTIFY_LYRICS, "id"),
                    getResourceId(INotification.DRAWABLE_NOTIFY_BTN_LYRICS, "drawable"))
        } else {
            //歌词按钮没选中时样式
            mBigRemoteView!!.setImageViewResource(getResourceId(INotification.ID_IMG_NOTIFY_LYRICS, "id"),
                    getResourceId(if (isDark)
                        INotification.DRAWABLE_NOTIFY_BTN_DARK_LYRICS
                    else
                        INotification.DRAWABLE_NOTIFY_BTN_LIGHT_LYRICS, "drawable"))
        }
        mNotificationManager!!.notify(INotification.NOTIFICATION_ID, mNotification)
    }


    private fun getResourceId(name: String, className: String): Int {
        return res.getIdentifier(name, className, packageName)
    }

    private fun setStopIntent(pendingIntent: PendingIntent?) {
        mStopIntent = pendingIntent ?: getPendingIntent(INotification.ACTION_STOP)
    }

    private fun setNextPendingIntent(pendingIntent: PendingIntent?) {
        mNextIntent = pendingIntent ?: getPendingIntent(INotification.ACTION_NEXT)
    }

    private fun setPrePendingIntent(pendingIntent: PendingIntent?) {
        mPreviousIntent = pendingIntent ?: getPendingIntent(INotification.ACTION_PREV)
    }

    private fun setPlayPendingIntent(pendingIntent: PendingIntent?) {
        mPlayIntent = pendingIntent ?: getPendingIntent(INotification.ACTION_PLAY)
    }

    private fun setPausePendingIntent(pendingIntent: PendingIntent?) {
        mPauseIntent = pendingIntent ?: getPendingIntent(INotification.ACTION_PAUSE)
    }

    private fun setFavoritePendingIntent(pendingIntent: PendingIntent?) {
        mFavoriteIntent = pendingIntent ?: getPendingIntent(INotification.ACTION_FAVORITE)
    }

    private fun setLyricsPendingIntent(pendingIntent: PendingIntent?) {
        mLyricsIntent = pendingIntent ?: getPendingIntent(INotification.ACTION_LYRICS)
    }

    private fun setDownloadPendingIntent(pendingIntent: PendingIntent?) {
        mDownloadIntent = pendingIntent ?: getPendingIntent(INotification.ACTION_DOWNLOAD)
    }

    private fun setClosePendingIntent(pendingIntent: PendingIntent?) {
        mCloseIntent = pendingIntent ?: getPendingIntent(INotification.ACTION_CLOSE)
    }

    private fun setPlayOrPauseIntent(pendingIntent: PendingIntent?) {
        mPlayOrPauseIntent = pendingIntent
                ?: getPendingIntent(INotification.ACTION_PLAY_OR_PAUSE)
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(action)
        intent.setPackage(packageName)
        return PendingIntent.getBroadcast(mService, INotification.REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

}
