package com.neomods.libdumper

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LibDumperApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val dumpChannel = NotificationChannel(
                CHANNEL_DUMP_PROGRESS,
                "Dump Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of ELF dump operations"
                setShowBadge(false)
            }

            val completionChannel = NotificationChannel(
                CHANNEL_DUMP_COMPLETION,
                "Dump Completion",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when dump operations complete"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(dumpChannel)
            notificationManager.createNotificationChannel(completionChannel)
        }
    }

    companion object {
        const val CHANNEL_DUMP_PROGRESS = "dump_progress"
        const val CHANNEL_DUMP_COMPLETION = "dump_completion"
        const val NOTIFICATION_DUMP_ID = 1001
        const val NOTIFICATION_COMPLETION_ID = 1002
    }
}
