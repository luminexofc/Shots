# Shots - Android Screenshot Manager

## Overview

**Shots** is a Kotlin-only Android app that detects screenshots and shows an overlay popup, letting users **Keep**, **Delete After X minutes**, or **Skip** each screenshot. Includes full settings, history, and auto-delete system.

---

## Project Metadata

| Field | Value |
|---|---|
| Package Name | `com.screenshotguard` |
| App Name | Shots |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |
| Compile SDK | 35 |
| Version Name | 2.0 |
| Version Code | 3 |
| Language | Kotlin |
| Build System | Gradle 8.11.1 |
| AGP Version | 8.7.3 |
| GitHub Repo | `https://github.com/EchoBolt-07/Shots` |

---

## Build Environment

- **OS**: Termux on Android (ARM aarch64)
- **Java**: OpenJDK 17 (`/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk`)
- **Android SDK**: `/data/data/com.termux/files/home/android-sdk`
- **Gradle User Home**: `/data/data/com.termux/files/home/.gradle`
- **aapt2 Override**: `android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2`
- **SSH Config**: `~/.ssh/config` uses IP `20.205.243.166` for github.com
- **Available SDK Platforms**: android-35, android-36, android-37
- **Cached Gradle Versions**: 8.10.2, 8.11.1, 8.14.3, 9.3.1

---

## GitHub Repository Access

| Method | URL |
|---|---|
| HTTPS Clone | `https://github.com/EchoBolt-07/Shots.git` |
| SSH Clone | `git@github.com:EchoBolt-07/Shots.git` |
| Web URL | `https://github.com/EchoBolt-07/Shots` |
| Branch | `main` |
| SSH Host | `github.com` (IP: `20.205.243.166`) |
| SSH Config | `~/.ssh/config` |

```bash
# Clone via HTTPS
git clone https://github.com/EchoBolt-07/Shots.git

# Clone via SSH
git clone git@github.com:EchoBolt-07/Shots.git

# Push changes
git add .
git commit -m "your message"
git push origin main
```

---

## Architecture

### Package Structure

```
com.screenshotguard/
├── MainActivity.kt
├── SettingsActivity.kt
├── HistoryActivity.kt
├── OnboardingActivity.kt
├── PermissionsActivity.kt
├── ScreenshotOverlayActivity.kt
├── data/
│   ├── Screenshot.kt (Entity)
│   ├── ScreenshotDao.kt (DAO)
│   └── ScreenshotDatabase.kt (Room DB)
├── service/
│   └── ScreenshotDetectionService.kt (Foreground Service)
├── receiver/
│   └── BootReceiver.kt
├── worker/
│   └── AutoDeleteWorker.kt (WorkManager)
├── ui/
│   ├── theme/
│   │   ├── ShotsTheme.kt
│   │   └── Type.kt
│   ├── onboarding/
│   │   └── OnboardingScreen.kt
│   ├── permissions/
│   │   └── PermissionManagerScreen.kt
│   ├── main/
│   │   └── MainScreen.kt
│   ├── overlay/
│   │   └── OverlayScreen.kt
│   ├── settings/
│   │   └── SettingsScreen.kt
│   ├── history/
│   │   └── HistoryScreen.kt
│   └── components/
│       ├── ShotsCard.kt
│       └── SegmentedControl.kt
└── res/
    ├── mipmap-*/ (launcher icons from icon.png)
    ├── values/themes.xml
    └── xml/file_paths.xml
```

---

## Design System: Mono Ink Minimal

### Color Palette

**Dark Mode (Default)**
| Token | Hex | Usage |
|---|---|---|
| Background | `#000000` | Screen background |
| Surface | `#0A0A0A` | Cards, sheets |
| Surface Hover | `#141414` | Interactive hover states |
| Border | `#1F1F1F` | Dividers, card borders |
| Border Subtle | `#141414` | Subtle separators |
| Text Primary | `#FAFAFA` | Headlines, primary text |
| Text Secondary | `#A1A1AA` | Body, descriptions |
| Text Tertiary | `#52525B` | Hints, captions |
| Icon Primary | `#FAFAFA` | Main icons |
| Icon Secondary | `#71717A` | Supporting icons |
| Disabled BG | `#141414` | Disabled backgrounds |
| Disabled Border | `#1F1F1F` | Disabled borders |
| Disabled Content | `#52525B` | Disabled text |
| Scrim | `#000000 @ 60%` | Modal overlays |

**Light Mode**
| Token | Hex | Usage |
|---|---|---|
| Background | `#FAFAFA` | Screen background |
| Surface | `#FFFFFF` | Cards, sheets |
| Surface Hover | `#F5F5F5` | Interactive hover states |
| Border | `#E5E5E5` | Dividers, card borders |
| Border Subtle | `#F0F0F0` | Subtle separators |
| Text Primary | `#0A0A0A` | Headlines, primary text |
| Text Secondary | `#52525B` | Body, descriptions |
| Text Tertiary | `#A1A1AA` | Hints, captions |
| Icon Primary | `#0A0A0A` | Main icons |
| Icon Secondary | `#71717A` | Supporting icons |
| Disabled BG | `#F5F5F5` | Disabled backgrounds |
| Disabled Border | `#E5E5E5` | Disabled borders |
| Disabled Content | `#A1A1AA` | Disabled text |
| Scrim | `#000000 @ 38%` | Modal overlays |

### Accent Colors

| Token | Dark | Light | Usage |
|---|---|---|---|
| Primary | `#FAFAFA` | `#0A0A0A` | Buttons, active states |
| On Primary | `#000000` | `#FFFFFF` | Text on primary |
| Destructive | `#EF4444` | `#DC2626` | Delete actions |
| On Destructive | `#FFFFFF` | `#FFFFFF` | Text on destructive |
| Success | `#22C55E` | `#16A34A` | Confirmations |
| On Success | `#000000` | `#FFFFFF` | Text on success |
| Warning | `#EAB308` | `#CA8A04` | Timer states |
| On Warning | `#000000` | `#FFFFFF` | Text on warning |

### Typography

Using Material3 `Typography` with custom `ShotsTypography` object. Font family: Default (system).

| Style | Size | Weight | Usage |
|---|---|---|---|
| displayLarge | 57sp | Bold | N/A |
| displayMedium | 45sp | Bold | N/A |
| displaySmall | 36sp | Bold | N/A |
| headlineLarge | 32sp | Bold | Screen titles |
| headlineMedium | 28sp | SemiBold | Section headers |
| headlineSmall | 24sp | SemiBold | N/A |
| titleLarge | 22sp | SemiBold | Card titles |
| titleMedium | 16sp | Medium | List item titles |
| titleSmall | 14sp | Medium | N/A |
| bodyLarge | 16sp | Normal | Descriptions |
| bodyMedium | 14sp | Normal | Secondary text |
| bodySmall | 12sp | Normal | Captions, hints |
| labelLarge | 14sp | Medium | Buttons, chips |
| labelMedium | 12sp | Medium | Small labels |
| labelSmall | 11sp | Medium | N/A |

---

## UI Screens

### 1. Onboarding Screen (`OnboardingActivity` + `OnboardingScreen.kt`)

4-page horizontal pager flow:

**Page 1 - Welcome**
- Animated pulsing camera icon
- App name + description

**Page 2 - Permissions**
- Storage Access (required)
- Display Over Apps (required)
- Notifications (required)
- Each permission shows Grant button or checkmark
- Uses `LifecycleEventObserver` to re-check overlay permission on `ON_RESUME`

**Page 3 - How It Works**
- Step 1: Take a Screenshot
- Step 2: Popup Appears
- Step 3: You Decide

**Page 4 - Ready**
- Animated pulsing checkmark
- "You're All Set!" message

Navigation:
- Skip button on pages 0-2 (jumps to page 3)
- Next/Continue button advances pages
- Dot indicators show current page
- Final button calls `onComplete()`

### 2. Main Screen (`MainActivity` + `MainScreen.kt`)

- Top bar: "Shots" title + Settings icon
- Overview card: Total screenshots count + Pending deletion count
- Quick Actions card: View History, How It Works
- Uses `ShotsTheme`

### 3. Overlay Screen (`ScreenshotOverlayActivity` + `OverlayScreen.kt`)

- Scrim background (60% black)
- Centered card with:
  - "Screenshot Detected" title
  - "What would you like to do?" subtitle
  - 3 action buttons: Keep (primary), Delete (destructive), Timer (primary)
  - Skip text button
- Timer picker dialog: 1, 5, 15, 30, 60 minute options
- Uses `ShotsTheme`

### 4. Settings Screen (`SettingsActivity` + `SettingsScreen.kt`)

- Back arrow + "Settings" title
- Default Action card: Keep / Timer / Skip chips
- Timer slider (1-60 min) when Timer selected
- Auto-Delete card: Toggle switch
- Notifications card: Toggle switch
- Uses `ShotsTheme`

### 5. History Screen (`HistoryActivity` + `HistoryScreen.kt`)

- Back arrow + "History" title
- Filter chips: All / Kept / Deleted
- LazyColumn of screenshot items
- Each item shows: status icon (colored), filename, timestamp
- Empty state: PhotoLibrary icon + "No screenshots yet"
- Uses `ShotsTheme`

### 6. Permission Manager Screen (`PermissionsActivity` + `PermissionManagerScreen.kt`)

- Back arrow + "Permissions" title
- Storage Access: Grant button or checkmark
- Display Over Apps: Grant button or checkmark
- Notifications: Grant button or checkmark
- Uses `LifecycleEventObserver` to re-check all permissions on `ON_RESUME`
- Uses `ShotsTheme`

---

## Components

### ShotsCard (`ShotsCard.kt`)

```kotlin
@Composable
fun ShotsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
)
```

- 16dp rounded corners
- Surface background color
- 16dp padding
- 0dp elevation (flat design)

### SegmentedControl (`SegmentedControl.kt`)

```kotlin
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
)
```

- 12dp rounded container
- Surface hover background
- 4dp internal padding
- Selected: primary background, on-primary text
- Unselected: surface background, secondary text

---

## Data Layer

### Entity: Screenshot

```kotlin
@Entity(tableName = "screenshots")
data class Screenshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val timestamp: String,
    val status: String // "kept", "deleted", "pending"
)
```

### DAO: ScreenshotDao

```kotlin
@Dao
interface ScreenshotDao {
    @Query("SELECT * FROM screenshots ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Screenshot>>

    @Query("SELECT * FROM screenshots ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<Screenshot>

    @Query("SELECT * FROM screenshots WHERE status = 'pending' ORDER BY timestamp DESC")
    fun getPendingDeletion(): Flow<List<Screenshot>>

    @Query("SELECT * FROM screenshots WHERE status = 'pending' ORDER BY timestamp DESC")
    suspend fun getPendingDeletionOnce(): List<Screenshot>

    @Insert
    suspend fun insert(screenshot: Screenshot): Long

    @Update
    suspend fun update(screenshot: Screenshot)

    @Query("UPDATE screenshots SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Delete
    suspend fun delete(screenshot: Screenshot)
}
```

### Database: ScreenshotDatabase

- Room database, version 1
- Singleton pattern with `getInstance(context)`
- Entity: `Screenshot::class`

---

## Services & Workers

### ScreenshotDetectionService (Foreground Service)

- Runs as foreground service with notification
- Uses `ContentObserver` to detect new screenshots
- On detection: launches `ScreenshotOverlayActivity`
- Requires `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PROJECTION` permissions

### AutoDeleteWorker (WorkManager)

- Periodic work (every 15 minutes)
- Checks for pending deletions
- Deletes files and updates database status

### BootReceiver

- Listens for `BOOT_COMPLETED`
- Restarts `ScreenshotDetectionService` on device boot

---

## Permissions

| Permission | Required | Purpose |
|---|---|---|
| `READ_EXTERNAL_STORAGE` / `READ_MEDIA_IMAGES` | Yes | Detect screenshots |
| `SYSTEM_ALERT_WINDOW` | Yes | Show overlay popup |
| `POST_NOTIFICATIONS` | Yes (API 33+) | Deletion warnings |
| `FOREGROUND_SERVICE` | Yes | Keep service alive |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | Yes (API 34+) | Media projection |
| `RECEIVE_BOOT_COMPLETED` | Yes | Restart on boot |

---

## Manifest

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <application
        android:name=".ScreenshotGuardApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="Shots"
        android:supportsRtl="true"
        android:theme="@style/Theme.ScreenshotGuard">

        <activity android:name=".OnboardingActivity"
            android:theme="@style/Theme.ScreenshotGuard" />

        <activity android:name=".MainActivity" />
        <activity android:name=".SettingsActivity" />
        <activity android:name=".HistoryActivity" />
        <activity android:name=".OnboardingActivity" />
        <activity android:name=".PermissionsActivity" />
        <activity android:name=".ScreenshotOverlayActivity"
            android:theme="@style/Theme.ScreenshotGuard.Overlay" />

        <service android:name=".service.ScreenshotDetectionService"
            android:foregroundServiceType="mediaProjection"
            android:exported="false" />

        <receiver android:name=".receiver.BootReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>

        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

    </application>
</manifest>
```

---

## Dependencies (build.gradle.kts)

```kotlin
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

---

## Plugins (build.gradle.kts - root)

```kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
}
```

---

## Gradle Properties

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```

---

## App Icon

- Source file: `/storage/emulated/0/Shots/icon.png`
- Copied to all `mipmap-*` directories as `ic_launcher.png`
- **Note**: `shots.png` does not exist, use `icon.png`

---

## XML Themes (res/values/themes.xml)

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.ScreenshotGuard" parent="android:Theme.Material.Light.NoActionBar" />
    <style name="Theme.ScreenshotGuard.Overlay" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:windowBackground">@android:color/transparent</item>
        <item name="android:windowIsTranslucent">true</item>
        <item name="android:windowNoTitle">true</item>
    </style>
</resources>
```

---

## Build Commands

```bash
# Set environment
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk
export ANDROID_HOME=/data/data/com.termux/files/home/android-sdk
export GRADLE_USER_HOME=/data/data/com.termux/files/home/.gradle

# Use gradle binary directly (gradlew has permission issues on external storage)
GRADLE_BIN=/data/data/com.termux/files/home/.gradle/wrapper/dists/gradle-8.11.1-bin/bpt9gzteqjrbo1mjrsomdt32c/gradle-8.11.1/bin/gradle

# Build
$GRADLE_BIN -p /storage/emulated/0/Shots --no-daemon assembleDebug

# APK output location
/storage/emulated/0/Shots/app/build/outputs/apk/debug/app-debug.apk
```

---

## Build Issues & Solutions

### Problem: Gradle daemon hangs indefinitely

**Symptoms**: Build starts, transforms jars, then hangs for 10+ minutes with no output.

**Root cause**: Multiple Gradle daemon instances conflict, stale lock files, or daemon crashes with "Broken pipe" KryoException.

**Solutions tried**:
1. `--no-daemon` flag — Still hangs (single-use daemon forked)
2. Killing daemon processes — No processes running
3. Cleaning daemon lock files — Doesn't resolve

**Actual fix**: The build DOES work, but takes 10-15 minutes on Termux ARM. Need to wait longer or use `--no-daemon` and be patient.

### Problem: gradlew permission denied on external storage

**Cause**: Android external storage doesn't support Unix permissions.

**Solution**: Use gradle binary directly from `~/.gradle/wrapper/dists/` instead of `./gradlew`.

### Problem: compileSdk 37 + Composables UI

**Cause**: Composables UI library requires compileSdk 37, but AGP 8.7.3 doesn't officially support it.

**Solution**: Use `android.suppressUnsupportedCompileSdk=37` in gradle.properties, or stay on compileSdk 35 with Material3.

---

## Skills Installed

| Skill | Location |
|---|---|
| composables-cli@0.10.1 | `~/.agents/skills/` |
| karpathy-guidelines | `~/.agents/skills/` |
| ui-ux-pro-max | `~/.agents/skills/` |
| krehel-better-interface | `~/.agents/skills/` |
| appllama-app-design-skill | `~/.agents/skills/appllama-app-design-skill/SKILL.md` |
| appllama-usage | `~/.agents/skills/appllama-usage/SKILL.md` |
| customize-opencode | Built-in |

---

## Karpathy Guidelines

1. **Think Before Coding** — Understand the problem fully before writing code
2. **Simplicity First** — Start with the simplest solution
3. **Surgical Changes** — Make minimal, targeted changes
4. **Goal-Driven Execution** — Know what you're building before you build it

---

## Todo Status

### Completed
- [x] Phase 1-9: Full Kotlin app (screenshot detection, overlay popup, Room DB, WorkManager, Settings, History, MainScreen, foreground service, boot receiver)
- [x] React Native rewrite abandoned (NDK x86-64 incompatible with ARM Termux)
- [x] Skills installed (composables-cli, karpathy, ui-ux-pro-max, krehel-better-interface)
- [x] Theme architecture fix (all Activities use ShotsTheme)
- [x] Mono Ink Minimal palette implemented
- [x] Type.kt rewritten (Material3 Typography)
- [x] All Activities updated (use ShotsTheme)
- [x] All Screens rewritten (use Material3 components)
- [x] Onboarding redesigned (4-page flow with Jakub principles)
- [x] Permission bug fixed (LifecycleEventObserver in PermissionManagerScreen and OnboardingScreen)
- [x] Components rewritten (ShotsCard, SegmentedControl)
- [x] App icon updated (icon.png to all mipmap dirs)
- [x] Material3 XML removed from themes.xml (uses android platform themes)
- [x] Version bumped to 2.0 (versionCode 3)

### In Progress
- [ ] Successful build (hanging on Termux ARM)

### Pending
- [ ] Test all screens work
- [ ] Test permission bug is fixed
- [ ] Test new onboarding flow
- [ ] Update README for v2.0
- [ ] Commit all changes and push to GitHub

---

## File Reference

| File | Path |
|---|---|
| Project Root | `/storage/emulated/0/Shots/` |
| build.gradle.kts (app) | `/storage/emulated/0/Shots/app/build.gradle.kts` |
| build.gradle.kts (root) | `/storage/emulated/0/Shots/build.gradle.kts` |
| gradle-wrapper.properties | `/storage/emulated/0/Shots/gradle/wrapper/gradle-wrapper.properties` |
| gradle.properties | `/storage/emulated/0/Shots/gradle.properties` |
| SettingsTheme.kt | `/storage/emulated/0/Shots/app/src/main/java/com/screenshotguard/ui/theme/ShotsTheme.kt` |
| Type.kt | `/storage/emulated/0/Shots/app/src/main/java/com/screenshotguard/ui/theme/Type.kt` |
| OnboardingScreen.kt | `/storage/emulated/0/Shots/app/src/main/java/com/screenshotguard/ui/onboarding/OnboardingScreen.kt` |
| PermissionManagerScreen.kt | `/storage/emulated/0/Shots/app/src/main/java/com/screenshotguard/ui/permissions/PermissionManagerScreen.kt` |
| MainScreen.kt | `/storage/emulated/0/Shots/app/src/main/java/com/screenshotguard/ui/main/MainScreen.kt` |
| OverlayScreen.kt | `/storage/emulated/0/Shots/app/src/main/java/com/screenshotguard/ui/overlay/OverlayScreen.kt` |
| SettingsScreen.kt | `/storage/emulated/0/Shots/app/src/main/java/com/screenshotguard/ui/settings/SettingsScreen.kt` |
| HistoryScreen.kt | `/storage/emulated/0/Shots/app/src/main/java/com/screenshotguard/ui/history/HistoryScreen.kt` |
| ShotsCard.kt | `/storage/emulated/0/Shots/app/src/main/java/com/screenshotguard/ui/components/ShotsCard.kt` |
| SegmentedControl.kt | `/storage/emulated/0/Shots/app/src/main/java/com/screenshotguard/ui/components/SegmentedControl.kt` |
| themes.xml | `/storage/emulated/0/Shots/app/src/main/res/values/themes.xml` |
| icon.png | `/storage/emulated/0/Shots/icon.png` |
| APK Output | `/storage/emulated/0/Shots/app/build/outputs/apk/debug/app-debug.apk` |
| Old APK (working) | `/storage/emulated/0/Shots/app/build/outputs/apk/debug/app-debug.apk` (Sep 12 12:37, 22MB) |
