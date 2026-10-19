package com.example.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdMobHelper {
    private const val TAG = "AdMobHelper"

    // Official Google Test Ad Unit IDs
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    private var interstitialAd: InterstitialAd? = null
    private var isAdLoading = false
    private var scanCountSinceLastAd = 0
    private const val SCANS_BETWEEN_INTERSTITIAL = 6 // Natural frequency cap

    fun initialize(context: Context) {
        try {
            MobileAds.initialize(context) {
                Log.d(TAG, "Mobile Ads initialized successfully")
                loadInterstitial(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Mobile Ads", e)
        }
    }

    fun loadInterstitial(context: Context) {
        if (interstitialAd != null || isAdLoading) return
        isAdLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            TEST_INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isAdLoading = false
                    Log.d(TAG, "Interstitial ad loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isAdLoading = false
                    Log.d(TAG, "Interstitial failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * Show interstitial ad only at natural user transitions (e.g., when returning to Home),
     * respecting the frequency limit so user workflow is uninterrupted.
     */
    fun showInterstitialIfAppropriate(activity: Activity, onDismiss: () -> Unit = {}) {
        scanCountSinceLastAd++
        if (scanCountSinceLastAd >= SCANS_BETWEEN_INTERSTITIAL && interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    scanCountSinceLastAd = 0
                    loadInterstitial(activity)
                    onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    scanCountSinceLastAd = 0
                    loadInterstitial(activity)
                    onDismiss()
                }
            }
            interstitialAd?.show(activity)
        } else {
            onDismiss()
        }
    }
}
