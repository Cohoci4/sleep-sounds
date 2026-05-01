package com.sleepsounds.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.getSystemService
import com.sleepsounds.app.audio.SleepPlaybackService
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class SleepSoundsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }
        createPlaybackChannel()
    }

    private fun createPlaybackChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SleepPlaybackService.CHANNEL_ID,
                getString(R.string.playback_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.playback_channel_description)
                setSound(null, null)
                enableVibration(false)
            }
            getSystemService<NotificationManager>()?.createNotificationChannel(channel)
        }
    }
}

private class CrashReportingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == android.util.Log.VERBOSE || priority == android.util.Log.DEBUG) {
            return
        }
        runCatching {
            val crashlytics = com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
            crashlytics.log(message)
            t?.let(crashlytics::recordException)
        }
    }
}
