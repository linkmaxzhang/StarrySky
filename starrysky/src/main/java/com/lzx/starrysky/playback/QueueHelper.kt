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

import android.app.Activity
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.text.TextUtils
import com.lzx.starrysky.model.MusicProvider
import java.util.*


/**
 * 播放队列帮助类
 */
object QueueHelper {

    private val TAG = "QueueHelper"

    /**
     * 获取正在播放的队列
     */
    fun getPlayingQueue(musicProvider: MusicProvider): List<MediaSessionCompat.QueueItem> {
        val tracks = musicProvider.musicList
        return convertToQueue(tracks)
    }

    /**
     * 获取 id 为 mediaId 的媒体在播放队列中的下标
     */
    fun getMusicIndexOnQueue(queue: Iterable<MediaSessionCompat.QueueItem>, mediaId: String): Int {
        var index = 0
        for (item in queue) {
            if (mediaId == item.description.mediaId) {
                return index
            }
            index++
        }
        return -1
    }

    fun getMusicIndexOnQueue(queue: Iterable<MediaSessionCompat.QueueItem>, queueId: Long): Int {
        var index = 0
        for (item in queue) {
            if (queueId == item.queueId) {
                return index
            }
            index++
        }
        return -1
    }

    /**
     * List<MediaMetadataCompat> 转 List<MediaSessionCompat.QueueItem>
    </MediaSessionCompat.QueueItem></MediaMetadataCompat> */
    private fun convertToQueue(tracks: List<MediaMetadataCompat>): List<MediaSessionCompat.QueueItem> {
        val queue = ArrayList<MediaSessionCompat.QueueItem>()
        var count = 0
        for (track in tracks) {
            val trackCopy = MediaMetadataCompat.Builder(track)
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, track.description.mediaId)
                    .build()
            val item = MediaSessionCompat.QueueItem(
                    trackCopy.description, count++.toLong())
            queue.add(item)
        }
        return queue

    }

    /**
     * 获取乱序的 List#MediaSessionCompat.QueueItem
     */
    fun getRandomQueue(musicProvider: MusicProvider): List<MediaSessionCompat.QueueItem> {
        val result = ArrayList<MediaMetadataCompat>()
        val shuffled = musicProvider.shuffledMusic
        for (metadata in shuffled) {
            result.add(metadata)
        }
        return convertToQueue(result)
    }

    /**
     * 检查下标有没有越界
     */
    fun isIndexPlayable(index: Int, queue: List<MediaSessionCompat.QueueItem>?): Boolean {
        return queue != null && index >= 0 && index < queue.size
    }

    /**
     * 对比两个列表
     */
    fun equals(list1: List<MediaSessionCompat.QueueItem>?,
               list2: List<MediaSessionCompat.QueueItem>?): Boolean {
        if (list1 === list2) {
            return true
        }
        if (list1 == null || list2 == null) {
            return false
        }
        if (list1.size != list2.size) {
            return false
        }
        for (i in list1.indices) {
            if (list1[i].queueId != list2[i].queueId) {
                return false
            }
            if (!TextUtils.equals(list1[i].description.mediaId,
                            list2[i].description.mediaId)) {
                return false
            }
        }
        return true
    }


    /**
     * 判断当前的媒体是否在播放
     */
    fun isQueueItemPlaying(context: Activity, queueItem: MediaSessionCompat.QueueItem): Boolean {
        val controller = MediaControllerCompat.getMediaController(context)
        if (controller != null && controller.playbackState != null) {
            val currentPlayingQueueId = controller.playbackState.activeQueueItemId
            val currentPlayingMediaId = controller.metadata.description.mediaId
            val itemMusicId = queueItem.description.mediaId
            return (queueItem.queueId == currentPlayingQueueId
                    && currentPlayingMediaId != null
                    && TextUtils.equals(currentPlayingMediaId, itemMusicId))
        }
        return false
    }
}
