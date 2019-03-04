package com.lzx.starrysky.manager


import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import com.lzx.starrysky.model.MusicProvider
import com.lzx.starrysky.model.SongInfo
import com.lzx.starrysky.notification.NotificationConstructor
import com.lzx.starrysky.notification.factory.INotification
import com.lzx.starrysky.playback.ExoPlayback
import com.lzx.starrysky.playback.Playback
import com.lzx.starrysky.playback.download.ExoDownload
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 用户操作管理类
 */
class MusicManager private constructor() {
    /**
     * 获取通知栏配置，如果为 null ,则不创建通知栏
     *
     * @return
     */
    var constructor: NotificationConstructor? = null
        private set
    val playerEventListeners = CopyOnWriteArrayList<OnPlayerEventListener>()
    var playback: Playback? = null

    /**
     * 获取播放模式
     */
    /**
     * 设置播放模式
     * 必须是以下之一：
     * PlaybackStateCompat.SHUFFLE_MODE_NONE 顺序播放
     * PlaybackStateCompat.SHUFFLE_MODE_ALL  随机播放
     */
    var shuffleMode: Int
        get() {
            val connection = MediaSessionConnection.instance
            return if (connection!!.isConnected) {
                connection.mediaController!!.shuffleMode
            } else -1
        }
        set(shuffleMode) {
            val connection = MediaSessionConnection.instance
            if (connection!!.isConnected) {
                connection.transportControls!!.setShuffleMode(shuffleMode)
            }
        }

    /**
     * 获取播放模式,默认顺序播放
     */
    /**
     * 设置播放模式
     * 必须是以下之一：
     * PlaybackStateCompat.REPEAT_MODE_NONE  顺序播放
     * PlaybackStateCompat.REPEAT_MODE_ONE   单曲循环
     * PlaybackStateCompat.REPEAT_MODE_ALL   列表循环
     */
    var repeatMode: Int
        get() {
            val connection = MediaSessionConnection.instance
            return if (connection!!.isConnected) {
                connection.mediaController!!.repeatMode
            } else -1
        }
        set(repeatMode) {
            val connection = MediaSessionConnection.instance
            if (connection!!.isConnected) {
                connection.transportControls!!.setRepeatMode(repeatMode)
            }
        }

    /**
     * 获取播放列表
     */
    val playList: List<SongInfo>
        get() = MusicProvider.instance.songInfos

    /**
     * 获取当前播放的歌曲信息
     */
    val nowPlayingSongInfo: SongInfo?
        get() {
            var songInfo: SongInfo? = null
            val connection = MediaSessionConnection.instance
            if (connection!!.isConnected) {
                val metadataCompat = connection.nowPlaying
                if (metadataCompat != null) {
                    val songId = metadataCompat.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                    songInfo = MusicProvider.instance.getSongInfo(songId)
                }
            }
            return songInfo
        }

    /**
     * 获取当前播放的歌曲songId
     */
    val nowPlayingSongId: String
        get() {
            var songId = ""
            val connection = MediaSessionConnection.instance
            if (connection!!.isConnected) {
                val metadataCompat = connection.nowPlaying
                if (metadataCompat != null) {
                    songId = metadataCompat.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                }
            }
            return songId
        }

    /**
     * 获取当前播放歌曲的下标
     */
    val nowPlayingIndex: Int
        get() {
            var index = -1
            val songId = nowPlayingSongId
            if (!TextUtils.isEmpty(songId)) {
                index = MusicProvider.instance.getIndexBySongInfo(songId)
            }
            return index
        }

    /**
     * 以ms为单位获取当前缓冲的位置。
     */
    val bufferedPosition: Long
        get() {
            val connection = MediaSessionConnection.instance
            return if (connection!!.isConnected) {
                if (playback != null) playback!!.bufferedPosition else 0
            } else {
                0
            }
        }

    /**
     * 获取播放位置 毫秒为单位。
     */
    val playingPosition: Long
        get() {
            val connection = MediaSessionConnection.instance
            return if (connection!!.isConnected) {
                if (playback != null) playback!!.currentStreamPosition else 0
            } else {
                0
            }
        }

    /**
     * 是否有下一首
     */
    val isSkipToNextEnabled: Boolean
        get() {
            val connection = MediaSessionConnection.instance
            return if (connection!!.isConnected) {
                connection.playbackState.actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L
            } else {
                false
            }
        }

    /**
     * 是否有上一首
     */
    val isSkipToPreviousEnabled: Boolean
        get() {
            val connection = MediaSessionConnection.instance
            return if (connection!!.isConnected) {
                connection.playbackState.actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L
            } else {
                false
            }
        }

    /**
     * 将当前播放速度作为正常播放的倍数。 倒带时这应该是负数， 值为1表示正常播放，0表示暂停。
     */
    val playbackSpeed: Float
        get() {
            val connection = MediaSessionConnection.instance
            return if (connection!!.isConnected) {
                connection.playbackState.playbackSpeed
            } else {
                -1f
            }
        }

    /**
     * 获取底层框架[android.media.session.PlaybackState]对象。
     * 此方法仅在API 21+上受支持。
     */
    val playbackState: Any?
        get() {
            val connection = MediaSessionConnection.instance
            return if (connection!!.isConnected) {
                connection.playbackState.playbackState
            } else {
                null
            }
        }

    /**
     * 获取发送错误时的错误信息
     */
    val errorMessage: CharSequence
        get() {
            val connection = MediaSessionConnection.instance
            return if (connection!!.isConnected) {
                connection.playbackState.errorMessage
            } else {
                "connection is not connect"
            }
        }

    /**
     * 获取发送错误时的错误码
     * 0 : 这是默认的错误代码
     * 1 : 当应用程序状态无效以满足请求时的错误代码。
     * 2 : 应用程序不支持请求时的错误代码。
     * 3 : 由于身份验证已过期而无法执行请求时出现错误代码。
     * 4 : 成功请求需要高级帐户时的错误代码。
     * 5 : 检测到太多并发流时的错误代码。
     * 6 : 由于家长控制而阻止内容时出现错误代码。
     * 7 : 内容因区域不可用而被阻止时的错误代码。
     * 8 : 请求的内容已在播放时出现错误代码。
     * 9 : 当应用程序无法跳过任何更多歌曲时出现错误代码，因为已达到跳过限制。
     * 10: 由于某些外部事件而导致操作中断时的错误代码。
     * 11: 由于队列耗尽而无法播放导航（上一个，下一个）时出现错误代码。
     */
    val errorCode: Int
        get() {
            val connection = MediaSessionConnection.instance
            return if (connection!!.isConnected) {
                connection.playbackState.errorCode
            } else {
                -1
            }
        }

    /**
     * 获取当前的播放状态。 以下之一：
     * PlaybackStateCompat.STATE_NONE                   默认播放状态，表示尚未添加媒体，或者表示已重置且无内容可播放。
     * PlaybackStateCompat.STATE_STOPPED                当前已停止。
     * PlaybackStateCompat.STATE_PLAYING                正在播放
     * PlaybackStateCompat.STATE_PAUSED                 已暂停
     * PlaybackStateCompat.STATE_FAST_FORWARDING        当前正在快进
     * PlaybackStateCompat.STATE_REWINDING              当前正在倒带
     * PlaybackStateCompat.STATE_BUFFERING              当前正在缓冲
     * PlaybackStateCompat.STATE_ERROR                  当前处于错误状态
     * PlaybackStateCompat.STATE_CONNECTING             正在连接中
     * PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS   正在转跳到上一首
     * PlaybackStateCompat.STATE_SKIPPING_TO_NEXT       正在转跳到下一首
     * PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM 正在切歌
     */
    val state: Int
        get() {
            val connection = MediaSessionConnection.instance
            return if (connection!!.isConnected) {
                connection.playbackState.state
            } else {
                -1
            }
        }

    /**
     * 比较方便的判断当前媒体是否在播放
     */
    val isPlaying: Boolean
        get() = state == PlaybackStateCompat.STATE_PLAYING

    /**
     * 比较方便的判断当前媒体是否暂停中
     */
    val isPaused: Boolean
        get() = state == PlaybackStateCompat.STATE_PAUSED

    /**
     * 比较方便的判断当前媒体是否空闲
     */
    val isIdea: Boolean
        get() = state == PlaybackStateCompat.STATE_NONE

    /**
     * 获取音量
     */
    /**
     * 设置音量
     */
    var volume: Float
        get() {
            val connection = MediaSessionConnection.instance
            return if (connection!!.isConnected) {
                if (playback != null) playback!!.volume else -1F
            } else {
                -1f
            }
        }
        set(audioVolume) {
            var audioVolume = audioVolume
            val connection = MediaSessionConnection.instance
            if (connection!!.isConnected) {
                if (audioVolume < 0) {
                    audioVolume = 0f
                }
                if (audioVolume > 1) {
                    audioVolume = 1f
                }
                val bundle = Bundle()
                bundle.putFloat("AudioVolume", audioVolume)
                connection.mediaController!!.sendCommand(ExoPlayback.ACTION_CHANGE_VOLUME, bundle, null)
            }
        }

    /**
     * 获取媒体时长，单位毫秒
     */
    //如果没设置duration
    val duration: Long
        get() {
            var duration: Long = -1
            val connection = MediaSessionConnection.instance
            if (connection!!.isConnected) {
                duration = connection.nowPlaying.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                if (duration == 0L) {
                    if (playback != null) {
                        duration = playback!!.duration
                    }
                }
            }
            return duration
        }

    private object SingletonHolder {
        val sInstance = MusicManager()
    }

    /**
     * 释放资源，关闭程序时调用
     */
    fun onRelease() {
        clearPlayerEventListener()
        sContext = null
        playback = null
        constructor = null
    }

    /**
     * 设置通知栏配置,在Application创建并调用
     */
    fun setNotificationConstructor(constructor: NotificationConstructor) {
        this.constructor = constructor
    }

    /**
     * 根据songId播放,调用前请确保已经设置了播放列表
     */
    fun playMusicById(songId: String) {
        val connection = MediaSessionConnection.instance
        if (connection!!.isConnected) {
            if (MusicProvider.instance.hasSongInfo(songId)) {
                connection.transportControls!!.playFromMediaId(songId, null)
            }
        }
    }

    /**
     * 根据 SongInfo 播放，实际也是根据 songId 播放
     */
    fun playMusicByInfo(info: SongInfo) {
        val connection = MediaSessionConnection.instance
        if (connection!!.isConnected) {
            MusicProvider.instance.addSongInfo(info)
            connection.transportControls!!.playFromMediaId(info.songId, null)
        }
    }

    /**
     * 根据要播放的歌曲在播放列表中的下标播放,调用前请确保已经设置了播放列表
     */
    fun playMusicByIndex(index: Int) {
        val connection = MediaSessionConnection.instance
        if (connection!!.isConnected) {
            val list = MusicProvider.instance.songInfos
            if (index >= 0 && index < list.size) {
                connection.transportControls!!.playFromMediaId(list[index].songId, null)
            }
        }
    }

    /**
     * 播放
     *
     * @param songInfos 播放列表
     * @param index     要播放的歌曲在播放列表中的下标
     */
    fun playMusic(songInfos: List<SongInfo>, index: Int) {
        val connection = MediaSessionConnection.instance
        if (connection!!.isConnected) {
            MusicProvider.instance.songInfos = songInfos
            connection.transportControls!!.playFromMediaId(songInfos[index].songId, null)
        }
    }

    /**
     * 暂停
     */
    fun pauseMusic() {
        val connection = MediaSessionConnection.instance
        if (connection!!.isConnected) {
            connection.transportControls!!.pause()
        }
    }

    /**
     * 恢复播放
     */
    fun playMusic() {
        val connection = MediaSessionConnection.instance
        if (connection!!.isConnected) {
            connection.transportControls!!.play()
        }
    }

    /**
     * 停止播放
     */
    fun stopMusic() {
        val connection = MediaSessionConnection.instance
        if (connection!!.isConnected) {
            connection.transportControls!!.stop()
        }
    }

    /**
     * 准备播放
     */
    fun prepare() {
        val connection = MediaSessionConnection.instance
        if (connection!!.isConnected) {
            connection.transportControls!!.prepare()
        }
    }

    /**
     * 准备播放，根据songId
     */
    fun prepareFromSongId(songId: String) {
        val connection = MediaSessionConnection.instance
        if (connection!!.isConnected) {
            connection.transportControls!!.prepareFromMediaId(songId, null)
        }
    }

    /**
     * 下一首
     */
    fun skipToNext() {
        val connection = MediaSessionConnection.instance
        if (connection!!.isConnected) {
            connection.transportControls!!.skipToNext()
        }
    }

    /**
     * 上一首
     */
    fun skipToPrevious() {
        val connection = MediaSessionConnection.instance
        if (connection!!.isConnected) {
            connection.transportControls!!.skipToPrevious()
        }
    }

    /**
     * 开始快进，每调一次加 0.5 倍
     */
    fun fastForward() {
        val connection = MediaSessionConnection.instance
        if (connection!!.isConnected) {
            connection.transportControls!!.fastForward()
        }
    }

    /**
     * 开始倒带 每调一次减 0.5 倍，最小为 0
     */
    fun rewind() {
        val connection = MediaSessionConnection.instance
        if (connection!!.isConnected) {
            connection.transportControls!!.rewind()
        }
    }

    /**
     * 移动到媒体流中的新位置,以毫秒为单位。
     */
    fun seekTo(pos: Long) {
        val connection = MediaSessionConnection.instance
        if (connection!!.isConnected) {
            connection.transportControls!!.seekTo(pos)
        }
    }

    /**
     * 更新播放列表
     */
    fun updatePlayList(songInfos: List<SongInfo>) {
        val connection = MediaSessionConnection.instance
        if (connection!!.isConnected) {
            MusicProvider.instance.songInfos = songInfos
        }
    }

    /**
     * 判断传入的音乐是不是正在播放的音乐
     */
    fun isCurrMusicIsPlayingMusic(songId: String): Boolean {
        if (TextUtils.isEmpty(songId)) {
            return false
        } else {
            val playingMusic = nowPlayingSongInfo
            return playingMusic != null && songId == playingMusic.songId
        }
    }

    /**
     * 判断传入的音乐是否正在播放
     */
    fun isCurrMusicIsPlaying(songId: String): Boolean {
        return isCurrMusicIsPlayingMusic(songId) && isPlaying
    }

    /**
     * 判断传入的音乐是否正在暂停
     */
    fun isCurrMusicIsPaused(songId: String): Boolean {
        return isCurrMusicIsPlayingMusic(songId) && isPaused
    }

    /**
     * 更新通知栏喜欢或收藏按钮UI
     */
    fun updateFavoriteUI(isFavorite: Boolean) {
        val connection = MediaSessionConnection.instance
        if (connection!!.isConnected) {
            val bundle = Bundle()
            bundle.putBoolean("isFavorite", isFavorite)
            connection.mediaController!!.sendCommand(INotification.ACTION_UPDATE_FAVORITE_UI, bundle, null)
        }
    }

    /**
     * 更新通知栏歌词按钮UI
     */
    fun updateLyricsUI(isChecked: Boolean) {
        val connection = MediaSessionConnection.instance
        if (connection!!.isConnected) {
            val bundle = Bundle()
            bundle.putBoolean("isChecked", isChecked)
            connection.mediaController!!.sendCommand(INotification.ACTION_UPDATE_LYRICS_UI, bundle, null)
        }
    }

    /**
     * 扫描本地媒体信息
     */
    fun querySongInfoInLocal(): List<SongInfo> {
        val songInfos = ArrayList<SongInfo>()
        val cursor = sContext!!.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null)
                ?: return songInfos
        while (cursor.moveToNext()) {
            val song = SongInfo()
            song.albumId = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID))
            song.albumCover = getAlbumArtPicPath(sContext!!, song.albumId)
            song.songNameKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE_KEY))
            song.artistKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST_KEY))
            song.albumNameKey = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_KEY))
            song.artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST))
            song.albumName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM))
            song.songUrl = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA))
            song.description = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DISPLAY_NAME))
            song.songName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE))
            song.mimeType = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.MIME_TYPE))
            song.year = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.YEAR)).toString()
            song.duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION))
            song.size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.SIZE)).toString()
            song.publishTime = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATE_ADDED)).toString()
            song.modifiedTime = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATE_MODIFIED)).toString()
            songInfos.add(song)
        }
        cursor.close()
        return songInfos
    }

    @Synchronized
    private fun getAlbumArtPicPath(context: Context, albumId: String?): String? {
        // 小米应用商店检测crash ，错误信息：[31188,0,com.duan.musicoco,13155908,java.lang.IllegalStateException,Unknown URL: content://media/external/audio/albums/null,Parcel.java,1548]
        if (TextUtils.isEmpty(albumId)) {
            return null
        }
        val projection = arrayOf(MediaStore.Audio.Albums.ALBUM_ART)
        var imagePath: String? = null
        val uri = Uri.parse("content://media" + MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI.path + "/" + albumId)
        val cur = context.contentResolver.query(uri, projection, null, null, null) ?: return null
        if (cur.count > 0 && cur.columnCount > 0) {
            cur.moveToNext()
            imagePath = cur.getString(0)
        }
        cur.close()
        return imagePath
    }

    /**
     * 添加一个状态监听
     */
    fun addPlayerEventListener(listener: OnPlayerEventListener?) {
        if (listener != null) {
            if (!playerEventListeners.contains(listener)) {
                playerEventListeners.add(listener)
            }
        }
    }

    /**
     * 删除一个状态监听
     */
    fun removePlayerEventListener(listener: OnPlayerEventListener?) {
        if (listener != null) {
            playerEventListeners.remove(listener)
        }
    }

    /**
     * 删除所有状态监听
     */
    fun clearPlayerEventListener() {
        playerEventListeners.clear()
    }

    companion object {

        @SuppressLint("StaticFieldLeak")
        private var sContext: Context? = null

        val instance: MusicManager
            get() = SingletonHolder.sInstance

        /**
         * 在Application调用
         */
        fun initMusicManager(context: Context) {
            sContext = context
            ExoDownload.initExoDownload(sContext!!)
            MediaSessionConnection.initConnection(sContext!!)
        }
    }
}
