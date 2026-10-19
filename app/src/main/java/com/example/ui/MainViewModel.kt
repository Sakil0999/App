package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.QrScannerApplication
import com.example.data.ScanItem
import com.example.data.SettingsState
import com.example.parser.BarcodeParser
import com.example.parser.BarcodeType
import com.example.parser.ParsedBarcode
import com.example.util.ActionHelper
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    TABS,
    SCANNER,
    RESULT
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as QrScannerApplication
    private val repository = app.repository
    private val appSettings = app.appSettings
    private val feedbackHelper = app.feedbackHelper

    // Navigation and screen state
    private val _currentScreen = MutableStateFlow(AppScreen.TABS)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedTab = MutableStateFlow(com.example.ui.components.NavTab.HOME)
    val selectedTab: StateFlow<com.example.ui.components.NavTab> = _selectedTab.asStateFlow()

    // Current scanned result
    private val _currentResult = MutableStateFlow<ParsedBarcode?>(null)
    val currentResult: StateFlow<ParsedBarcode?> = _currentResult.asStateFlow()

    // Duplicate detection suppression
    private var lastScannedRaw: String? = null
    private var lastScannedTime: Long = 0L

    // Camera settings state
    private val _torchEnabled = MutableStateFlow(false)
    val torchEnabled: StateFlow<Boolean> = _torchEnabled.asStateFlow()

    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val lensFacing: StateFlow<Int> = _lensFacing.asStateFlow()

    private val _zoomRatio = MutableStateFlow(1f)
    val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()

    // History and search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedHistoryFilter = MutableStateFlow<String>("ALL")
    val selectedHistoryFilter: StateFlow<String> = _selectedHistoryFilter.asStateFlow()

    val allScans: StateFlow<List<ScanItem>> = repository.allScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredScans: StateFlow<List<ScanItem>> = combine(
        repository.allScans,
        _searchQuery,
        _selectedHistoryFilter
    ) { scans, query, filter ->
        scans.filter { item ->
            val matchesQuery = query.isEmpty() ||
                    item.rawText.contains(query, ignoreCase = true) ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.format.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                "ALL" -> true
                "QR" -> item.format.contains("QR", ignoreCase = true)
                "BARCODE" -> !item.format.contains("QR", ignoreCase = true)
                "URL" -> item.type == BarcodeType.URL.name
                "WIFI" -> item.type == BarcodeType.WIFI.name
                "FAVORITES" -> item.isFavorite
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<SettingsState> = appSettings.settings

    // Gallery scanning status
    private val _galleryProcessing = MutableStateFlow(false)
    val galleryProcessing: StateFlow<Boolean> = _galleryProcessing.asStateFlow()

    private val _galleryErrorMessage = MutableStateFlow<String?>(null)
    val galleryErrorMessage: StateFlow<String?> = _galleryErrorMessage.asStateFlow()

    fun setTab(tab: com.example.ui.components.NavTab) {
        _selectedTab.value = tab
        _currentScreen.value = AppScreen.TABS
    }

    fun openScanner() {
        _currentScreen.value = AppScreen.SCANNER
    }

    fun openResult(barcode: ParsedBarcode) {
        _currentResult.value = barcode
        _currentScreen.value = AppScreen.RESULT
    }

    fun openResultFromScanItem(item: ScanItem) {
        val parsed = BarcodeParser.parseFromRaw(item.rawText, item.format, item.barcodeFormatInt)
        openResult(parsed)
    }

    fun navigateBack(): Boolean {
        if (_currentScreen.value != AppScreen.TABS) {
            _currentScreen.value = AppScreen.TABS
            return true
        }
        return false
    }

    fun toggleTorch() {
        _torchEnabled.value = !_torchEnabled.value
    }

    fun setTorch(enabled: Boolean) {
        _torchEnabled.value = enabled
    }

    fun toggleCameraFacing() {
        _lensFacing.value = if (_lensFacing.value == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        // Front camera usually doesn't have torch
        if (_lensFacing.value == CameraSelector.LENS_FACING_FRONT) {
            _torchEnabled.value = false
        }
    }

    fun setZoom(zoom: Float) {
        _zoomRatio.value = zoom.coerceIn(1f, 5f)
    }

    fun onBarcodeScanned(barcode: Barcode) {
        val raw = barcode.rawValue ?: barcode.displayValue ?: return
        val now = System.currentTimeMillis()

        // Debounce: prevent duplicate scan trigger if pointed at same code within 2.5 seconds
        if (raw == lastScannedRaw && (now - lastScannedTime) < 2500) {
            return
        }

        lastScannedRaw = raw
        lastScannedTime = now

        val parsed = BarcodeParser.parse(barcode)
        handleNewBarcode(parsed)
    }

    private fun handleNewBarcode(parsed: ParsedBarcode) {
        val currentSettings = settings.value

        // Haptic feedback
        if (currentSettings.vibrateOnScan) {
            feedbackHelper.vibrate()
        }

        // Sound feedback
        if (currentSettings.beepOnScan) {
            feedbackHelper.playBeep()
        }

        // Auto copy to clipboard
        if (currentSettings.autoCopyToClipboard) {
            ActionHelper.copyToClipboard(app, parsed.rawText)
        }

        // Save locally to Room database
        viewModelScope.launch {
            val scanItem = ScanItem(
                rawText = parsed.rawText,
                format = parsed.formatName,
                barcodeFormatInt = parsed.barcodeFormatInt,
                type = parsed.type.name,
                title = parsed.title,
                detailsJson = parsed.subtitle,
                timestamp = System.currentTimeMillis()
            )
            repository.insert(scanItem)
            appSettings.incrementScansCount()
        }

        // Navigate to dedicated result screen
        _currentResult.value = parsed
        _currentScreen.value = AppScreen.RESULT
    }

    fun scanImageUri(context: Context, uri: Uri) {
        _galleryProcessing.value = true
        _galleryErrorMessage.value = null

        viewModelScope.launch {
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                val scanner = BarcodeScanning.getClient()

                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        _galleryProcessing.value = false
                        if (barcodes.isNotEmpty()) {
                            val parsed = BarcodeParser.parse(barcodes[0])
                            handleNewBarcode(parsed)
                        } else {
                            _galleryErrorMessage.value =
                                "No QR code or barcode found in this image. Try another image with clear lighting."
                        }
                    }
                    .addOnFailureListener { e ->
                        _galleryProcessing.value = false
                        _galleryErrorMessage.value = "Failed to scan image: ${e.localizedMessage ?: "Unknown error"}"
                    }
            } catch (e: Exception) {
                _galleryProcessing.value = false
                _galleryErrorMessage.value = "Unable to read image file: ${e.localizedMessage}"
            }
        }
    }

    fun dismissGalleryError() {
        _galleryErrorMessage.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setHistoryFilter(filter: String) {
        _selectedHistoryFilter.value = filter
    }

    fun deleteScan(item: ScanItem) {
        viewModelScope.launch {
            repository.delete(item)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun toggleFavorite(item: ScanItem) {
        viewModelScope.launch {
            repository.setFavorite(item.id, !item.isFavorite)
        }
    }

    fun setTheme(theme: String) {
        appSettings.setThemePreference(theme)
    }

    fun setVibrate(vibrate: Boolean) {
        appSettings.setVibrateOnScan(vibrate)
    }

    fun setBeep(beep: Boolean) {
        appSettings.setBeepOnScan(beep)
    }

    fun setAutoCopy(autoCopy: Boolean) {
        appSettings.setAutoCopyToClipboard(autoCopy)
    }

    fun setConfirmUrl(confirm: Boolean) {
        appSettings.setConfirmUrlBeforeOpening(confirm)
    }
}
