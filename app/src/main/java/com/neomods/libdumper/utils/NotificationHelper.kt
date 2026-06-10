package com.neomods.libdumper.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.neomods.libdumper.LibDumperApp
import com.neomods.libdumper.MainActivity
import com.neomods.libdumper.R

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun showDumpProgress(stage: String, progress: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, LibDumperApp.CHANNEL_DUMP_PROGRESS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Dumping Library")
            .setContentText(stage)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .build()

        notificationManager.notify(LibDumperApp.NOTIFICATION_DUMP_ID, notification)
    }

    fun showDumpComplete(dumpPath: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, LibDumperApp.CHANNEL_DUMP_COMPLETION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Dump Complete")
            .setContentText("Output saved to: $dumpPath")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(LibDumperApp.NOTIFICATION_COMPLETION_ID, notification)
    }

    fun showDumpError(message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, LibDumperApp.CHANNEL_DUMP_COMPLETION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Dump Failed")
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(LibDumperApp.NOTIFICATION_COMPLETION_ID, notification)
    }

    fun cancelDumpProgress() {
        notificationManager.cancel(LibDumperApp.NOTIFICATION_DUMP_ID)
    }
}
