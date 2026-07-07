package com.vinstall.alwiz.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.vinstall.alwiz.R

object NotificationHelper {

    private const val CHANNEL_ID = "vinstall_install"
    private const val NOTIF_ID_INSTALLING = 1001
    private var nextNotificationId = 2000

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notify_channel_install),
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    fun postInstalling(context: Context, appLabel: String, step: String) {
        val label = appLabel.ifBlank { context.getString(R.string.app_name) }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.getString(R.string.notify_installing_title, label))
            .setContentText(step)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID_INSTALLING, notification)
    }

    fun updateProgress(context: Context, appLabel: String, progress: Float) {
        val label = appLabel.ifBlank { context.getString(R.string.app_name) }
        val percent = (progress * 100).toInt().coerceIn(0, 100)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.getString(R.string.notify_installing_title, label))
            .setContentText("$percent%")
            .setOngoing(true)
            .setProgress(100, percent, false)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID_INSTALLING, notification)
    }

    fun updateInstallingStep(context: Context, appLabel: String, step: String) {
        postInstalling(context, appLabel, step)
    }

    fun cancelInstalling(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIF_ID_INSTALLING)
    }

    fun postInstallCancelled(context: Context, appLabel: String, reason: String) {
        val label = appLabel.ifBlank { context.getString(R.string.app_name) }
        val body = reason.ifBlank { context.getString(R.string.notify_install_cancelled_body) }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(context.getString(R.string.notify_install_cancelled_title, label))
            .setContentText(body)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID_INSTALLING, notification)
    }

    fun postInstallSuccess(context: Context, packageName: String, appLabel: String) {
        cancelInstalling(context)
        val label = appLabel.ifBlank { packageName }.ifBlank { context.getString(R.string.app_name) }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.notify_install_success_title))
            .setContentText(context.getString(R.string.notify_install_success_body, label))
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(nextNotificationId++, notification)
    }

    fun postInstallFailure(context: Context, appLabel: String, reason: String) {
        cancelInstalling(context)
        val label = appLabel.ifBlank { context.getString(R.string.app_name) }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(context.getString(R.string.notify_install_failed_title, label))
            .setContentText(reason)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(nextNotificationId++, notification)
    }
}
