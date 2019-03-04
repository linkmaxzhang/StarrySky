package com.lzx.starrysky.utils

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * 时间任务管理
 */
class TimerTaskManager {
    private val mHandler = Handler()
    private var mTimerHandler: Handler? = null
    private var mTimerRunnable: Runnable? = null
    private val mExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var mScheduleFuture: ScheduledFuture<*>? = null
    private var mUpdateProgressTask: Runnable? = null


    /**
     * 开始倒计时
     */
    private var time: Long = 0

    /**
     * 开始更新进度条
     */
    fun startToUpdateProgress() {
        stopToUpdateProgress()
        if (!mExecutorService.isShutdown) {
            mScheduleFuture = mExecutorService.scheduleAtFixedRate({
                if (mUpdateProgressTask != null) {
                    mHandler.post(mUpdateProgressTask)
                }
            },
                    PROGRESS_UPDATE_INITIAL_INTERVAL,
                    PROGRESS_UPDATE_INTERNAL,
                    TimeUnit.MILLISECONDS)
        }
    }

    /**
     * 设置定时Runnable
     */
    fun setUpdateProgressTask(task: Runnable) {
        mUpdateProgressTask = task
    }

    /**
     * 停止更新进度条
     */
    fun stopToUpdateProgress() {
        if (mScheduleFuture != null) {
            mScheduleFuture!!.cancel(false)
        }
    }

    /**
     * 释放资源
     */
    fun removeUpdateProgressTask() {
        stopToUpdateProgress()
        mExecutorService.shutdown()
        mHandler.removeCallbacksAndMessages(null)
    }

    fun startCountDownTask(millisInFuture: Long, listener: OnCountDownFinishListener) {
        if (mTimerHandler == null) {
            mTimerHandler = Handler(Looper.getMainLooper())
        }
        if (millisInFuture != -1L && millisInFuture > 0L) {
            if (mTimerRunnable == null) {
                time = millisInFuture
                mTimerRunnable = Runnable {
                    time = time - 1000L
                    listener.onTick(time)
                    if (time <= 0L) {
                        listener.onFinish()
                        cancelCountDownTask()
                    } else {
                        mTimerHandler!!.postDelayed(mTimerRunnable, 1000L)
                    }
                }
            }
            mTimerHandler!!.postDelayed(mTimerRunnable, 1000L)
        }
    }

    /**
     * 取消倒计时
     */
    fun cancelCountDownTask() {
        time = 0
        if (mTimerHandler != null) {
            mTimerHandler!!.removeCallbacksAndMessages(null)
            mTimerHandler = null
        }
        if (mTimerRunnable != null) {
            mTimerRunnable = null
        }
    }

    /**
     * 倒计时监听
     */
    interface OnCountDownFinishListener {
        fun onFinish()

        fun onTick(millisUntilFinished: Long)
    }

    companion object {
        private val PROGRESS_UPDATE_INTERNAL: Long = 1000
        private val PROGRESS_UPDATE_INITIAL_INTERVAL: Long = 100
    }
}
