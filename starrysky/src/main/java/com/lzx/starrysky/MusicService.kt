package com.lzx.starrysky

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.RemoteException
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.lzx.starrysky.manager.MusicManager
import com.lzx.starrysky.model.MusicProvider
import com.lzx.starrysky.notification.factory.NotificationFactory
import com.lzx.starrysky.playback.ExoPlayback
import com.lzx.starrysky.playback.PlaybackManager
import com.lzx.starrysky.playback.QueueManager
import java.lang.ref.WeakReference


class MusicService : MediaBrowserServiceCompat(), QueueManager.MetadataUpdateListener, PlaybackManager.PlaybackServiceCallback {

    private var mediaSession: MediaSessionCompat? = null
    private var mediaController: MediaControllerCompat? = null
    private var transportControls: MediaControllerCompat.TransportControls? = null

    private var mPackageValidator: PackageValidator? = null
    private var mPlaybackManager: PlaybackManager? = null

    private var mNotificationFactory: NotificationFactory? = null

    private var mBecomingNoisyReceiver: BecomingNoisyReceiver? = null
    private val mDelayedStopHandler = DelayedStopHandler(this)

    override fun onCreate() {
        super.onCreate()
        val musicProvider = MusicProvider.instance
        val queueManager = QueueManager(this, musicProvider, this)
        val playback = ExoPlayback(this, musicProvider)

        mPlaybackManager = PlaybackManager(this, this, queueManager, playback)

        val sessionIntent = packageManager.getLaunchIntentForPackage(packageName)
        val sessionActivityPendingIntent = PendingIntent.getActivity(this, 0, sessionIntent, 0)

        //会话连接
        mediaSession = MediaSessionCompat(this, "MusicService")
        sessionToken = mediaSession!!.sessionToken
        mediaSession!!.setSessionActivity(sessionActivityPendingIntent)
        mediaSession!!.setCallback(mPlaybackManager!!.mediaSessionCallback)
        mediaSession!!.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        val mSessionExtras = Bundle()
        mediaSession!!.setExtras(mSessionExtras)

        try {
            mediaController = MediaControllerCompat(this, mediaSession!!.sessionToken)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }

        if (mediaController != null) {
            transportControls = mediaController!!.transportControls
        }

        mBecomingNoisyReceiver = BecomingNoisyReceiver(this, transportControls)

        mPlaybackManager!!.updatePlaybackState(false, null)
        mPackageValidator = PackageValidator(this)
        //通知栏相关
        val constructor = MusicManager.instance.constructor
        mNotificationFactory = NotificationFactory(this, constructor)
        mNotificationFactory!!.createNotification()
        mPlaybackManager!!.setNotificationFactory(mNotificationFactory!!)
    }

    override fun onStartCommand(startIntent: Intent?, flags: Int, startId: Int): Int {
        if (startIntent != null) {
            //you can do something
        }
        mDelayedStopHandler.removeCallbacksAndMessages(null)
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY.toLong())
        return Service.START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        mPlaybackManager!!.handleStopRequest(null)
        mNotificationFactory!!.stopNotification()

        mDelayedStopHandler.removeCallbacksAndMessages(null)

        mediaSession!!.release()
    }

    /**
     * 媒体信息更新时回调
     */
    override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
        mediaSession!!.setMetadata(metadata)
    }

    /**
     * 当前播放媒体为 null 时回调
     */
    override fun onMetadataRetrieveError() {
        mPlaybackManager!!.updatePlaybackState(false, "Unable to retrieve metadata")
    }

    /**
     * 播放下标更新时回调
     */
    override fun onCurrentQueueIndexUpdated(queueIndex: Int) {
        mPlaybackManager!!.handlePlayRequest()
    }

    /**
     * 播放队列更新时回调
     */
    override fun onQueueUpdated(newQueue: List<MediaSessionCompat.QueueItem>) {
        mediaSession!!.setQueue(newQueue)
    }

    /**
     * 播放时回调
     */
    override fun onPlaybackStart() {
        mediaSession!!.isActive = true
        mDelayedStopHandler.removeCallbacksAndMessages(null)
        startService(Intent(applicationContext, MusicService::class.java))
    }

    /**
     * 状态是播放或暂停时回调
     */
    override fun onNotificationRequired() {
        mNotificationFactory!!.startNotification()
    }

    /**
     * 暂停或停止时回调
     */
    override fun onPlaybackStop() {
        mediaSession!!.isActive = false
        mDelayedStopHandler.removeCallbacksAndMessages(null)
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY.toLong())
        stopForeground(true)
    }

    /**
     * 播放状态改变时回调
     */
    override fun onPlaybackStateUpdated(newState: PlaybackStateCompat, currMetadata: MediaMetadataCompat?) {
        mediaSession!!.setPlaybackState(newState)

        if (newState.state == PlaybackStateCompat.STATE_BUFFERING || newState.state == PlaybackStateCompat.STATE_PLAYING) {
            mBecomingNoisyReceiver!!.register()
        } else {
            mBecomingNoisyReceiver!!.unregister()
        }
    }

    /**
     * 更新播放顺序
     */
    override fun onShuffleModeUpdated(shuffleMode: Int) {
        mediaSession!!.setShuffleMode(shuffleMode)
    }

    /**
     * 更新播放模式
     */
    override fun onRepeatModeUpdated(repeatMode: Int) {
        mediaSession!!.setRepeatMode(repeatMode)
    }

    private class DelayedStopHandler (service: MusicService) : Handler() {
        private val mWeakReference: WeakReference<MusicService> = WeakReference(service)

        override fun handleMessage(msg: Message) {
            val service = mWeakReference.get()
            if (service != null && service.mPlaybackManager!!.playback != null) {
                if (service.mPlaybackManager!!.playback!!.isPlaying) {
                    return
                }
                service.stopSelf()
            }
        }
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): MediaBrowserServiceCompat.BrowserRoot? {
        return if (mPackageValidator!!.isKnownCaller(clientPackageName, clientUid)) {
            MediaBrowserServiceCompat.BrowserRoot(STARRYSKY_BROWSABLE_ROOT, null)
        } else {
            MediaBrowserServiceCompat.BrowserRoot(STARRYSKY_EMPTY_ROOT, null)
        }
    }

    override fun onLoadChildren(parentId: String, result: MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>>) {
        //可以不做任何事情
    }

    /**
     * 拔下耳机时暂停，具体意思可参考 AudioManager.ACTION_AUDIO_BECOMING_NOISY
     */
    private class BecomingNoisyReceiver(private val context: Context, private val transportControls: MediaControllerCompat.TransportControls?) : BroadcastReceiver() {
        private val noisyIntentFilter: IntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        private var registered = false

        fun register() {
            if (!registered) {
                context.registerReceiver(this, noisyIntentFilter)
                registered = true
            }
        }

        fun unregister() {
            if (registered) {
                context.unregisterReceiver(this)
                registered = false
            }
        }

        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                transportControls?.pause()
            }
        }
    }

    companion object {

        val UPDATE_PARENT_ID = "update"
        private val STARRYSKY_BROWSABLE_ROOT = "/"
        private val STARRYSKY_EMPTY_ROOT = "@empty@"

        private val STOP_DELAY = 30000
    }
}
