package com.shots.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.shots.ScreenshotOverlayActivity
import com.shots.data.PreferencesManager
import com.shots.ui.overlay.OverlayScreen
import com.shots.ui.theme.ShotsTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Shows the screenshot popup as a true overlay window
 * (TYPE_APPLICATION_OVERLAY) instead of launching an activity.
 * Works on any device: activity launches from a background service
 * are blocked by the system on Android 10+, but overlay windows
 * only need the Display-over-apps permission (asked in onboarding).
 * Falls back to ScreenshotOverlayActivity when permission is missing.
 */
object OverlayController {

    private const val TAG = "OverlayController"

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: ManagedOwner? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun show(context: Context, screenshotPath: String) {
        if (!Settings.canDrawOverlays(context)) {
            launchActivity(context, screenshotPath)
            return
        }
        scope.launch {
            try {
                hide()
                val prefs = PreferencesManager(context)
                val darkMode = try {
                    prefs.darkMode.first()
                } catch (_: Exception) {
                    0
                }
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val view = ComposeView(context).apply {
                    setContent {
                        ShotsTheme(darkMode = darkMode) {
                            OverlayScreen(
                                screenshotPath = screenshotPath,
                                onDismiss = { hide() }
                            )
                        }
                    }
                }
                attachLifecycle(view).also { lifecycleOwner = it }
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_PHONE
                    },
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                )
                wm.addView(view, params)
                windowManager = wm
                overlayView = view
            } catch (e: Exception) {
                Log.e(TAG, "Overlay window failed, falling back to activity", e)
                hide()
                launchActivity(context, screenshotPath)
            }
        }
    }

    fun hide() {
        try {
            val wm = windowManager
            val view = overlayView
            val owner = lifecycleOwner
            overlayView = null
            windowManager = null
            lifecycleOwner = null
            if (wm != null && view != null) {
                view.disposeComposition()
                wm.removeViewImmediate(view)
            }
            owner?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "hide failed", e)
        }
    }

    private fun launchActivity(context: Context, screenshotPath: String) {
        try {
            val overlayIntent = Intent(context, ScreenshotOverlayActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
                putExtra("screenshot_path", screenshotPath)
            }
            context.startActivity(overlayIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Activity fallback failed", e)
        }
    }

    private class ManagedOwner : ViewModelStoreOwner, SavedStateRegistryOwner,
        androidx.lifecycle.LifecycleOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val store = ViewModelStore()
        private val savedStateController = SavedStateRegistryController.create(this)

        init {
            savedStateController.performAttach()
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun destroy() {
            try {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                store.clear()
            } catch (_: Exception) {
            }
        }

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val viewModelStore: ViewModelStore get() = store
        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateController.savedStateRegistry
    }

    private fun attachLifecycle(view: ComposeView): ManagedOwner {
        val owner = ManagedOwner()
        view.setViewTreeLifecycleOwner(owner)
        view.setViewTreeViewModelStoreOwner(owner)
        view.setViewTreeSavedStateRegistryOwner(owner)
        return owner
    }

    fun destroy() {
        hide()
        scope.cancel()
    }
}
