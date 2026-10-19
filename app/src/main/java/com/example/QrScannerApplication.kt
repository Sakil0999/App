package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.AppSettings
import com.example.data.ScanRepository
import com.example.util.AdMobHelper
import com.example.util.FeedbackHelper

class QrScannerApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: ScanRepository
        private set

    lateinit var appSettings: AppSettings
        private set

    lateinit var feedbackHelper: FeedbackHelper
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        repository = ScanRepository(database.scanDao())
        appSettings = AppSettings(this)
        feedbackHelper = FeedbackHelper(this)

        // Initialize Google Mobile Ads SDK for test ads
        AdMobHelper.initialize(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        feedbackHelper.release()
    }
}
