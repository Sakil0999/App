package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.AppBottomNavBar
import com.example.ui.components.NavTab
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AdMobHelper

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val settings by mainViewModel.settings.collectAsStateWithLifecycle()

            MyApplicationTheme(
                themePreference = settings.themePreference
            ) {
                MainAppNavigation(
                    viewModel = mainViewModel,
                    activity = this
                )
            }
        }
    }
}

@Composable
fun MainAppNavigation(
    viewModel: MainViewModel,
    activity: ComponentActivity
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val currentResult by viewModel.currentResult.collectAsStateWithLifecycle()

    when (currentScreen) {
        AppScreen.SCANNER -> {
            ScannerScreen(
                viewModel = viewModel,
                onBack = {
                    viewModel.navigateBack()
                }
            )
        }

        AppScreen.RESULT -> {
            val result = currentResult
            if (result != null) {
                ResultScreen(
                    barcode = result,
                    viewModel = viewModel,
                    onBack = {
                        // Natural transition: optionally display interstitial ad with frequency capping
                        AdMobHelper.showInterstitialIfAppropriate(activity) {
                            viewModel.navigateBack()
                        }
                    }
                )
            } else {
                viewModel.navigateBack()
            }
        }

        AppScreen.TABS -> {
            BackHandler(enabled = selectedTab != NavTab.HOME) {
                viewModel.setTab(NavTab.HOME)
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    AppBottomNavBar(
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            viewModel.setTab(tab)
                        }
                    )
                }
            ) { innerPadding ->
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "TabContentTransition",
                    modifier = Modifier.padding(innerPadding)
                ) { tab ->
                    when (tab) {
                        NavTab.HOME -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToScanner = { viewModel.openScanner() },
                                onNavigateToHistory = { viewModel.setTab(NavTab.HISTORY) }
                            )
                        }

                        NavTab.HISTORY -> {
                            HistoryScreen(
                                viewModel = viewModel,
                                onItemSelected = { scanItem ->
                                    viewModel.openResultFromScanItem(scanItem)
                                }
                            )
                        }

                        NavTab.SETTINGS -> {
                            SettingsScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
