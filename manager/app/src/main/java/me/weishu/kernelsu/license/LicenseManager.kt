package me.weishu.kernelsu.license

import android.content.Context
import android.provider.Settings
import android.util.Log
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object LicenseManager {
    private const val PREFS = "license_prefs"
    private const val KEY_LICENSE = "license_string"
    private const val KEY_ACTIVATED_AT = "activated_at"
    private const val SECRET = "1234567890abcdef1234567890abcdef"

    data class LicenseInfo(val deviceId: String, val expiresAt: Long)
    sealed class ValidationResult {
        data class Valid(val info: LicenseInfo, val daysLeft: Long) : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }

    fun getDeviceId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

    fun saveLicense(context: Context, license: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LICENSE, license.trim().uppercase()).apply()
        if (prefs.getLong(KEY_ACTIVATED_AT, 0L) == 0L) prefs.edit().putLong(KEY_ACTIVATED_AT, System.currentTimeMillis()).apply()
    }

    fun loadLicense(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LICENSE, null)

    fun validate(context: Context, license: String): ValidationResult {
        val trimmed = license.trim().uppercase()
        if (!trimmed.startsWith("ARTHUR-DR-")) return ValidationResult.Invalid("Invalid format")

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var activatedAt = prefs.getLong(KEY_ACTIVATED_AT, 0L)
        if (activatedAt == 0L) {
            activatedAt = System.currentTimeMillis()
            prefs.edit().putLong(KEY_ACTIVATED_AT, activatedAt).apply()
        }
        val expiresAt = activatedAt + (365L * 24 * 60 * 60 * 1000)
        if (System.currentTimeMillis() >= expiresAt) return ValidationResult.Invalid("License expired")
        val daysLeft = (expiresAt - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)

        // Universal Master Key
        if (trimmed == "ARTHUR-DR-00000") {
            return ValidationResult.Valid(LicenseInfo("MASTER", expiresAt), daysLeft)
        }

        val code = trimmed.removePrefix("ARTHUR-DR-")
        if (code.length != 5) return ValidationResult.Invalid("Invalid key length")
        if (code != generateCode(getDeviceId(context))) return ValidationResult.Invalid("Key does not match this device")

        return ValidationResult.Valid(LicenseInfo(getDeviceId(context), expiresAt), daysLeft)
    }

    private fun generateCode(deviceId: String): String {
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(SECRET.toByteArray(), "HmacSHA256"))
            mac.doFinal(deviceId.toByteArray()).joinToString("") { String.format("%02X", it) }.take(5)
        } catch (e: Exception) { "ERROR" }
    }
}
