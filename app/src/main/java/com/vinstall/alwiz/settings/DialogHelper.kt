package com.vinstall.alwiz.settings

import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.google.android.material.color.MaterialColors
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.vinstall.alwiz.R
import com.vinstall.alwiz.ui.ConfirmationBottomSheet

class DialogController(
    private val onDismiss: () -> Unit,
    private val onUpdateProgress: (step: String, progress: Float) -> Unit = { _, _ -> }
) {
    fun dismiss() = onDismiss()
    fun updateProgress(step: String, progress: Float) = onUpdateProgress(step, progress)
}

object DialogHelper {

    fun showConfirmation(
        activity: FragmentActivity,
        title: String,
        message: String = "",
        positiveLabel: String,
        negativeLabel: String,
        isDangerous: Boolean = false,
        appInstallInfo: ConfirmationBottomSheet.AppInstallInfo? = null,
        onDismissed: (() -> Unit)? = null,
        onConfirm: () -> Unit,
    ): DialogController {
        return when (AppSettings.getDialogStyle(activity)) {
            DialogStyle.BOTTOM_SHEET -> {
                val sheet = ConfirmationBottomSheet.show(
                    fragmentManager = activity.supportFragmentManager,
                    title = title,
                    message = message,
                    positiveLabel = positiveLabel,
                    negativeLabel = negativeLabel,
                    isDangerous = isDangerous,
                    appInstallInfo = appInstallInfo,
                    onDismissed = onDismissed,
                    onConfirm = onConfirm,
                )
                DialogController(
                    onDismiss = { if (sheet.isAdded) sheet.dismiss() },
                    onUpdateProgress = { step, progress -> sheet.setInstalling(step, progress) }
                )
            }
            DialogStyle.ALERT_DIALOG -> {
                val builder = AlertDialog.Builder(activity)
                    .setTitle(title)
                    .setPositiveButton(positiveLabel) { _, _ -> onConfirm() }
                    .setNegativeButton(negativeLabel) { _, _ -> onDismissed?.invoke() }
                    .setOnCancelListener { onDismissed?.invoke() }

                var progressLayout: View? = null
                var progressBar: LinearProgressIndicator? = null
                var progressText: TextView? = null

                if (appInstallInfo != null) {
                    val view = activity.layoutInflater.inflate(R.layout.dialog_app_info, null)
                    
                    // --- OPTIMIZACIÓN: Usar función helper compartida ---
                    ConfirmationBottomSheet.bindAppInfoToView(view, appInstallInfo, activity)
                    // ---------------------------------------------------
                    
                    // Solo obtener las referencias para progreso
                    progressLayout = view.findViewById(R.id.layout_progress)
                    progressBar = view.findViewById(R.id.progress_install)
                    progressText = view.findViewById(R.id.text_progress_status)

                    builder.setView(view)
                } else if (message.isNotEmpty()) {
                    builder.setMessage(message)
                }

                val dialog = builder.show()
                if (isDangerous) {
                    val errorColor = MaterialColors.getColor(
                        activity.window.decorView,
                        androidx.appcompat.R.attr.colorError
                    )
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(errorColor)
                }

                DialogController(
                    onDismiss = { if (dialog.isShowing) dialog.dismiss() },
                    onUpdateProgress = { step, progress ->
                        if (!dialog.isShowing) return@DialogController
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled = false
                        progressLayout?.isVisible = true
                        progressText?.text = if (progress >= 0f) {
                            "$step (${(progress * 100).toInt()}%)"
                        } else {
                            step
                        }
                        if (progress >= 0f) {
                            progressBar?.isIndeterminate = false
                            progressBar?.setProgressCompat((progress * 100).toInt(), true)
                        } else {
                            progressBar?.isIndeterminate = true
                        }
                    }
                )
            }
        }
    }
}
