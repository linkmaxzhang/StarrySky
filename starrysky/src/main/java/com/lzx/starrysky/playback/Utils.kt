package com.lzx.starrysky.playback

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.exoplayer2.ExoPlayerLibraryInfo

object Utils {
    fun getUserAgent(context: Context, applicationName: String): String {
        val versionName: String
        versionName = try {
            val packageName = context.packageName
            val info = context.packageManager.getPackageInfo(packageName, 0)
            info.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "?"
        }

        return (applicationName + "/" + versionName + " (Linux;Android " + Build.VERSION.RELEASE
                + ") " + ExoPlayerLibraryInfo.VERSION_SLASHY)
    }

}
