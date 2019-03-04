package com.lzx.starrysky.manager

import android.content.ComponentName
import android.content.Context
import android.os.RemoteException
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.lzx.starrysky.MusicService
import com.lzx.starrysky.model.MusicProvider


/**
 * 与服务端连接的管理类
 */
class MediaSessionConnection private constructor(private val serviceComponent: ComponentName) {
    /**
     * 获取 MediaBrowserCompat
     */
    val mediaBrowser: MediaBrowserCompat
    /**
     * 是否已连接
     */
    var isConnected: Boolean = false
        private set
    /**
     * 获取rootMediaId
     */
    var rootMediaId: String? = null
        private set
    /**
     * 获取当前播放的 PlaybackStateCompat
     */
    var playbackState = EMPTY_PLAYBACK_STATE
        private set
    /**
     * 获取当前播放的 MediaMetadataCompat
     */
    var nowPlaying = NOTHING_PLAYING
        private set
    /**
     * 获取播放控制器
     */
    var transportControls: MediaControllerCompat.TransportControls? = null
        private set
    var mediaController: MediaControllerCompat? = null
        private set
    private val mediaBrowserConnectionCallback: MediaBrowserConnectionCallback
    private val mMediaControllerCallback: MediaControllerCallback
    private var mConnectListener: OnConnectListener? = null

    init {
        mediaBrowserConnectionCallback = MediaBrowserConnectionCallback()
        mMediaControllerCallback = MediaControllerCallback()
        mediaBrowser = MediaBrowserCompat(sContext, serviceComponent, mediaBrowserConnectionCallback, null)
    }

    fun subscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.subscribe(parentId, callback)
    }

    fun unsubscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.unsubscribe(parentId, callback)
    }

    fun setConnectListener(connectListener: OnConnectListener) {
        mConnectListener = connectListener
    }

    /**
     * 连接
     */
    fun connect() {
        if (!isConnected) {
            mediaBrowser.connect()
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        if (isConnected) {
            if (mediaController != null) {
                mediaController!!.unregisterCallback(mMediaControllerCallback)
            }
            mediaBrowser.disconnect()
        }
    }

    /**
     * 连接回调
     */
    private inner class MediaBrowserConnectionCallback : MediaBrowserCompat.ConnectionCallback() {
        /**
         * 已连接上
         */
        override fun onConnected() {
            super.onConnected()
            try {
                mediaController = MediaControllerCompat(sContext, mediaBrowser.sessionToken)
                mediaController!!.registerCallback(mMediaControllerCallback)
                transportControls = mediaController!!.transportControls
                rootMediaId = mediaBrowser.root
                isConnected = true
                if (mConnectListener != null) {
                    mConnectListener!!.onConnected()
                }
            } catch (e: RemoteException) {
                e.printStackTrace()
            }

        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            isConnected = false
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            isConnected = false
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            playbackState = state ?: EMPTY_PLAYBACK_STATE

            //状态监听
            val mPlayerEventListeners = MusicManager.instance.playerEventListeners
            if (state != null) {
                for (listener in mPlayerEventListeners) {
                    when (state.state) {
                        PlaybackStateCompat.STATE_PLAYING -> listener.onPlayerStart()
                        PlaybackStateCompat.STATE_PAUSED -> listener.onPlayerPause()
                        PlaybackStateCompat.STATE_STOPPED -> listener.onPlayerStop()
                        PlaybackStateCompat.STATE_ERROR -> listener.onError(state.errorCode, state.errorMessage.toString())
                        PlaybackStateCompat.STATE_NONE -> {
                            val songId = nowPlaying.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                            val songInfo = MusicProvider.instance.getSongInfo(songId)
                            listener.onPlayCompletion(songInfo!!)
                        }
                        PlaybackStateCompat.STATE_BUFFERING -> listener.onBuffering()
                        else -> {
                        }
                    }
                }
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            nowPlaying = metadata ?: NOTHING_PLAYING

            //状态监听
            val mPlayerEventListeners = MusicManager.instance.playerEventListeners
            if (metadata != null) {
                for (listener in mPlayerEventListeners) {
                    val songId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                    val songInfo = MusicProvider.instance.getSongInfo(songId)
                    listener.onMusicSwitch(songInfo!!)
                }
            }
        }

        override fun onQueueChanged(queue: List<MediaSessionCompat.QueueItem>?) {
            super.onQueueChanged(queue)
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }

    interface OnConnectListener {
        fun onConnected()
    }

    companion object {
        private var sContext: Context? = null

        fun initConnection(context: Context) {
            sContext = context
        }

        @Volatile
        private var sInstance: MediaSessionConnection? = null

        val instance: MediaSessionConnection?
            get() {
                if (sInstance == null) {
                    synchronized(MediaSessionConnection::class.java) {
                        if (sInstance == null) {
                            sInstance = MediaSessionConnection(ComponentName(sContext!!, MusicService::class.java))
                        }
                    }
                }
                return sInstance
            }

        private val EMPTY_PLAYBACK_STATE = PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_NONE, 0, 0f)
                .build()

        private val NOTHING_PLAYING = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0)
                .build()
    }
}
