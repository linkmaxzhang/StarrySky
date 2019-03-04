/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lzx.starrysky.playback

import android.content.Context
import android.os.Bundle
import android.os.ResultReceiver
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

import com.lzx.starrysky.manager.MusicManager
import com.lzx.starrysky.model.MusicProvider
import com.lzx.starrysky.notification.factory.INotification
import com.lzx.starrysky.notification.factory.NotificationFactory


/**
 * 播放管理类
 */
class PlaybackManager(private val mContext: Context, private val mServiceCallback: PlaybackServiceCallback, private val mQueueManager: QueueManager,
                      val playback: Playback?) : Playback.Callback {
    private val mMediaSessionCallback: MediaSessionCallback
    private var mNotificationFactory: NotificationFactory? = null
    private var currRepeatMode: Int = 0
    private var shouldPlayNext = true //是否可以播放下一首
    private var shouldPlayPre = false  //是否可以播放上一首
    private var stateBuilder: PlaybackStateCompat.Builder? = null

    val mediaSessionCallback: MediaSessionCompat.Callback
        get() = mMediaSessionCallback

    /**
     * 获取状态
     */
    private val availableActions: Long
        get() {
            var actions = PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                    PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            if (playback!!.isPlaying) {
                actions = actions or PlaybackStateCompat.ACTION_PAUSE
            } else {
                actions = actions or PlaybackStateCompat.ACTION_PLAY
            }
            if (!shouldPlayNext) {
                if (actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L) {
                    actions = actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT.inv()
                }
            } else {
                if (actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT == 0L) {
                    actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                }
            }
            if (!shouldPlayPre) {
                if (actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L) {
                    actions = actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS.inv()
                }
            } else {
                if (actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS == 0L) {
                    actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                }
            }
            return actions
        }

    init {
        mMediaSessionCallback = MediaSessionCallback()
        this.playback!!.setCallback(this)
        MusicManager.instance.playback = this.playback
        currRepeatMode = PlaybackStateCompat.REPEAT_MODE_NONE
    }

    fun setNotificationFactory(notificationFactory: NotificationFactory) {
        mNotificationFactory = notificationFactory
    }

    /**
     * 播放
     */
    fun handlePlayRequest() {
        val currentMusic = mQueueManager.currentMusic
        if (currentMusic != null) {
            mServiceCallback.onPlaybackStart()
            playback!!.play(currentMusic)
        }
    }

    /**
     * 暂停
     */
    fun handlePauseRequest() {
        if (playback!!.isPlaying) {
            playback.pause()
            mServiceCallback.onPlaybackStop()
        }
    }

    /**
     * 停止
     */
    fun handleStopRequest(withError: String?) {
        playback!!.stop(true)
        mServiceCallback.onPlaybackStop()
        updatePlaybackState(false, withError)
    }

    /**
     * 快进
     */
    fun handleFastForward() {
        playback!!.onFastForward()
    }

    /**
     * 倒带
     */
    fun handleRewind() {
        playback!!.onRewind()
    }

    /**
     * 更新播放状态
     */
    fun updatePlaybackState(isOnlyUpdateActions: Boolean, error: String?) {
        if (isOnlyUpdateActions && stateBuilder != null) {
            //单独更新 Actions
            stateBuilder!!.setActions(availableActions)
            mServiceCallback.onPlaybackStateUpdated(stateBuilder!!.build(), null)
        } else {
            var position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN
            if (playback != null && playback.isConnected) {
                position = playback.currentStreamPosition
            }
            //构建一个播放状态对象
            stateBuilder = PlaybackStateCompat.Builder()
                    .setActions(availableActions)
            //获取播放器播放状态
            var state = playback!!.state
            //如果错误信息不为 null 的时候，播放状态设为 STATE_ERROR
            if (error != null) {
                stateBuilder!!.setErrorMessage(error)
                state = PlaybackStateCompat.STATE_ERROR
            }
            //设置播放状态
            stateBuilder!!.setState(state, position, 1.0f, SystemClock.elapsedRealtime())
            //设置当前活动的 songId
            val currentMusic = mQueueManager.currentMusic
            var currMetadata: MediaMetadataCompat? = null
            if (currentMusic != null) {
                stateBuilder!!.setActiveQueueItemId(currentMusic.queueId)
                val musicId = currentMusic.description.mediaId
                currMetadata = MusicProvider.instance.getMusic(musicId!!)
            }
            //把状态回调出去
            mServiceCallback.onPlaybackStateUpdated(stateBuilder!!.build(), currMetadata)
            //如果是播放或者暂停的状态，更新一下通知栏
            if (state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_PAUSED) {
                mServiceCallback.onNotificationRequired()
            }
        }
    }

    /**
     * 播放器播放完成回调
     */
    override fun onCompletion() {
        updatePlaybackState(false, null)

        if (currRepeatMode == PlaybackStateCompat.REPEAT_MODE_NONE) {
            //顺序播放
            shouldPlayNext = mQueueManager.currentIndex != mQueueManager.currentQueueSize - 1 && mQueueManager.skipQueuePosition(1)
            if (shouldPlayNext) {
                handlePlayRequest()
                mQueueManager.updateMetadata()
            } else {
                handleStopRequest(null)
            }
        } else if (currRepeatMode == PlaybackStateCompat.REPEAT_MODE_ONE) {
            //单曲播放
            shouldPlayNext = false
            playback!!.currentMediaId = ""
            handlePlayRequest()
        } else if (currRepeatMode == PlaybackStateCompat.REPEAT_MODE_ALL) {
            //列表循环
            shouldPlayNext = mQueueManager.skipQueuePosition(1)
            if (shouldPlayNext) {
                handlePlayRequest()
                mQueueManager.updateMetadata()
            } else {
                handleStopRequest(null)
            }
        }
    }

    /**
     * 播放器播放状态改变回调
     */
    override fun onPlaybackStatusChanged(state: Int) {
        updatePlaybackState(false, null)
    }

    /**
     * 播放器发送错误回调
     */
    override fun onError(error: String) {
        updatePlaybackState(false, error)
    }

    /**
     * 设置当前播放 id
     */
    override fun setCurrentMediaId(mediaId: String) {
        mQueueManager.setQueueFromMusic(mediaId)
    }

    /**
     * MusicManager API 方法的具体实现
     */
    private inner class MediaSessionCallback : MediaSessionCompat.Callback() {

        override fun onPlay() {
            if (mQueueManager.currentMusic == null) {
                mQueueManager.setRandomQueue()
            }
            handlePlayRequest()
        }

        override fun onSkipToQueueItem(queueId: Long) {
            mQueueManager.setCurrentQueueItem(queueId.toString())
            mQueueManager.updateMetadata()
        }

        override fun onSeekTo(position: Long) {
            playback!!.seekTo(position.toInt().toLong())
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            mQueueManager.setQueueFromMusic(mediaId!!)
            handlePlayRequest()
        }

        override fun onPause() {
            handlePauseRequest()
        }

        override fun onStop() {
            handleStopRequest(null)
        }

        override fun onSkipToNext() {
            if (currRepeatMode == PlaybackStateCompat.REPEAT_MODE_NONE) {
                //顺序播放
                shouldPlayNext = mQueueManager.currentIndex != mQueueManager.currentQueueSize - 1 && mQueueManager.skipQueuePosition(1)
            } else {
                shouldPlayNext = mQueueManager.skipQueuePosition(1)
            }
            if (shouldPlayNext) {
                //当前的媒体如果是在倒数第二首点击到最后一首的时候，如果不重新判断，会用于为 true
                if (currRepeatMode == PlaybackStateCompat.REPEAT_MODE_NONE) {
                    shouldPlayNext = mQueueManager.currentIndex != mQueueManager.currentQueueSize - 1
                }
                shouldPlayPre = true
                handlePlayRequest()
                mQueueManager.updateMetadata()
            }
        }

        override fun onSkipToPrevious() {
            if (currRepeatMode == PlaybackStateCompat.REPEAT_MODE_NONE) {
                shouldPlayPre = mQueueManager.currentIndex != 0 && mQueueManager.skipQueuePosition(-1)
            } else {
                shouldPlayPre = mQueueManager.skipQueuePosition(-1)
            }
            if (shouldPlayPre) {
                //当前的媒体如果是在第二首点击到上一首的时候，如果不重新判断，会用于为 true
                if (currRepeatMode == PlaybackStateCompat.REPEAT_MODE_NONE) {
                    shouldPlayPre = mQueueManager.currentIndex != 0
                }
                shouldPlayNext = true
                handlePlayRequest()
                mQueueManager.updateMetadata()
            }
        }

        override fun onCustomAction(action: String, extras: Bundle?) {
            //updatePlaybackState(null);
        }

        override fun onFastForward() {
            super.onFastForward()
            handleFastForward()
        }

        override fun onRewind() {
            super.onRewind()
            handleRewind()
        }

        override fun onSetShuffleMode(shuffleMode: Int) {
            super.onSetShuffleMode(shuffleMode)
            mQueueManager.setQueueByShuffleMode(shuffleMode)
            mServiceCallback.onShuffleModeUpdated(shuffleMode)
        }

        override fun onSetRepeatMode(repeatMode: Int) {
            super.onSetRepeatMode(repeatMode)
            currRepeatMode = repeatMode
            mServiceCallback.onRepeatModeUpdated(repeatMode)
            if (currRepeatMode == PlaybackStateCompat.REPEAT_MODE_NONE) {
                shouldPlayNext = mQueueManager.currentIndex != mQueueManager.currentQueueSize - 1
                shouldPlayPre = mQueueManager.currentIndex != 0
            } else {
                shouldPlayNext = true
                shouldPlayPre = true
            }
            updatePlaybackState(true, null)  //更新状态
        }

        /**
         * 自定义方法
         */
        override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
            super.onCommand(command, extras, cb)
            if (command == null) {
                return
            }
            if (INotification.ACTION_UPDATE_FAVORITE_UI == command) {
                val isFavorite = extras!!.getBoolean("isFavorite")
                if (mNotificationFactory != null) {
                    mNotificationFactory!!.updateFavoriteUI(isFavorite)
                }
            }
            if (INotification.ACTION_UPDATE_LYRICS_UI == command) {
                val isChecked = extras!!.getBoolean("isChecked")
                if (mNotificationFactory != null) {
                    mNotificationFactory!!.updateLyricsUI(isChecked)
                }
            }
            if (ExoPlayback.ACTION_CHANGE_VOLUME == command) {
                val audioVolume = extras!!.getFloat("AudioVolume")
                playback!!.volume = audioVolume
            }
        }
    }

    interface PlaybackServiceCallback {
        fun onPlaybackStart()

        fun onNotificationRequired()

        fun onPlaybackStop()

        fun onPlaybackStateUpdated(newState: PlaybackStateCompat, currMetadata: MediaMetadataCompat?)

        fun onShuffleModeUpdated(shuffleMode: Int)

        fun onRepeatModeUpdated(repeatMode: Int)
    }

    companion object {

        private val TAG = "PlaybackManager"
    }
}
