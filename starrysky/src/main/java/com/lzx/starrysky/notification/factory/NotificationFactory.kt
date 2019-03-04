package com.lzx.starrysky.notification.factory

import android.os.RemoteException

import com.lzx.starrysky.MusicService
import com.lzx.starrysky.notification.CustomNotification
import com.lzx.starrysky.notification.NotificationConstructor
import com.lzx.starrysky.notification.SystemNotification

class NotificationFactory(private val mMusicService: MusicService, private val mConstructor: NotificationConstructor?) : INotificationFactory {
    private var mNotification: INotification? = null

    override fun createNotification() {
        if (mConstructor == null) {
            return
        }
        try {
            if (mConstructor.isCreateSystemNotification) {
                mNotification = SystemNotification(mMusicService, mConstructor)
            } else {
                mNotification = CustomNotification(mMusicService, mConstructor)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }

    }

    override fun startNotification() {
        if (mNotification != null) {
            mNotification!!.startNotification()
        }
    }

    override fun stopNotification() {
        if (mNotification != null) {
            mNotification!!.stopNotification()
        }
    }

    /**
     * 更新喜欢或收藏按钮UI
     */
    fun updateFavoriteUI(isFavorite: Boolean) {
        if (mNotification != null) {
            mNotification!!.updateFavoriteUI(isFavorite)
        }
    }

    /**
     * 更新歌词按钮UI
     */
    fun updateLyricsUI(isChecked: Boolean) {
        if (mNotification != null) {
            mNotification!!.updateLyricsUI(isChecked)
        }
    }


}
