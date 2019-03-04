package com.lzx.starrysky.model

import android.graphics.Bitmap
import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


/**
 * 面向用户的音频信息
 */

class SongInfo() : Parcelable {
    var songId: String? = "" //音乐id
    var songName: String? = "" //音乐标题
    var songCover: String? = "" //音乐封面
    var songHDCover: String? = "" //专辑封面(高清)
    var songSquareCover: String? = "" //专辑封面(正方形)
    var songRectCover: String? = "" //专辑封面(矩形)
    var songRoundCover: String? = "" //专辑封面(圆形)
    var songNameKey: String? = ""
    var songCoverBitmap: Bitmap? = null
    var songUrl: String? = "" //音乐播放地址
    var genre: String? = "" //类型（流派）
    var type: String? = "" //类型
    var size: String? = "0" //音乐大小
    var duration: Long = -1 //音乐长度
    var artist: String? = "" //音乐艺术家
    var artistKey: String? = ""
    var artistId: String? = "" //音乐艺术家id
    var downloadUrl: String? = "" //音乐下载地址
    var site: String? = "" //地点
    var favorites = 0 //喜欢数
    var playCount = 0 //播放数
    var trackNumber = -1 //媒体的曲目号码（序号：1234567……）
    var language: String? = ""//语言
    var country: String? = "" //地区
    var proxyCompany: String? = ""//代理公司
    var publishTime: String? = ""//发布时间
    var year: String? = "" //录制音频文件的年份
    var modifiedTime: String? = "" //最后修改时间
    var description: String? = "" //音乐描述
    var versions: String? = "" //版本
    var mimeType: String? = ""

    var albumId: String? = ""    //专辑id
    var albumName: String? = ""  //专辑名称
    var albumNameKey: String? = ""
    var albumCover: String? = "" //专辑封面
    var albumHDCover: String? = "" //专辑封面(高清)
    var albumSquareCover: String? = "" //专辑封面(正方形)
    var albumRectCover: String? = "" //专辑封面(矩形)
    var albumRoundCover: String? = "" //专辑封面(圆形)
    var albumArtist: String? = ""     //专辑艺术家
    var albumSongCount = 0      //专辑音乐数
    var albumPlayCount = 0      //专辑播放数

    constructor(parcel: Parcel) : this() {
        songId = parcel.readString()
        songName = parcel.readString()
        songCover = parcel.readString()
        songHDCover = parcel.readString()
        songSquareCover = parcel.readString()
        songRectCover = parcel.readString()
        songRoundCover = parcel.readString()
        songNameKey = parcel.readString()
        songCoverBitmap = parcel.readParcelable(Bitmap::class.java.classLoader)
        songUrl = parcel.readString()
        genre = parcel.readString()
        type = parcel.readString()
        size = parcel.readString()
        duration = parcel.readLong()
        artist = parcel.readString()
        artistKey = parcel.readString()
        artistId = parcel.readString()
        downloadUrl = parcel.readString()
        site = parcel.readString()
        favorites = parcel.readInt()
        playCount = parcel.readInt()
        trackNumber = parcel.readInt()
        language = parcel.readString()
        country = parcel.readString()
        proxyCompany = parcel.readString()
        publishTime = parcel.readString()
        year = parcel.readString()
        modifiedTime = parcel.readString()
        description = parcel.readString()
        versions = parcel.readString()
        mimeType = parcel.readString()
        albumId = parcel.readString()
        albumName = parcel.readString()
        albumNameKey = parcel.readString()
        albumCover = parcel.readString()
        albumHDCover = parcel.readString()
        albumSquareCover = parcel.readString()
        albumRectCover = parcel.readString()
        albumRoundCover = parcel.readString()
        albumArtist = parcel.readString()
        albumSongCount = parcel.readInt()
        albumPlayCount = parcel.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(songId)
        parcel.writeString(songName)
        parcel.writeString(songCover)
        parcel.writeString(songHDCover)
        parcel.writeString(songSquareCover)
        parcel.writeString(songRectCover)
        parcel.writeString(songRoundCover)
        parcel.writeString(songNameKey)
        parcel.writeParcelable(songCoverBitmap, flags)
        parcel.writeString(songUrl)
        parcel.writeString(genre)
        parcel.writeString(type)
        parcel.writeString(size)
        parcel.writeLong(duration)
        parcel.writeString(artist)
        parcel.writeString(artistKey)
        parcel.writeString(artistId)
        parcel.writeString(downloadUrl)
        parcel.writeString(site)
        parcel.writeInt(favorites)
        parcel.writeInt(playCount)
        parcel.writeInt(trackNumber)
        parcel.writeString(language)
        parcel.writeString(country)
        parcel.writeString(proxyCompany)
        parcel.writeString(publishTime)
        parcel.writeString(year)
        parcel.writeString(modifiedTime)
        parcel.writeString(description)
        parcel.writeString(versions)
        parcel.writeString(mimeType)
        parcel.writeString(albumId)
        parcel.writeString(albumName)
        parcel.writeString(albumNameKey)
        parcel.writeString(albumCover)
        parcel.writeString(albumHDCover)
        parcel.writeString(albumSquareCover)
        parcel.writeString(albumRectCover)
        parcel.writeString(albumRoundCover)
        parcel.writeString(albumArtist)
        parcel.writeInt(albumSongCount)
        parcel.writeInt(albumPlayCount)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SongInfo> {
        override fun createFromParcel(parcel: Parcel): SongInfo {
            return SongInfo(parcel)
        }

        override fun newArray(size: Int): Array<SongInfo?> {
            return arrayOfNulls(size)
        }
    }
}
