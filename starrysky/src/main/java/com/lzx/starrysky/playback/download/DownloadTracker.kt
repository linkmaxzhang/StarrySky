/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.lzx.starrysky.playback.download

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.text.TextUtils
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.offline.*
import com.google.android.exoplayer2.offline.DownloadManager.TaskState
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.util.Util
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet


class DownloadTracker(context: Context, private val dataSourceFactory: DataSource.Factory, actionFile: File,
                      vararg deserializers: DownloadAction.Deserializer) : DownloadManager.Listener, DownloadHelper.Callback {

    private val context: Context = context.applicationContext
    private val listeners: CopyOnWriteArraySet<Listener> = CopyOnWriteArraySet()
    private val trackedDownloadStates: HashMap<Uri, DownloadAction> = HashMap()
    private val actionFile: ActionFile = ActionFile(actionFile)
    private val actionFileWriteHandler: Handler
    private var downloadHelper: DownloadHelper? = null
    private val trackKeys: MutableList<TrackKey>
    private var name: String? = null

    interface Listener {
        fun onDownloadsChanged()
    }

    init {
        trackKeys = ArrayList()

        val actionFileWriteThread = HandlerThread("DownloadTracker")
        actionFileWriteThread.start()
        actionFileWriteHandler = Handler(actionFileWriteThread.looper)
        loadTrackedActions(if (deserializers.isNotEmpty()) deserializers else DownloadAction.getDefaultDeserializers())
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    /**
     * 是否已经下载
     */
    private fun isDownloaded(uri: Uri): Boolean {
        return trackedDownloadStates.containsKey(uri)
    }

    fun getOfflineStreamKeys(uri: Uri): List<StreamKey> {
        return if (!trackedDownloadStates.containsKey(uri)) {
            emptyList()
        } else {
            val downloadAction = trackedDownloadStates[uri]
            if (downloadAction != null) downloadAction.keys else emptyList()
        }
    }

    /**
     * 下载
     */
    fun toggleDownload(name: String, uri: Uri, extension: String) {
        this.name = name
        if (isDownloaded(uri)) {
            //Log.i("xian", "--- 已经下载完了 ---");
            //DownloadAction removeAction = getDownloadHelper(uri, extension).getRemoveAction(Util.getUtf8Bytes(name));
            //startServiceWithAction(removeAction);
        } else {
            // Log.i("xian", "--- 新下载 ---");
            downloadHelper = getDownloadHelper(uri, extension)
            if (downloadHelper != null) {
                downloadHelper!!.prepare(this)
            }
        }
    }

    fun deleteCacheFileByUrl(url: String) {
        if (TextUtils.isEmpty(url)) {
            return
        }
        val uri = Uri.parse(url)
        if (isDownloaded(uri)) {
            if (downloadHelper == null) {
                downloadHelper = getDownloadHelper(uri, "")
            }
            if (downloadHelper != null) {
                val removeAction = downloadHelper!!.getRemoveAction(Util.getUtf8Bytes(name!!))
                startServiceWithAction(removeAction)
            }
        }
    }

    /**
     * 开始下载
     */
    private fun startDownload() {
        if (downloadHelper == null) {
            return
        }
        val downloadAction = downloadHelper!!.getDownloadAction(Util.getUtf8Bytes(name!!), trackKeys)
        if (trackedDownloadStates.containsKey(downloadAction.uri)) {
            return
        }
        trackedDownloadStates[downloadAction.uri] = downloadAction
        handleTrackedDownloadStatesChanged()
        startServiceWithAction(downloadAction)
    }

    override fun onPrepared(helper: DownloadHelper) {
        if (downloadHelper == null) {
            return
        }
        for (i in 0 until downloadHelper!!.periodCount) {
            val trackGroups = downloadHelper!!.getTrackGroups(i)
            for (j in 0 until trackGroups.length) {
                val trackGroup = trackGroups.get(j)
                for (k in 0 until trackGroup.length) {
                    trackKeys.add(TrackKey(i, j, k))
                }
            }
        }
        startDownload()
    }

    override fun onPrepareError(helper: DownloadHelper, e: IOException) {
        Log.e(TAG, "Failed to start download", e)
    }

    // ExoDownload.Listener
    override fun onInitialized(downloadManager: DownloadManager) {
        // Do nothing.
    }

    override fun onTaskStateChanged(downloadManager: DownloadManager, taskState: TaskState) {
        val action = taskState.action
        val uri = action.uri
        if (action.isRemoveAction && taskState.state == TaskState.STATE_COMPLETED || !action.isRemoveAction && taskState.state == TaskState.STATE_FAILED) {
            // A download has been removed, or has failed. Stop tracking it.
            if (trackedDownloadStates.remove(uri) != null) {
                handleTrackedDownloadStatesChanged()
            }
        }
    }

    override fun onIdle(downloadManager: DownloadManager) {
        // Do nothing.
    }

    // Internal methods
    private fun loadTrackedActions(deserializers: Array<out DownloadAction.Deserializer>) {
        try {
            val allActions = actionFile.load(*deserializers)
            for (action in allActions) {
                trackedDownloadStates[action.uri] = action
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(TAG, "Failed to load tracked actions", e)
        }

    }

    private fun handleTrackedDownloadStatesChanged() {
        for (listener in listeners) {
            listener.onDownloadsChanged()
        }
        val actions = trackedDownloadStates.values.toTypedArray()
        actionFileWriteHandler.post {
            try {
                actionFile.store(*actions)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to store tracked actions", e)
            }
        }
    }


    /**
     * 执行DownloadAction
     */
    private fun startServiceWithAction(action: DownloadAction) {
        DownloadService.startWithAction(context, ExoDownloadService::class.java, action, false)
    }

    private fun getDownloadHelper(uri: Uri, extension: String): DownloadHelper? {
        val type = Util.inferContentType(uri, extension)
        return when (type) {
            //            case C.TYPE_DASH:
            //                return new DashDownloadHelper(uri, dataSourceFactory);
            //            case C.TYPE_SS:
            //                return new SsDownloadHelper(uri, dataSourceFactory);
            //            case C.TYPE_HLS:
            //                return new HlsDownloadHelper(uri, dataSourceFactory);
            C.TYPE_OTHER -> ProgressiveDownloadHelper(uri)
            else -> null
        }
    }

    companion object {

        private val TAG = "DownloadTracker"
    }
}
