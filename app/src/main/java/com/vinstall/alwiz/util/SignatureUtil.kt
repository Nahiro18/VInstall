package com.vinstall.alwiz.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest
import java.security.cert.Certificate
import java.security.cert.CertificateEncodingException
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SignatureUtil {

    data class SignatureInfo(
        val signerName: String?,
        val fingerprintMD5: String,
        val fingerprintSHA1: String,
        val fingerprintSHA256: String,
        val validFrom: String?,
        val validUntil: String?,
        val isValid: Boolean
    )

    /**
     * Extrae la información de firma de un APK sin instalarlo.
     * @param context Contexto de Android
     * @param apkPath Ruta absoluta del archivo APK
     * @return SignatureInfo con los detalles de la firma, o null si no se pudo leer
     */
    fun getApkSignatureInfo(context: Context, apkPath: String): SignatureInfo? {
        return try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES.toLong()
            } else {
                PackageManager.GET_SIGNATURES.toLong()
            }

            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageArchiveInfo(apkPath, PackageManager.PackageInfoFlags.of(flags))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageArchiveInfo(apkPath, flags.toInt())
            } ?: return null

            val certificates = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.signingInfo?.signingCertificateHistory ?: emptyArray()
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.signatures ?: emptyArray()
            }

            if (certificates.isEmpty()) return null

            val cert = certificates[0]
            val certFactory = java.security.cert.CertificateFactory.getInstance("X.509")
            val x509 = certFactory.generateCertificate(java.io.ByteArrayInputStream(cert.toByteArray())) as? X509Certificate ?: return null

            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val validFrom = try { dateFormat.format(x509.notBefore) } catch (_: Exception) { null }
            val validUntil = try { dateFormat.format(x509.notAfter) } catch (_: Exception) { null }

            SignatureInfo(
                signerName = x509.subjectDN?.name,
                fingerprintMD5 = getFingerprint(cert, "MD5"),
                fingerprintSHA1 = getFingerprint(cert, "SHA-1"),
                fingerprintSHA256 = getFingerprint(cert, "SHA-256"),
                validFrom = validFrom,
                validUntil = validUntil,
                isValid = isCertificateValid(x509)
            )
        } catch (e: Exception) {
            DebugLog.e("SignatureUtil", "Error reading signature: ${e.message}")
            null
        }
    }

    private fun getFingerprint(cert: Certificate, algorithm: String): String {
        return try {
            val digest = MessageDigest.getInstance(algorithm)
            val bytes = digest.digest(cert.encoded)
            bytes.joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun isCertificateValid(cert: X509Certificate): Boolean {
        return try {
            cert.checkValidity()
            true
        } catch (_: Exception) {
            false
        }
    }
}
