# DataBridge Pro

**Android /data folder manager — No Root, No Shizuku**

DataBridge Pro lets you browse, copy, move, delete, rename, and back up files inside `/Android/data` on Android 11+ using only the Storage Access Framework (SAF) and `MANAGE_EXTERNAL_STORAGE`. No root access or Shizuku is required.

---

## Features

- Browse `/Android/data` for all installed apps
- Copy, move, delete, rename files and folders
- Create new folders
- Batch selection with long-press
- Backup app data as ZIP archives
- Backup history with Room database
- Scheduled auto-backup via WorkManager
- Biometric lock support
- Material You / Dynamic Color theming
- Light / Dark / System theme
- Search files with debounced input
- Storage usage arc chart on home screen
- Sort files by name, size, date, or type

---

## How to Build

### Prerequisites

- **Android Studio** Ladybug (2024.2+) or newer
- **JDK 17**
- **Android SDK 36** (install via SDK Manager)

### Steps

```bash
# Clone the repository
git clone https://github.com/<your-org>/DataBridgePro.git
cd DataBridgePro

# Generate Gradle wrapper (if not present)
gradle wrapper --gradle-version=8.9

# Build debug APK
./gradlew assembleDebug

# Install on a connected device
./gradlew installDebug
```

The debug APK is output to `app/build/outputs/apk/debug/app-debug.apk`.

---

## How to Grant /Android/data Permission

### Step 1: All Files Access (MANAGE_EXTERNAL_STORAGE)

1. Open the app — you'll see the **Permission** screen.
2. Tap **"Grant Permission"** under **Step 1: All Files Access**.
3. The system **Settings → All Files Access** screen opens.
4. Toggle the switch to allow DataBridge Pro to manage all files.
5. Press Back to return to the app.

### Step 2: SAF Folder Access

1. Tap **"Grant Permission"** under **Step 2: Android/data Access**.
2. The system document picker opens, pre-navigated to `Android/data`.
3. Tap **"Use this folder"** at the bottom.
4. Confirm by tapping **"Allow"** in the dialog.
5. The app now has persistent read/write access to `/Android/data`.

> **Note:** On some devices you may need to navigate manually to `Internal Storage → Android → data` in the picker.

---

## Known Limitations on Android 14+

- **FUSE restrictions:** Android 14 (API 34) added stricter FUSE-level restrictions on `/Android/data`. SAF access still works, but performance may be degraded for large directories.
- **Document ID changes:** Some OEMs modify the `DocumentsProvider`, causing the pre-navigation hint to fail. Users may need to manually navigate to `Android/data` in the picker.
- **No cross-profile access:** Work profile apps' data folders are not accessible via this method.
- **Large file operations:** Copying very large files (>2 GB) via SAF may be slower than native file operations due to the ContentResolver bridge.
- **Android 15+ considerations:** Future Android versions may further restrict SAF access to `/Android/data`. Monitor the Android developer blog for updates.

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                     Presentation Layer                    │
│  ┌────────────┐ ┌──────────┐ ┌────────┐ ┌──────────────┐│
│  │ HomeScreen  │ │FilesScreen│ │Backup  │ │SettingsScreen││
│  │ + ViewModel │ │+ViewModel│ │Screen  │ │ + ViewModel  ││
│  └──────┬─────┘ └────┬─────┘ └───┬────┘ └──────┬───────┘│
│         │            │           │              │        │
│  ┌──────┴────────────┴───────────┴──────────────┴──────┐ │
│  │              Navigation (NavHost + BottomBar)        │ │
│  └──────────────────────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────┤
│                      Domain Layer                        │
│  ┌─────────────────┐ ┌──────────────┐ ┌───────────────┐ │
│  │ GetFilesUseCase  │ │SearchFiles   │ │ BackupUseCase │ │
│  └────────┬────────┘ └──────┬───────┘ └───────┬───────┘ │
├───────────┼─────────────────┼─────────────────┼──────────┤
│                       Data Layer                         │
│  ┌────────┴────────┐ ┌──────┴───────┐ ┌───────┴───────┐ │
│  │ FileRepository   │ │BackupRepo    │ │SettingsRepo   │ │
│  │ (DocumentFile)   │ │(Room + DAO)  │ │(DataStore)    │ │
│  └─────────────────┘ └──────────────┘ └───────────────┘ │
├──────────────────────────────────────────────────────────┤
│                    DI Layer (Hilt)                        │
│  ┌──────────────────────────────────────────────────────┐│
│  │ DatabaseModule  ·  RepositoryModule                  ││
│  └──────────────────────────────────────────────────────┘│
├──────────────────────────────────────────────────────────┤
│                   Worker / Services                      │
│  ┌──────────────────┐ ┌────────────────────────────────┐ │
│  │ BackupWorker      │ │ BackupForegroundService        │ │
│  │ (WorkManager)     │ │ (Notification + Progress)      │ │
│  └──────────────────┘ └────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Component      | Library                          |
|----------------|----------------------------------|
| Language       | Kotlin 2.0                       |
| UI             | Jetpack Compose + Material 3     |
| DI             | Hilt (Dagger)                    |
| Database       | Room                             |
| Preferences    | DataStore                        |
| Navigation     | Navigation Compose               |
| Background     | WorkManager                      |
| File Access    | DocumentFile (SAF)               |
| Image Loading  | Coil                             |
| Biometric      | AndroidX Biometric               |
| Architecture   | MVVM + Clean Architecture        |

---

## Project Structure

```
app/src/main/kotlin/com/databridgepro/filemanager/
├── DataBridgeApp.kt              # Application class (@HiltAndroidApp)
├── MainActivity.kt               # Single activity entry point
├── di/
│   └── AppModule.kt              # Hilt modules (Database, Repository)
├── data/
│   ├── model/                    # Data classes (FileItem, BackupItem, etc.)
│   ├── local/                    # Room database + DAOs
│   └── repository/               # Repository implementations
├── domain/
│   └── usecase/                  # Use cases (GetFiles, SearchFiles, Backup)
├── presentation/
│   ├── navigation/               # NavHost, Theme, Screen routes
│   ├── permission/               # Permission grant flow
│   ├── home/                     # Dashboard with storage chart
│   ├── files/                    # File browser with breadcrumbs
│   ├── backup/                   # App backup manager
│   └── settings/                 # App settings
├── util/                         # PermissionUtils, ZipUtils
└── worker/                       # BackupWorker, BackupForegroundService
```

---

## License

This project is provided as-is for educational purposes.
