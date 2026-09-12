package com.screenshotguard.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.screenshotguard.MainActivity
import com.screenshotguard.OnboardingActivity
import com.screenshotguard.ScreenshotGuardApp
import com.screenshotguard.ui.theme.ShotsTheme
import kotlinx.coroutines.delay
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        val app = application as ScreenshotGuardApp

        setContent {
            ShotsTheme {
                SplashContent()
            }
        }

        lifecycleScope.launch {
            delay(1500)
            val next = if (app.preferencesManager.getOnboardingComplete()) {
                Intent(this@SplashActivity, MainActivity::class.java)
            } else {
                Intent(this@SplashActivity, OnboardingActivity::class.java)
            }
            startActivity(next)
            finish()
        }
    }
}
