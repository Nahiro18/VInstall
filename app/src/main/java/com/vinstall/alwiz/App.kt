package com.vinstall.alwiz

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.vinstall.alwiz.settings.AppSettings
import com.vinstall.alwiz.util.CrashHandler
import com.vinstall.alwiz.util.NotificationHelper

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
        NotificationHelper.createChannel(this)
        applyTheme(AppSettings.getTheme(this))
    }

    companion object {
        fun applyTheme(theme: String) {
            val mode = when (theme) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }
}
