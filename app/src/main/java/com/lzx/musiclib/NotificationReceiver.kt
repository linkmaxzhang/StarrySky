package com.lzx.musiclib

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils

import com.lzx.starrysky.manager.MusicManager

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (TextUtils.isEmpty(action)) {
            return
        }
        if (TestApplication.ACTION_PLAY_OR_PAUSE == action) {
            val state = MusicManager.instance.state
            if (state == PlaybackStateCompat.STATE_PLAYING) {
                MusicManager.instance.pauseMusic()
            } else {
                MusicManager.instance.playMusic()
            }
        }
        if (TestApplication.ACTION_NEXT == action) {
            MusicManager.instance.skipToNext()
        }
        if (TestApplication.ACTION_PRE == action) {
            MusicManager.instance.skipToPrevious()
        }
        if (TestApplication.ACTION_FAVORITE == action) {
            //这里实现自己的喜欢或收藏逻辑，如果选中可以传 true 把按钮变成选中状态，false 就非选中状态
            MusicManager.instance.updateFavoriteUI(true)
        }
        if (TestApplication.ACTION_LYRICS == action) {
            //这里实现自己的是否显示歌词逻辑，如果选中可以传 true 把按钮变成选中状态，false 就非选中状态
            MusicManager.instance.updateLyricsUI(true)
        }
    }
}
