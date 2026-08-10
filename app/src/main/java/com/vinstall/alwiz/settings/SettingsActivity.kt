package com.vinstall.alwiz.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.vinstall.alwiz.App
import com.vinstall.alwiz.R
import com.vinstall.alwiz.databinding.ActivitySettingsBinding
import com.vinstall.alwiz.util.DebugLog

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        loadCurrentSettings()
        setupListeners()
    }

    private fun loadCurrentSettings() {
        val theme = AppSettings.getTheme(this)
        binding.textCurrentTheme.text = when (theme) {
            "light" -> getString(R.string.theme_light)
            "dark" -> getString(R.string.theme_dark)
            "amoled" -> getString(R.string.theme_amoled)
            else -> getString(R.string.theme_system)
        }

        val installMode = AppSettings.getInstallMode(this)
        binding.textCurrentInstallMode.text = when (installMode) {
            InstallMode.ROOT -> getString(R.string.mode_root)
            InstallMode.SHIZUKU -> getString(R.string.mode_shizuku)
            else -> getString(R.string.mode_normal)
        }

        val confirmInstall = AppSettings.isConfirmInstall(this)
        binding.switchConfirmInstall.isChecked = confirmInstall

        val clearCache = AppSettings.isClearCacheAfterInstall(this)
        binding.switchClearCache.isChecked = clearCache

        val debugEnabled = AppSettings.isDebugWindowEnabled(this)
        binding.switchDebugWindow.isChecked = debugEnabled

        val dialogStyle = AppSettings.getDialogStyle(this)
        binding.textCurrentDialogStyle.text = when (dialogStyle) {
            DialogStyle.BOTTOM_SHEET -> getString(R.string.dialog_style_bottom_sheet)
            else -> getString(R.string.dialog_style_alert_dialog)
        }
    }

    private fun setupListeners() {
        binding.layoutTheme.setOnClickListener {
            showThemeDialog()
        }

        binding.layoutInstallMode.setOnClickListener {
            showInstallModeDialog()
        }

        binding.switchConfirmInstall.setOnCheckedChangeListener { _, isChecked ->
            AppSettings.setConfirmInstall(this, isChecked)
        }

        binding.switchClearCache.setOnCheckedChangeListener { _, isChecked ->
            AppSettings.setClearCacheAfterInstall(this, isChecked)
        }

        binding.switchDebugWindow.setOnCheckedChangeListener { _, isChecked ->
            AppSettings.setDebugWindowEnabled(this, isChecked)
        }

        binding.layoutDialogStyle.setOnClickListener {
            showDialogStyleDialog()
        }

        binding.layoutCrashReports.setOnClickListener {
            val intent = android.content.Intent(this, com.vinstall.alwiz.util.CrashLogActivity::class.java)
            startActivity(intent)
        }

        binding.layoutHistory.setOnClickListener {
            val intent = android.content.Intent(this, com.vinstall.alwiz.history.InstallHistoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showThemeDialog() {
        val themes = arrayOf(
            getString(R.string.theme_system),
            getString(R.string.theme_light),
            getString(R.string.theme_dark),
            getString(R.string.theme_amoled)
        )
        val keys = arrayOf("system", "light", "dark", "amoled")
        val current = AppSettings.getTheme(this)
        val idx = keys.indexOf(current).coerceAtLeast(0)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.choose_theme))
            .setSingleChoiceItems(themes, idx) { dialog, which ->
                AppSettings.setTheme(this, keys[which])
                binding.textCurrentTheme.text = themes[which]
                applyTheme(keys[which])
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showInstallModeDialog() {
        val modes = arrayOf(
            getString(R.string.mode_normal),
            getString(R.string.mode_root),
            getString(R.string.mode_shizuku)
        )
        val keys = arrayOf(InstallMode.NORMAL, InstallMode.ROOT, InstallMode.SHIZUKU)
        val current = AppSettings.getInstallMode(this)
        val idx = keys.indexOf(current).coerceAtLeast(0)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.settings_section_install_mode))
            .setSingleChoiceItems(modes, idx) { dialog, which ->
                AppSettings.setInstallMode(this, keys[which])
                binding.textCurrentInstallMode.text = modes[which]
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDialogStyleDialog() {
        val styles = arrayOf(
            getString(R.string.dialog_style_bottom_sheet),
            getString(R.string.dialog_style_alert_dialog)
        )
        val keys = arrayOf(DialogStyle.BOTTOM_SHEET, DialogStyle.ALERT_DIALOG)
        val current = AppSettings.getDialogStyle(this)
        val idx = keys.indexOf(current).coerceAtLeast(0)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.choose_dialog_style))
            .setSingleChoiceItems(styles, idx) { dialog, which ->
                AppSettings.setDialogStyle(this, keys[which])
                binding.textCurrentDialogStyle.text = styles[which]
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun applyTheme(theme: String) {
        App.applyTheme(theme)
        DebugLog.i("Settings", "Theme changed to: $theme")
        
        // Recrear la actividad para aplicar el tema inmediatamente
        recreate()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
