package com.shots

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.shots.data.PreferencesManager
import com.shots.ui.onboarding.OnboardingScreen
import com.shots.ui.theme.ShotsTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferencesManager(this)

        CoroutineScope(Dispatchers.IO).launch {
            val done = prefs.onboardingDone.first()
            if (done) {
                startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
                finish()
                return@launch
            }
        }

        setContent {
            ShotsTheme {
                OnboardingScreen(
                    onComplete = {
                        CoroutineScope(Dispatchers.IO).launch {
                            prefs.setOnboardingDone()
                        }
                        startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}
