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
import android.graphics.Bitmap
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.lzx.starrysky.R
import com.lzx.starrysky.model.MusicProvider
import java.util.*


class QueueManager(private val mContext: Context, private val mMusicProvider: MusicProvider,
                   private val mListener: MetadataUpdateListener) {

    //正在播放的队列
    private var mPlayingQueue: List<MediaSessionCompat.QueueItem>? = null
    //下标
    /**
     * 获取当前下标
     */
    var currentIndex: Int = 0
        private set

    /**
     * 获取当前播放的媒体
     */
    val currentMusic: MediaSessionCompat.QueueItem?
        get() = if (!QueueHelper.isIndexPlayable(currentIndex, mPlayingQueue)) {
            null
        } else mPlayingQueue!![currentIndex]

    /**
     * 获取队列大小
     */
    val currentQueueSize: Int
        get() = if (mPlayingQueue == null) {
            0
        } else mPlayingQueue!!.size

    init {

        mPlayingQueue = Collections.synchronizedList<MediaSessionCompat.QueueItem>(ArrayList<MediaSessionCompat.QueueItem>())
        currentIndex = 0
    }

    /**
     * 判断传入的媒体跟正在播放的媒体是否一样
     */
    fun isSameBrowsingCategory(mediaId: String): Boolean {
        val current = currentMusic ?: return false
        return mediaId == current.description.mediaId
    }

    /**
     * 更新当前下标并通知
     */
    private fun setCurrentQueueIndex(index: Int) {
        if (index >= 0 && index < mPlayingQueue!!.size) {
            currentIndex = index
            mListener.onCurrentQueueIndexUpdated(currentIndex)
        }
    }

    /**
     * 根据传入的媒体id来更新此媒体的下标并通知
     */
    fun setCurrentQueueItem(mediaId: String): Boolean {
        val index = QueueHelper.getMusicIndexOnQueue(mPlayingQueue!!, mediaId)
        setCurrentQueueIndex(index)
        return index >= 0
    }

    /**
     * 转跳下一首或上一首
     *
     * @param amount 正为下一首，负为上一首
     */
    fun skipQueuePosition(amount: Int): Boolean {
        if (mPlayingQueue!!.size == 0) {
            return false
        }
        var index = currentIndex + amount
        if (index < 0) {
            index = 0
        } else {
            index %= mPlayingQueue!!.size
        }
        if (!QueueHelper.isIndexPlayable(index, mPlayingQueue)) {
            return false
        }
        currentIndex = index
        return true
    }

    /**
     * 打乱当前的列表顺序
     */
    fun setRandomQueue() {
        setCurrentQueue(QueueHelper.getRandomQueue(mMusicProvider))
        updateMetadata()
    }

    /**
     * 如果当前模式是随机，则打乱顺序，否则恢复正常顺序
     */
    fun setQueueByShuffleMode(shuffleMode: Int) {
        if (shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_NONE) {
            setCurrentQueue(QueueHelper.getPlayingQueue(mMusicProvider))
        } else if (shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL) {
            setCurrentQueue(QueueHelper.getRandomQueue(mMusicProvider))
        }
    }

    fun setQueueFromMusic(mediaId: String) {
        var canReuseQueue = false
        if (isSameBrowsingCategory(mediaId)) {
            canReuseQueue = setCurrentQueueItem(mediaId)
        }
        if (!canReuseQueue) {
            setCurrentQueue(QueueHelper.getPlayingQueue(mMusicProvider), mediaId)
        }
        updateMetadata()
    }

    /**
     * 更新队列和下标
     */
    @JvmOverloads
    protected fun setCurrentQueue(newQueue: List<MediaSessionCompat.QueueItem>, initialMediaId: String? = null) {
        mPlayingQueue = newQueue
        var index = 0
        if (initialMediaId != null) {
            index = QueueHelper.getMusicIndexOnQueue(mPlayingQueue!!, initialMediaId)
        }
        currentIndex = Math.max(index, 0)
        mListener.onQueueUpdated(newQueue)
    }

    /**
     * 更新媒体信息
     */
    fun updateMetadata() {
        val currentMusic = currentMusic
        if (currentMusic == null) {
            mListener.onMetadataRetrieveError()
            return
        }
        val musicId = currentMusic.description.mediaId
        val metadata = mMusicProvider.getMusic(musicId!!)
                ?: throw IllegalArgumentException("Invalid musicId $musicId")
        mListener.onMetadataChanged(metadata)
        //更新封面 bitmap
        val coverUrl = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
        if (!TextUtils.isEmpty(coverUrl)) {
             Glide.with(mContext).applyDefaultRequestOptions(
                    RequestOptions()
                            .fallback(R.drawable.default_art)
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE))
                    .asBitmap().load(coverUrl).into(object : SimpleTarget<Bitmap>(144, 144) {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            mMusicProvider.updateMusicArt(musicId, metadata, resource, resource)
                            mListener.onMetadataChanged(metadata)
                        }
                    })
        }
    }

    interface MetadataUpdateListener {
        fun onMetadataChanged(metadata: MediaMetadataCompat?)

        fun onMetadataRetrieveError()

        fun onCurrentQueueIndexUpdated(queueIndex: Int)

        fun onQueueUpdated(newQueue: List<MediaSessionCompat.QueueItem>)
    }

    companion object {
        private val TAG = "QueueManager"
    }
}
/**
 * 更新队列,下标为 0
 */
