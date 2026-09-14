# Shots

**Your screenshots, your rules.** Keep what matters, auto-delete the rest.

<p align="center">
  <img src="icon.png" width="120" alt="Shots Icon">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-26%2B-brightgreen" alt="Min SDK">
  <img src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF" alt="Kotlin">
  <img src="https://img.shields.io/badge/Compose-M3-4285F4" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/License-MIT-blue" alt="License">
</p>

---

## What is Shots?

Shots is a screenshot management app for Android. When you take a screenshot, a popup appears instantly letting you:

- **Keep** the screenshot
- **Delete After** a custom delay (1 to 60 minutes)
- **Skip** and decide later

No more cluttered gallery. No more accidentally deleting important screenshots.

---

## Features

- **Real-time Detection** - Uses MediaStore ContentObserver to detect screenshots instantly (skips pending/trash staging entries)
- **Smart Overlay** - Popup appears immediately after the screenshot is saved
- **Verified Deletion** - Files are only marked deleted after the app confirms they're actually gone from storage and MediaStore
- **System Dialog Fallback** - When direct deletion is blocked by Android, the official system confirmation dialog is used
- **Exact-Alarm Timers** - Deletion timers fire at the precise minute, even in Doze mode with the screen off; rescheduled after reboot
- **History** - View all screenshots with real filenames, thumbnails, and status filters

---

## Tech Stack

| Category | Library |
|----------|---------|
| **UI** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM + Repository Pattern |
| **Database** | Room (SQLite) |
| **Background** | WorkManager + Exact Alarms + Foreground Service |
| **Image Loading** | Coil |
| **Language** | Kotlin 2.1.0 |
| **Build** | Gradle 8.10.2 |
| **Min SDK** | 26 (Android 8.0) |
| **Target SDK** | 35 (Android 15) |

---

## Installation

### Download APK

Download the latest APK from the [Releases](https://github.com/EchoBolt-07/Shots/releases) page.

### Build from Source

```bash
# Clone the repository
git clone https://github.com/EchoBolt-07/Shots.git
cd Shots

# Build debug APK
./gradlew assembleDebug

# APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

### Requirements

- Android 8.0 (API 26) or higher
- Storage permission (to access screenshots)
- All Files Access (to delete screenshots directly)
- Overlay permission (for popup)
- Notification permission (for deletion confirmations, optional)

---

## How It Works

1. **Install & Setup** - Grant required permissions during onboarding
2. **Take a Screenshot** - Shots detects it instantly via MediaStore
3. **Popup Appears** - Popup shows with Keep/Delete/Set Timer/Skip options
4. **Choose Action** - Keep it, set a deletion timer, or skip for later
5. **Auto-Delete** - Exact alarms handle scheduled deletions in the background

---

## Architecture

```
com.shots/
├── data/               # Room database, DAOs, Entity, Preferences
├── receiver/            # Boot receiver for alarm rescheduling
├── service/             # Foreground service for detection
├── ui/
│   ├── components/     # Reusable UI components (ShotsCard, SegmentedControl)
│   ├── history/        # History screen
│   ├── main/           # Main dashboard
│   ├── onboarding/     # Onboarding flow
│   ├── overlay/        # Overlay popup
│   ├── permissions/    # Permission manager
│   ├── settings/       # Settings screen
│   └── theme/          # Theme system (ShotsTheme, colors, typography)
├── util/               # MediaStore helpers, alarm scheduler, suppressor
└── worker/             # WorkManager safety net for auto-delete
```

---

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
