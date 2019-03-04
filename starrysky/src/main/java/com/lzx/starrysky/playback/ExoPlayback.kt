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
import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat.QueueItem
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.C.CONTENT_TYPE_MUSIC
import com.google.android.exoplayer2.C.USAGE_MEDIA
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.google.android.exoplayer2.ext.rtmp.RtmpDataSourceFactory
import com.google.android.exoplayer2.offline.FilteringManifestParser
import com.google.android.exoplayer2.offline.StreamKey
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.manifest.DashManifest
import com.google.android.exoplayer2.source.dash.manifest.DashManifestParser
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.hls.playlist.DefaultHlsPlaylistParserFactory
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.manifest.SsManifest
import com.google.android.exoplayer2.source.smoothstreaming.manifest.SsManifestParser
import com.google.android.exoplayer2.trackselection.*
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.Util
import com.lzx.starrysky.model.MusicProvider
import com.lzx.starrysky.playback.download.ExoDownload

/**
 * ExoPlayer 播放器的具体封装
 */
class ExoPlayback(context: Context, private val mMusicProvider: MusicProvider) : Playback {

    private val mContext: Context = context.applicationContext
    private var mPlayOnFocusGain: Boolean = false
    private var mCallback: Playback.Callback? = null

    override var currentMediaId: String = ""


    private var mExoPlayer: SimpleExoPlayer? = null
    private val mEventListener = ExoPlayerEventListener()

    private var mExoPlayerNullIsStopped = false

    private var trackSelector: DefaultTrackSelector? = null
    private val trackSelectorParameters: DefaultTrackSelector.Parameters = DefaultTrackSelector.ParametersBuilder().build()

    override var state: Int
        get() {
            if (mExoPlayer == null) {
                return if (mExoPlayerNullIsStopped) PlaybackStateCompat.STATE_STOPPED else PlaybackStateCompat.STATE_NONE
            }
            return when (mExoPlayer!!.playbackState) {
                Player.STATE_IDLE -> PlaybackStateCompat.STATE_PAUSED
                Player.STATE_BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
                Player.STATE_READY -> if (mExoPlayer!!.playWhenReady) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
                Player.STATE_ENDED -> PlaybackStateCompat.STATE_NONE
                else -> PlaybackStateCompat.STATE_NONE
            }
        }
        set(state) {

        }

    override val isConnected: Boolean
        get() = true

    override val isPlaying: Boolean
        get() = mPlayOnFocusGain || mExoPlayer != null && mExoPlayer!!.playWhenReady

    override val currentStreamPosition: Long
        get() = if (mExoPlayer != null) mExoPlayer!!.currentPosition else 0

    override val bufferedPosition: Long
        get() = if (mExoPlayer != null) mExoPlayer!!.bufferedPosition else 0

    /**
     * 获取音量
     */
    /**
     * 设置音量
     */
    override var volume: Float
        get() = if (mExoPlayer != null) {
            mExoPlayer!!.volume
        } else {
            -1f
        }
        set(audioVolume) {
            if (mExoPlayer != null) {
                mExoPlayer!!.volume = audioVolume
            }
        }

    /**
     * 获取时长
     */
    override val duration: Long
        get() = if (mExoPlayer != null) {
            mExoPlayer!!.duration
        } else {
            -1
        }

    override fun start() {

    }

    override fun stop(notifyListeners: Boolean) {
        releaseResources(true)
    }

    override fun updateLastKnownStreamPosition() {
        // Nothing to do. Position maintained by ExoPlayer.
    }

    override fun play(item: QueueItem) {
        mPlayOnFocusGain = true
        val mediaId = item.description.mediaId
        val mediaHasChanged = !TextUtils.equals(mediaId, currentMediaId)
        if (mediaHasChanged) {
            currentMediaId = mediaId!!
        }
        if (mediaHasChanged || mExoPlayer == null) {
            releaseResources(false) // release everything except the player
            val track = mMusicProvider.getMusic(item.description.mediaId!!)

            var source = track!!.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)
            if (TextUtils.isEmpty(source)) {
                return
            }
            source = source.replace(" ".toRegex(), "%20") // Escape spaces for URLs
            //缓存歌曲
            if (ExoDownload.instance.isOpenCache) {
                mediaId?.let { ExoDownload.instance.getDownloadTracker()!!.toggleDownload(it, Uri.parse(source), "") }
            }

            if (mExoPlayer == null) {
                //轨道选择
                val trackSelectionFactory: TrackSelection.Factory
                if (abrAlgorithm == ABR_ALGORITHM_DEFAULT) {
                    trackSelectionFactory = AdaptiveTrackSelection.Factory()
                } else {
                    trackSelectionFactory = RandomTrackSelection.Factory()
                }
                //使用扩展渲染器的模式
                @DefaultRenderersFactory.ExtensionRendererMode val extensionRendererMode = if (rendererMode == EXTENSION_RENDERER_MODE_ON)
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                else
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF //不使用
                val renderersFactory = DefaultRenderersFactory(mContext, extensionRendererMode)

                //轨道选择
                trackSelector = DefaultTrackSelector(trackSelectionFactory)
                trackSelector!!.parameters = trackSelectorParameters

                val drmSessionManager: DefaultDrmSessionManager<FrameworkMediaCrypto>? = null

                mExoPlayer = ExoPlayerFactory.newSimpleInstance(mContext, renderersFactory, trackSelector!!, drmSessionManager)
                mExoPlayer!!.addListener(mEventListener)
                mExoPlayer!!.addAnalyticsListener(EventLogger(trackSelector))
            }

            val audioAttributes = AudioAttributes.Builder()
                    .setContentType(CONTENT_TYPE_MUSIC)
                    .setUsage(USAGE_MEDIA)
                    .build()
            mExoPlayer!!.setAudioAttributes(audioAttributes, true) //第二个参数能使ExoPlayer自动管理焦点

            val dataSourceFactory = ExoDownload.instance.buildDataSourceFactory(mContext)

            val mediaSource = buildMediaSource(dataSourceFactory, Uri.parse(source), null)

            mExoPlayer!!.prepare(mediaSource)
        }
        mExoPlayer!!.playWhenReady = true
    }

    private fun buildMediaSource(dataSourceFactory: DataSource.Factory, uri: Uri, overrideExtension: String?): MediaSource {
        @C.ContentType val type = Util.inferContentType(uri, overrideExtension)
        when (type) {
            C.TYPE_DASH -> return DashMediaSource.Factory(dataSourceFactory)
                    .setManifestParser(
                            FilteringManifestParser<DashManifest>(DashManifestParser(), getOfflineStreamKeys(uri)))
                    .createMediaSource(uri)
            C.TYPE_SS -> return SsMediaSource.Factory(dataSourceFactory)
                    .setManifestParser(
                            FilteringManifestParser<SsManifest>(SsManifestParser(), getOfflineStreamKeys(uri)))
                    .createMediaSource(uri)
            C.TYPE_HLS -> return HlsMediaSource.Factory(dataSourceFactory)
                    .setPlaylistParserFactory(
                            DefaultHlsPlaylistParserFactory(getOfflineStreamKeys(uri)))
                    .createMediaSource(uri)
            C.TYPE_OTHER -> {
                val isRtmpSource = uri.toString().toLowerCase().startsWith("rtmp://")
                return ExtractorMediaSource.Factory(if (isRtmpSource) RtmpDataSourceFactory() else dataSourceFactory)
                        .createMediaSource(uri)
            }
            else -> {
                throw IllegalStateException("Unsupported type: $type")
            }
        }
    }

    private fun getOfflineStreamKeys(uri: Uri): List<StreamKey> {
        return ExoDownload.instance.getDownloadTracker()!!.getOfflineStreamKeys(uri)
    }

    override fun pause() {
        if (mExoPlayer != null) {
            mExoPlayer!!.playWhenReady = false
        }
        releaseResources(false)
    }

    override fun seekTo(position: Long) {
        if (mExoPlayer != null) {
            mExoPlayer!!.seekTo(position)
        }
    }

    override fun setCallback(callback: Playback.Callback) {
        this.mCallback = callback
    }

    /**
     * 快进
     */
    override fun onFastForward() {
        if (mExoPlayer != null) {
            val currSpeed = mExoPlayer!!.playbackParameters.speed
            val currPitch = mExoPlayer!!.playbackParameters.pitch
            val newSpeed = currSpeed + 0.5f
            mExoPlayer!!.playbackParameters = PlaybackParameters(newSpeed, currPitch)
        }
    }

    /**
     * 倒带
     */
    override fun onRewind() {
        if (mExoPlayer != null) {
            val currSpeed = mExoPlayer!!.playbackParameters.speed
            val currPitch = mExoPlayer!!.playbackParameters.pitch
            var newSpeed = currSpeed - 0.5f
            if (newSpeed <= 0) {
                newSpeed = 0f
            }
            mExoPlayer!!.playbackParameters = PlaybackParameters(newSpeed, currPitch)
        }
    }

    private fun releaseResources(releasePlayer: Boolean) {
        if (releasePlayer && mExoPlayer != null) {
            mExoPlayer!!.release()
            mExoPlayer!!.removeListener(mEventListener)
            mExoPlayer = null
            mExoPlayerNullIsStopped = true
            mPlayOnFocusGain = false
        }
    }

    private inner class ExoPlayerEventListener : Player.EventListener {
        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
            // Nothing to do.
        }

        override fun onTracksChanged(
                trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
            // Nothing to do.
        }

        override fun onLoadingChanged(isLoading: Boolean) {
            // Nothing to do.
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE, Player.STATE_BUFFERING, Player.STATE_READY -> if (mCallback != null) {
                    mCallback!!.onPlaybackStatusChanged(state)
                }
                Player.STATE_ENDED -> if (mCallback != null) {
                    mCallback!!.onCompletion()
                }
            }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            val what: String = when (error.type) {
                ExoPlaybackException.TYPE_SOURCE -> error.sourceException.message!!
                ExoPlaybackException.TYPE_RENDERER -> error.rendererException.message!!
                ExoPlaybackException.TYPE_UNEXPECTED -> error.unexpectedException.message!!
                else -> "Unknown: $error"
            }

            if (mCallback != null) {
                mCallback!!.onError("ExoPlayer error $what")
            }
        }

        override fun onPositionDiscontinuity(reason: Int) {
            // Nothing to do.
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
            // Nothing to do.
        }

        override fun onSeekProcessed() {
            // Nothing to do.
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            // Nothing to do.
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            // Nothing to do.
        }
    }

    companion object {

        private val TAG = "ExoPlayback"

        val ACTION_CHANGE_VOLUME = "ACTION_CHANGE_VOLUME"

        val ABR_ALGORITHM_DEFAULT = "default"
        val ABR_ALGORITHM_RANDOM = "random"
        var abrAlgorithm = ABR_ALGORITHM_DEFAULT

        val EXTENSION_RENDERER_MODE_ON = "EXTENSION_RENDERER_MODE_ON"
        val EXTENSION_RENDERER_MODE_OFF = "EXTENSION_RENDERER_MODE_OFF"
        var rendererMode = EXTENSION_RENDERER_MODE_OFF
    }
}
