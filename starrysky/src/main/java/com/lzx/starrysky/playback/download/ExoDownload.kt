package com.lzx.starrysky.playback.download

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloaderConstructorHelper
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.*
import com.lzx.starrysky.playback.Utils
import java.io.File

class ExoDownload private constructor() {
    private val userAgent: String
    private var downloadManager: DownloadManager? = null
    private var downloadCache: Cache? = null
    private var downloadDirectory: File? = null
    private var downloadTracker: DownloadTracker? = null
    private var destFileDir: String? = null
    /**
     * 是否打开缓存功能
     */
    /**
     * 配置缓存开关
     */
    var isOpenCache = false
    /**
     * 下载时是否显示通知栏
     */
    /**
     * 配置下载时是否显示通知栏
     */
    var isShowNotificationWhenDownload = false

    /**
     * 获取媒体缓存大小
     */
    val cachedSize: Long
        get() = getDownloadDirectory(sContext)!!.length()

    private object SingletonHolder {
        @SuppressLint("StaticFieldLeak")
        val sInstance = ExoDownload()
    }

    init {
        userAgent = Utils.getUserAgent(sContext!!, "ExoPlayback")
    }

    /**
     * 配置缓存文件夹
     */
    fun setCacheDestFileDir(destFileDir: String) {
        this.destFileDir = destFileDir
    }

    /**
     * 获取 DownloadManager
     */
    fun getDownloadManager(): DownloadManager? {
        initDownloadManager(sContext)
        return downloadManager
    }

    /**
     * 获取 DownloadTracker
     */
    fun getDownloadTracker(): DownloadTracker? {
        initDownloadManager(sContext)
        return downloadTracker
    }

    /**
     * 初始化 DownloadManager
     */
    @Synchronized
    private fun initDownloadManager(context: Context?) {
        if (downloadManager ==
                /* eventListener= */ null) {
            val downloaderConstructorHelper = DownloaderConstructorHelper(getDownloadCache(), DefaultHttpDataSourceFactory(userAgent))
            downloadManager = DownloadManager(
                    downloaderConstructorHelper,
                    MAX_SIMULTANEOUS_DOWNLOADS,
                    DownloadManager.DEFAULT_MIN_RETRY_COUNT,
                    File(getDownloadDirectory(context), DOWNLOAD_ACTION_FILE))
            downloadTracker = sContext?.let {
                DownloadTracker(
                        /* context= */ it,
                        buildDataSourceFactory(sContext),
                        File(getDownloadDirectory(context), DOWNLOAD_TRACKER_ACTION_FILE))
            }
            downloadManager!!.addListener(downloadTracker)
        }
    }

    /**
     * 获取缓存实例
     */
    @Synchronized
    fun getDownloadCache(): Cache {
        if (downloadCache == null) {
            val downloadContentDirectory = File(getDownloadDirectory(sContext), DOWNLOAD_CONTENT_DIRECTORY)
            downloadCache = SimpleCache(downloadContentDirectory, NoOpCacheEvictor())
        }
        return downloadCache as Cache
    }

    /**
     * 创建缓存文件夹
     */
    private fun getDownloadDirectory(context: Context?): File? {
        if (!TextUtils.isEmpty(destFileDir)) {
            downloadDirectory = File(destFileDir)
            if (!downloadDirectory!!.exists()) {
                downloadDirectory!!.mkdirs()
            }
        }
        if (downloadDirectory == null) {
            downloadDirectory = context!!.getExternalFilesDir(null)
            if (downloadDirectory == null) {
                downloadDirectory = context.filesDir
            }
        }
        return downloadDirectory
    }

    /**
     * 删除所有缓存文件
     */
    fun deleteAllCacheFile(): Boolean {
        if (downloadDirectory == null) {
            downloadDirectory = getDownloadDirectory(sContext)
        }
        for (file in downloadDirectory!!.listFiles()) {
            if (file.isFile) {
                file.delete() // 删除所有文件
            } else if (file.isDirectory) {
                deleteAllCacheFile() // 递规的方式删除文件夹
            }
        }
        return downloadDirectory!!.delete()// 删除目录本身
    }

    /**
     * 删除某一首歌的缓存
     */
    fun deleteCacheFileByUrl(url: String) {
        getDownloadTracker()!!.deleteCacheFileByUrl(url)
    }

    /**
     * DataSourceFactory构造
     */
    fun buildDataSourceFactory(context: Context?): DataSource.Factory {
        val upstreamFactory = DefaultDataSourceFactory(context!!, buildHttpDataSourceFactory())
        return buildReadOnlyCacheDataSource(upstreamFactory, getDownloadCache())
    }

    fun buildHttpDataSourceFactory(): HttpDataSource.Factory {
        return DefaultHttpDataSourceFactory(userAgent)
    }

    companion object {

        private val DOWNLOAD_ACTION_FILE = "actions"
        private val DOWNLOAD_TRACKER_ACTION_FILE = "tracked_actions"
        private val DOWNLOAD_CONTENT_DIRECTORY = "downloads" //下载路径子文件夹
        private val MAX_SIMULTANEOUS_DOWNLOADS = 2

        @SuppressLint("StaticFieldLeak")
        private var sContext: Context? = null

        val instance: ExoDownload
            get() = SingletonHolder.sInstance

        fun initExoDownload(context: Context) {
            sContext = context
        }

        private fun buildReadOnlyCacheDataSource(upstreamFactory: DefaultDataSourceFactory, cache: Cache): CacheDataSourceFactory {
            return CacheDataSourceFactory(
                    cache,
                    upstreamFactory,
                    FileDataSourceFactory(), null,
                    CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR, null)/* cacheWriteDataSinkFactory= */
        }
    }
}
