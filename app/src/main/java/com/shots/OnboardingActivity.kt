package com.shots

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.shots.data.PreferencesManager
import com.shots.ui.onboarding.OnboardingScreen
import com.shots.ui.theme.ShotsTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferencesManager(this)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val done = prefs.onboardingDone.first()
                if (done) {
                    navigateToMain()
                    return@repeatOnLifecycle
                }
            }
        }

        setContent {
            val darkMode by prefs.darkMode.collectAsState(initial = 0)
            ShotsTheme(darkMode = darkMode) {
                OnboardingScreen(
                    onComplete = {
                        lifecycleScope.launch {
                            prefs.setOnboardingDone()
                            ShotsApp.startDetectionService(this@OnboardingActivity)
                            (application as ShotsApp).trackOnboardingCompleted()
                            navigateToMain()
                        }
                    }
                )
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
