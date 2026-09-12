package com.screenshotguard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.screenshotguard.ui.onboarding.OnboardingContent
import com.screenshotguard.ui.theme.ShotsTheme

class OnboardingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as ScreenshotGuardApp

        setContent {
            ShotsTheme {
                OnboardingContent(
                    onComplete = {
                        app.preferencesManager.setOnboardingComplete(true)
                        startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}
