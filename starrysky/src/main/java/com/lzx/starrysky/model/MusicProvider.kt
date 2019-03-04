package com.lzx.starrysky.model

import android.graphics.Bitmap
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.text.TextUtils
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit


/**
 * 媒体信息提供类
 */
class MusicProvider private constructor() {

    //使用Map在查找方面会效率高一点
    private val mSongInfoListById: ConcurrentMap<String, SongInfo>
    private var mMusicListById: ConcurrentMap<String, MediaMetadataCompat>? = null

    /**
     * 获取List#SongInfo
     */
    /**
     * 设置播放列表
     */
    var songInfos: List<SongInfo>
        get() = ArrayList(mSongInfoListById.values)
        @Synchronized set(songInfos) {
            mSongInfoListById.clear()
            for (info in songInfos) {
                mSongInfoListById[info.songId!!] = info
            }
            mMusicListById = toMediaMetadata(songInfos)
        }

    /**
     * 获取List#MediaMetadataCompat
     */
    val musicList: List<MediaMetadataCompat>
        get() = ArrayList(mMusicListById!!.values)

    /**
     * 获取 List#MediaBrowserCompat.MediaItem 用于 onLoadChildren 回调
     */
    val childrenResult: List<MediaBrowserCompat.MediaItem>
        get() {
            val mediaItems = ArrayList<MediaBrowserCompat.MediaItem>()
            val list = ArrayList(mMusicListById!!.values)
            for (metadata in list) {
                val mediaItem = MediaBrowserCompat.MediaItem(
                        metadata.description,
                        MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
                mediaItems.add(mediaItem)
            }
            return mediaItems
        }

    /**
     * 获取乱序列表
     */
    val shuffledMusic: Iterable<MediaMetadataCompat>
        get() {
            val shuffled = ArrayList<MediaMetadataCompat>(mMusicListById!!.size)
            shuffled.addAll(mMusicListById!!.values)
            shuffled.shuffle()
            return shuffled
        }

    private object SingletonHolder {
        val sInstance = MusicProvider()
    }

    init {
        mSongInfoListById = ConcurrentHashMap()
        mMusicListById = ConcurrentHashMap()
    }

    /**
     * 添加一首歌
     */
    @Synchronized
    fun addSongInfo(songInfo: SongInfo) {
        mSongInfoListById[songInfo.songId!!] = songInfo
        mMusicListById!![songInfo.songId!!] = toMediaMetadata(songInfo)
    }

    /**
     * 根据检查是否有某首音频
     */
    fun hasSongInfo(songId: String): Boolean {
        return mSongInfoListById.containsKey(songId)
    }

    /**
     * 根据songId获取SongInfo
     */
    fun getSongInfo(songId: String): SongInfo? {
        return if (mSongInfoListById.containsKey(songId)) {
            mSongInfoListById[songId]
        } else {
            null
        }
    }

    /**
     * 根据songId获取索引
     */
    fun getIndexBySongInfo(songId: String): Int {
        val songInfo = getSongInfo(songId)
        return if (songInfo != null) songInfos.indexOf(songInfo) else -1
    }

    /**
     * 根据id获取对应的MediaMetadataCompat对象
     */
    fun getMusic(songId: String): MediaMetadataCompat? {
        return if (mMusicListById!!.containsKey(songId)) mMusicListById!![songId] else null
    }

    /**
     * 更新封面art
     */
    @Synchronized
    fun updateMusicArt(songId: String, changeData: MediaMetadataCompat, albumArt: Bitmap, icon: Bitmap) {
        val metadata = MediaMetadataCompat.Builder(changeData)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, icon)
                .build()
        mMusicListById!![songId] = metadata
    }

    companion object {

        val instance: MusicProvider
            get() = SingletonHolder.sInstance

        /**
         * List<SongInfo> 转 ConcurrentMap<String></String>, MediaMetadataCompat>
        </SongInfo> */
        @Synchronized
        private fun toMediaMetadata(songInfos: List<SongInfo>): ConcurrentMap<String, MediaMetadataCompat> {
            val map = ConcurrentHashMap<String, MediaMetadataCompat>()
            for (info in songInfos) {
                val metadataCompat = toMediaMetadata(info)
                map[info.songId!!] = metadataCompat
            }
            return map
        }

        /**
         * SongInfo 转 MediaMetadataCompat
         */
        @Synchronized
        private fun toMediaMetadata(info: SongInfo): MediaMetadataCompat {
            var albumTitle: String? = ""
            if (!TextUtils.isEmpty(info.albumName)) {
                albumTitle = info.albumName
            } else if (!TextUtils.isEmpty(info.songName)) {
                albumTitle = info.songName
            }
            var songCover: String? = ""
            if (!TextUtils.isEmpty(info.songCover)) {
                songCover = info.songCover
            } else if (!TextUtils.isEmpty(info.albumCover)) {
                songCover = info.albumCover
            }
            val builder = MediaMetadataCompat.Builder()
            builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, info.songId)
            builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, info.songUrl)
            if (!TextUtils.isEmpty(albumTitle)) {
                builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, albumTitle)
            }
            if (!TextUtils.isEmpty(info.artist)) {
                builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, info.artist)
            }
            if (info.duration != -1L) {
                val durationMs = TimeUnit.SECONDS.toMillis(info.duration)
                builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs)
            }
            if (!TextUtils.isEmpty(info.genre)) {
                builder.putString(MediaMetadataCompat.METADATA_KEY_GENRE, info.genre)
            }
            if (!TextUtils.isEmpty(songCover)) {
                builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, songCover)
            }
            if (!TextUtils.isEmpty(info.songName)) {
                builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, info.songName)
            }
            if (info.trackNumber != -1) {
                builder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, info.trackNumber.toLong())
            }
            builder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, info.albumSongCount.toLong())
            return builder.build()
        }
    }
}
