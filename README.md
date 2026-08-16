# Minimal Launcher

Minimal Launcher is a native Android home-screen replacement built with Kotlin and Jetpack Compose. It keeps the first screen deliberately quiet: a large clock, optional date, and a small list of favorite apps. Swipe up, or tap **All apps**, for an alphabetically sorted, searchable application drawer.

The launcher is entirely offline. It has no analytics, advertisements, news feed, network requirement, or broad package-query permission.

## Features

- Can be selected as the device's default HOME launcher.
- Large clock with 12/24-hour and date-visibility settings.
- Persisted favorite applications backed by DataStore.
- App drawer with icons, case-insensitive live search, and alphabetical ordering.
- Automatic drawer refresh while the launcher is visible when packages are installed, removed, or updated.
- Light, dark, and system theme choices.
- Accessibility-friendly touch targets and screen-reader labels.

## Architecture

The project intentionally uses a small, direct structure:

```text
app/src/main/java/com/example/minimallauncher/
├── data/
│   ├── apps/             PackageManager discovery and explicit app launching
│   └── preferences/      DataStore settings and favorites
├── domain/               Android-free app models, sorting, search, and favorite logic
├── platform/             Package-change observer
├── ui/
│   ├── home/             Clock and favorites home screen
│   ├── apps/             App drawer and search
│   ├── settings/         Preferences and favorite management
│   ├── theme/            Minimal Material 3 color schemes
│   └── LauncherViewModel.kt
└── MainActivity.kt       Activity lifecycle and default-launcher settings intent
```

`LauncherViewModel` owns observable UI state. It combines the package-manager result with the DataStore preference flow. Package queries run off the main thread and are only refreshed at launch or after a package-change broadcast; Compose does not query `PackageManager` during recomposition.

## Build

Prerequisites:

- Android Studio with Android SDK Platform 37 installed.
- JDK 17.

Build a debug APK:

```bash
./gradlew assembleDebug
```

If Homebrew's `JAVA_HOME` points at the formula directory rather than the macOS JDK bundle, use:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew assembleDebug
```

The resulting APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Install and make it the default launcher

1. Enable Developer options and USB debugging on a physical Android device.
2. Install the debug APK:

   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

3. Press the Home gesture/button and select **Minimal Launcher**. Choose **Always** when Android offers the choice.
4. To change it later, open **Settings** in Minimal Launcher and tap **Default launcher settings**, or use the device's Apps / Default apps settings.

## Test

Run local unit tests:

```bash
./gradlew testDebugUnitTest
```

Run instrumentation and Compose UI tests on a connected device or emulator:

```bash
./gradlew connectedDebugAndroidTest
```

The current tests cover sorting, case-insensitive app filtering, favorite changes, ViewModel updates, DataStore restoration, and search-to-launch behavior in the app drawer.

## Android considerations

- The manifest declares visibility only for activities with `ACTION_MAIN` and `CATEGORY_LAUNCHER`; it does not request `QUERY_ALL_PACKAGES`.
- Android and device-management policy can still hide work-profile, suspended, or administrator-restricted applications from launcher queries.
- The launcher observes package changes only while its activity is running. This avoids a persistent background service; returning Home refreshes the list and changes that occur while visible refresh immediately.
- Becoming the default launcher is always a user-controlled Android system decision. An application cannot silently make itself the default home app.
