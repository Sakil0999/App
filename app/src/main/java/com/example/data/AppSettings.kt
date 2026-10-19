package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsState(
    val themePreference: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val vibrateOnScan: Boolean = true,
    val beepOnScan: Boolean = true,
    val autoCopyToClipboard: Boolean = false,
    val confirmUrlBeforeOpening: Boolean = true,
    val totalScansDone: Int = 0
)

class AppSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("qr_scanner_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    private fun loadSettings(): SettingsState {
        return SettingsState(
            themePreference = prefs.getString("theme_preference", "SYSTEM") ?: "SYSTEM",
            vibrateOnScan = prefs.getBoolean("vibrate_on_scan", true),
            beepOnScan = prefs.getBoolean("beep_on_scan", true),
            autoCopyToClipboard = prefs.getBoolean("auto_copy", false),
            confirmUrlBeforeOpening = prefs.getBoolean("confirm_url", true),
            totalScansDone = prefs.getInt("total_scans_done", 0)
        )
    }

    fun setThemePreference(theme: String) {
        prefs.edit().putString("theme_preference", theme).apply()
        _settings.value = _settings.value.copy(themePreference = theme)
    }

    fun setVibrateOnScan(vibrate: Boolean) {
        prefs.edit().putBoolean("vibrate_on_scan", vibrate).apply()
        _settings.value = _settings.value.copy(vibrateOnScan = vibrate)
    }

    fun setBeepOnScan(beep: Boolean) {
        prefs.edit().putBoolean("beep_on_scan", beep).apply()
        _settings.value = _settings.value.copy(beepOnScan = beep)
    }

    fun setAutoCopyToClipboard(autoCopy: Boolean) {
        prefs.edit().putBoolean("auto_copy", autoCopy).apply()
        _settings.value = _settings.value.copy(autoCopyToClipboard = autoCopy)
    }

    fun setConfirmUrlBeforeOpening(confirm: Boolean) {
        prefs.edit().putBoolean("confirm_url", confirm).apply()
        _settings.value = _settings.value.copy(confirmUrlBeforeOpening = confirm)
    }

    fun incrementScansCount(): Int {
        val newCount = _settings.value.totalScansDone + 1
        prefs.edit().putInt("total_scans_done", newCount).apply()
        _settings.value = _settings.value.copy(totalScansDone = newCount)
        return newCount
    }
}
