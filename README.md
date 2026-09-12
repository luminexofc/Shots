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
- **Delete After** a custom delay (5 min to 48 hours)
- **Skip** and decide later

No more cluttered gallery. No more accidentally deleting important screenshots.

---

## Features

### Core
- **Real-time Detection** - Uses MediaStore ContentObserver to detect screenshots instantly
- **Smart Overlay** - Bottom sheet popup appears immediately after screenshot
- **Auto-Delete** - Schedule deletions from 5 minutes to 48 hours
- **Keep/Skip/Delete** - Full control over every screenshot

### Screens
- **Splash** - Animated entrance with scale and fade effects
- **Onboarding** - 5-page guided setup (Welcome, Permissions, How It Works)
- **Main Dashboard** - Stats, recent screenshots, quick actions
- **Overlay** - Glassmorphism bottom sheet with delay picker
- **Settings** - Customize deletion delays, default actions, auto-dismiss
- **History** - View all screenshots with batch actions
- **Permissions** - Clear permission management with status indicators

### Design
- **Midnight Indigo + Coral Accent** - Modern dark-first color palette
- **Jetpack Compose Material 3** - Latest Material Design components
- **Smooth Animations** - Staggered entries, scale-on-press, fade transitions
- **Dark Mode** - Full dark theme support with OLED-friendly blacks

---

## Tech Stack

| Category | Library |
|----------|---------|
| **UI** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM + Repository Pattern |
| **Database** | Room (SQLite) |
| **Background** | WorkManager + Foreground Service |
| **Image Loading** | Coil |
| **Language** | Kotlin 2.1.0 |
| **Build** | Gradle 8.10.2 |
| **Min SDK** | 26 (Android 8.0) |
| **Target SDK** | 35 (Android 15) |

---

## Screenshots

<p align="center">
  <em>Splash → Onboarding → Main → Overlay → Settings → History</em>
</p>

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
- Overlay permission (for popup)
- Notification permission (for deletion warnings, optional)

---

## How It Works

1. **Install & Setup** - Grant required permissions during onboarding
2. **Take a Screenshot** - Shots detects it instantly via MediaStore
3. **Popup Appears** - Bottom sheet shows with Keep/Delete/Skip options
4. **Choose Action** - Keep it, set a deletion timer, or skip for later
5. **Auto-Delete** - WorkManager handles scheduled deletions in background

---

## Architecture

```
com.screenshotguard/
├── data/               # Room database, DAOs, Entity, Preferences
├── detection/          # Screenshot detection (ContentObserver)
├── receiver/           # Boot receiver for service restart
├── service/            # Foreground service for detection
├── ui/
│   ├── components/     # Reusable UI components (ShotsCard, SegmentedControl)
│   ├── history/        # History screen
│   ├── main/           # Main dashboard
│   ├── onboarding/     # Onboarding flow
│   ├── overlay/        # Overlay popup
│   ├── permissions/    # Permission manager
│   ├── settings/       # Settings screen
│   ├── splash/         # Splash screen
│   └── theme/          # Theme system (ShotsTheme, colors, typography)
└── worker/             # WorkManager for auto-delete
```

---

## Color Palette

### Midnight Indigo (Primary)
- `#4F46E5` - Light mode primary
- `#6366F1` - Dark mode primary

### Coral Accent
- `#EA580C` - Light mode accent
- `#F97316` - Dark mode accent

### Status Colors
- **Success**: `#16A34A` / `#22C55E`
- **Warning**: `#CA8A04` / `#EAB308`
- **Destructive**: `#DC2626` / `#EF4444`

---

## Permissions

| Permission | Required | Purpose |
|------------|----------|---------|
| `READ_MEDIA_IMAGES` | Yes | Access screenshots in gallery |
| `SYSTEM_ALERT_WINDOW` | Yes | Show overlay popup |
| `POST_NOTIFICATIONS` | Optional | Deletion warnings |
| `FOREGROUND_SERVICE` | Yes | Background detection |
| `RECEIVE_BOOT_COMPLETED` | Yes | Restart service after reboot |

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

---

## Acknowledgments

- Built entirely in [Termux](https://termux.dev/) on Android
- UI redesigned with design intelligence skills (Karpathy, UI UX Pro Max, Krehel)
- Color palette inspired by modern dark-first design principles

---

<p align="center">
  Made with Kotlin + Jetpack Compose
</p>
