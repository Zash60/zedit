# Zedit - Android Video Editor

A native Android video editor built with Kotlin, Jetpack Compose, and Media3.

## Features

- **Multi-track timeline**: Video and audio tracks with visual clip rendering
- **Clip operations**: Trim, Split, Merge, Speed control (0.25x - 4.0x)
- **Playback preview**: Real-time timeline playback via ExoPlayer
- **Export**: H.264 MP4 export via Media3 Transformer
- **Project management**: Create, save, load, delete projects via Room database
- **Undo/Redo**: 50 levels of undo for all timeline operations

## Tech Stack

| Component | Library |
|-----------|---------|
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + StateFlow |
| DI | Hilt |
| Database | Room |
| Video | Media3 (ExoPlayer + Transformer) |
| Build | Gradle with version catalog |

## Requirements

- Android 9.0+ (API 28)
- OpenGL ES 3.0
- Device with video encoding support

## Build

```bash
./gradlew assembleDebug
```

APK location: `app/build/outputs/apk/debug/app-debug.apk`

## Download

Latest APK available as a GitHub Actions artifact:
1. Go to [Actions tab](https://github.com/Zash60/zedit/actions)
2. Select the latest successful workflow run
3. Download the `app-debug.apk` artifact
4. Install on your device: `adb install app-debug.apk`

## Architecture

```
com.zedit/
├── data/
│   ├── db/           # Room database, DAOs, relations
│   ├── media/        # SAF media picker, MediaStore saver
│   ├── model/        # Room entities (Project, Track, Clip)
│   ├── repository/   # ProjectRepository
│   └── settings/     # DataStore preferences
├── di/               # Hilt modules
├── engine/           # TimelinePlayer, ExportEngine
├── permissions/      # Runtime permission handling
└── ui/
    ├── editor/       # EditorScreen, ExportDialog, toolbar
    │   └── timeline/ # TimelineCanvas, ViewModel, state models
    ├── navigation/   # Screen routes
    ├── projects/     # Project list screen
    └── theme/        # Material 3 dark theme
```

## License

MIT
