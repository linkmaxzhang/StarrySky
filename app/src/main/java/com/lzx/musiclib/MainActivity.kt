package com.lzx.musiclib


import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.lzx.starrysky.manager.MediaSessionConnection
import com.lzx.starrysky.manager.MusicManager
import com.lzx.starrysky.manager.OnPlayerEventListener
import com.lzx.starrysky.model.SongInfo
import com.lzx.starrysky.playback.download.ExoDownload
import com.lzx.starrysky.utils.TimerTaskManager
import java.util.*

/**
 * kotlin版
 */
class MainActivity : AppCompatActivity(), OnPlayerEventListener {

    private var isFavorite = false
    private var isChecked = false

    private lateinit var mTimerTask: TimerTaskManager
    private lateinit var currInfo: TextView
    private lateinit var currTime: TextView
    private lateinit var mSeekBar: SeekBar

    private var mMediaSessionConnection: MediaSessionConnection? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        currInfo = findViewById(R.id.currInfo)
        currTime = findViewById(R.id.currTime)
        mSeekBar = findViewById(R.id.seekBar)

        mTimerTask = TimerTaskManager()
        mMediaSessionConnection = MediaSessionConnection.instance

        val s1 = SongInfo()
        s1.songId = "111"
        s1.songUrl = "http://music.163.com/song/media/outer/url?id=317151.mp3&a=我"
        s1.songCover = "https://www.qqkw.com/d/file/p/2018/04-21/c24fd86006670f964e63cb8f9c129fc6.jpg"
        s1.songName = "心雨"
        s1.artist = "贤哥"

        val s2 = SongInfo()
        s2.songId = "222"
        s2.songUrl = "http://music.163.com/song/media/outer/url?id=281951.mp3"
        s2.songCover = "https://n.sinaimg.cn/sinacn13/448/w1024h1024/20180504/7b5f-fzyqqiq8228305.jpg"
        s2.songName = "我曾用心爱着你"
        s2.artist = "潘美辰"

        val s3 = SongInfo()
        s3.songId = "333"
        s3.songUrl = "http://music.163.com/song/media/outer/url?id=25906124.mp3"
        s3.songCover = "http://cn.chinadaily.com.cn/img/attachement/jpg/site1/20180211/509a4c2df41d1bea45f73b.jpg"
        s3.songName = "不要说话"
        s3.artist = "陈奕迅"

        val songInfos = ArrayList<SongInfo>()
        songInfos.add(s1)
        songInfos.add(s2)
        songInfos.add(s3)

        //播放
        findViewById<View>(R.id.play).setOnClickListener { MusicManager.instance.playMusic(songInfos, 0) }
        //暂停
        findViewById<View>(R.id.pause).setOnClickListener { MusicManager.instance.pauseMusic() }
        //恢复播放
        findViewById<View>(R.id.resum).setOnClickListener { MusicManager.instance.playMusic() }
        //停止播放
        findViewById<View>(R.id.stop).setOnClickListener { MusicManager.instance.stopMusic() }
        //下一首
        findViewById<View>(R.id.pre).setOnClickListener { MusicManager.instance.skipToPrevious() }
        //上一首
        findViewById<View>(R.id.next).setOnClickListener { MusicManager.instance.skipToNext() }
        //快进
        findViewById<View>(R.id.fastForward).setOnClickListener { MusicManager.instance.fastForward() }
        //快退
        findViewById<View>(R.id.rewind).setOnClickListener { MusicManager.instance.rewind() }
        //当前歌曲信息
        findViewById<View>(R.id.currSong).setOnClickListener {
            val songInfo = MusicManager.instance.nowPlayingSongInfo
            if (songInfo == null) {
                Toast.makeText(this@MainActivity, "songInfo is null", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "curr SongInfo = " + songInfo.songId!!, Toast.LENGTH_SHORT).show()
            }
        }
        //当前歌曲id
        findViewById<View>(R.id.currSongId).setOnClickListener {
            val songId = MusicManager.instance.nowPlayingSongId
            if (TextUtils.isEmpty(songId)) {
                Toast.makeText(this@MainActivity, "songId is null", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "songId = $songId", Toast.LENGTH_SHORT).show()
            }
        }
        //当前歌曲下标
        findViewById<View>(R.id.currSongIndex).setOnClickListener {
            val index = MusicManager.instance.nowPlayingIndex
            Toast.makeText(this@MainActivity, "index = $index", Toast.LENGTH_SHORT).show()
        }
        //通知栏喜欢按钮
        findViewById<View>(R.id.sendFavorite).setOnClickListener {
            if (isFavorite) {
                MusicManager.instance.updateFavoriteUI(false)
                isFavorite = false
            } else {
                MusicManager.instance.updateFavoriteUI(true)
                isFavorite = true
            }
        }
        //通知栏歌词按钮
        findViewById<View>(R.id.sendLyrics).setOnClickListener {
            if (isChecked) {
                MusicManager.instance.updateLyricsUI(false)
                isChecked = false
            } else {
                MusicManager.instance.updateLyricsUI(true)
                isChecked = true
            }
        }
        //缓存大小
        findViewById<View>(R.id.cacheSize).setOnClickListener {
            val size = ExoDownload.instance.cachedSize.toString() + ""
            Toast.makeText(this@MainActivity, "大小：$size", Toast.LENGTH_SHORT).show()
        }
        //设置是否随机播放
        findViewById<View>(R.id.shuffleMode).setOnClickListener {
            val repeatMode = MusicManager.instance.shuffleMode
            if (repeatMode == PlaybackStateCompat.SHUFFLE_MODE_NONE) {
                Toast.makeText(this@MainActivity, "设置为随机播放", Toast.LENGTH_SHORT).show()
                MusicManager.instance.shuffleMode = PlaybackStateCompat.SHUFFLE_MODE_ALL
            } else {
                Toast.makeText(this@MainActivity, "设置为顺序播放", Toast.LENGTH_SHORT).show()
                MusicManager.instance.shuffleMode = PlaybackStateCompat.SHUFFLE_MODE_NONE
            }
        }
        //获取是否随机播放
        findViewById<View>(R.id.isShuffleMode).setOnClickListener {
            val repeatMode = MusicManager.instance.shuffleMode
            Toast.makeText(this@MainActivity, if (repeatMode == PlaybackStateCompat.SHUFFLE_MODE_NONE) "否" else "是", Toast.LENGTH_SHORT).show()
        }
        //设置播放模式
        findViewById<View>(R.id.playMode).setOnClickListener {
            val repeatMode = MusicManager.instance.repeatMode
            if (repeatMode == PlaybackStateCompat.REPEAT_MODE_NONE) {
                MusicManager.instance.repeatMode = PlaybackStateCompat.REPEAT_MODE_ONE
                Toast.makeText(this@MainActivity, "设置为单曲循环", Toast.LENGTH_SHORT).show()
            } else if (repeatMode == PlaybackStateCompat.REPEAT_MODE_ONE) {
                MusicManager.instance.repeatMode = PlaybackStateCompat.REPEAT_MODE_ALL
                Toast.makeText(this@MainActivity, "设置为列表循环", Toast.LENGTH_SHORT).show()
            } else if (repeatMode == PlaybackStateCompat.REPEAT_MODE_ALL) {
                MusicManager.instance.repeatMode = PlaybackStateCompat.REPEAT_MODE_NONE
                Toast.makeText(this@MainActivity, "设置为顺序播放", Toast.LENGTH_SHORT).show()
            }
        }
        //获取播放模式
        findViewById<View>(R.id.currPlayMode).setOnClickListener {
            val repeatMode = MusicManager.instance.repeatMode
            if (repeatMode == PlaybackStateCompat.REPEAT_MODE_NONE) {
                Toast.makeText(this@MainActivity, "当前为顺序播放", Toast.LENGTH_SHORT).show()
            } else if (repeatMode == PlaybackStateCompat.REPEAT_MODE_ONE) {
                Toast.makeText(this@MainActivity, "当前为单曲循环", Toast.LENGTH_SHORT).show()
            } else if (repeatMode == PlaybackStateCompat.REPEAT_MODE_ALL) {
                Toast.makeText(this@MainActivity, "当前为列表循环", Toast.LENGTH_SHORT).show()
            }
        }
        //是否有下一首
        findViewById<View>(R.id.hasNext).setOnClickListener {
            val hasNext = MusicManager.instance.isSkipToNextEnabled
            Toast.makeText(this@MainActivity, if (hasNext) "有" else "没", Toast.LENGTH_SHORT).show()
        }
        //是否有上一首
        findViewById<View>(R.id.hasPre).setOnClickListener {
            val hasPre = MusicManager.instance.isSkipToPreviousEnabled
            Toast.makeText(this@MainActivity, if (hasPre) "有" else "没", Toast.LENGTH_SHORT).show()
        }
        //获取播放速度
        findViewById<View>(R.id.playSpeed).setOnClickListener {
            val speed = MusicManager.instance.playbackSpeed
            Toast.makeText(this@MainActivity, "speed = $speed", Toast.LENGTH_SHORT).show()
        }
        //音量加
        findViewById<View>(R.id.addvolume).setOnClickListener {
            var volume = MusicManager.instance.volume
            volume += 0.1f
            if (volume > 1) {
                volume = 1f
            }
            MusicManager.instance.volume = volume
        }
        //音量减
        findViewById<View>(R.id.jianvolume).setOnClickListener {
            var volume = MusicManager.instance.volume
            volume -= 0.1f
            if (volume < 0) {
                volume = 0f
            }
            MusicManager.instance.volume = volume
        }
        //获取当前音量
        findViewById<View>(R.id.getVolume).setOnClickListener {
            val volume = MusicManager.instance.volume
            Toast.makeText(this@MainActivity, "volume = $volume", Toast.LENGTH_SHORT).show()
        }
        //获取本地音频信息
        findViewById<View>(R.id.localSong).setOnClickListener {
            val list = MusicManager.instance.querySongInfoInLocal()
            Toast.makeText(this@MainActivity, "list.size = " + list.size, Toast.LENGTH_SHORT).show()
        }
        //添加监听
        MusicManager.instance.addPlayerEventListener(this)
        //进度更新
        mTimerTask.setUpdateProgressTask(Runnable {
            val position = MusicManager.instance.playingPosition
            val duration = MusicManager.instance.duration
            val buffered = MusicManager.instance.bufferedPosition
            if (mSeekBar.max.toLong() != duration) {
                mSeekBar.max = duration.toInt()
            }
            mSeekBar.progress = position.toInt()
            mSeekBar.secondaryProgress = buffered.toInt()
            currTime.text = formatMusicTime(position) + "/" + formatMusicTime(duration)
        })
        //进度条滑动
        mSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                MusicManager.instance.seekTo(seekBar.progress.toLong())
            }
        })

    }

    override fun onStart() {
        super.onStart()
        //连接音频服务
        mMediaSessionConnection!!.connect()
    }

    override fun onStop() {
        super.onStop()
        //断开音频服务
        mMediaSessionConnection!!.disconnect()
    }

    override fun onDestroy() {
        super.onDestroy()
        //回收资源
        MusicManager.instance.removePlayerEventListener(this)
        mTimerTask.removeUpdateProgressTask()
    }

    @SuppressLint("SetTextI18n")
    override fun onMusicSwitch(songInfo: SongInfo) {
        currInfo.text = "当前播放：" + songInfo.songName!!
        LogUtil.i("= onMusicSwitch = " + songInfo.songName!!)
    }

    override fun onPlayerStart() {
        //开始更新进度条
        mTimerTask.startToUpdateProgress()
        LogUtil.i("= onPlayerStart = ")
    }

    override fun onPlayerPause() {
        //停止更新进度条
        mTimerTask.stopToUpdateProgress()
        LogUtil.i("= onPlayerPause = ")
    }

    @SuppressLint("SetTextI18n")
    override fun onPlayerStop() {
        //停止更新进度条
        mTimerTask.stopToUpdateProgress()
        mSeekBar.progress = 0
        currTime.text = "00:00"
        LogUtil.i("= onPlayerStop = ")
    }

    override fun onPlayCompletion(songInfo: SongInfo) {
        //songInfo maybe null
        //停止更新进度条
        mTimerTask.stopToUpdateProgress()
        LogUtil.i("= onPlayCompletion = " + songInfo.songName!!)
    }

    override fun onBuffering() {
        LogUtil.i("= onBuffering = ")
    }

    override fun onError(errorCode: Int, errorMsg: String) {
        //停止更新进度条
        mTimerTask.stopToUpdateProgress()
        LogUtil.i("= onError = errorCode:$errorCode errorMsg:$errorMsg")
    }

    object LogUtil {
        fun i(msg: String) {
            Log.i("LogUtil", msg)
        }
    }

    companion object {

        fun formatMusicTime(duration: Long): String {
            var time = ""
            val minute = duration / 60000
            val seconds = duration % 60000
            val second = Math.round((seconds.toInt() / 1000).toFloat()).toLong()
            if (minute < 10) {
                time += "0"
            }
            time += "$minute:"
            if (second < 10) {
                time += "0"
            }
            time += second
            return time
        }
    }

}
