# Fitness Journey for Samsung

A native Android fitness tracker built with Kotlin and Jetpack Compose. The app stores weigh-ins locally on the device and includes a compact Android home-screen widget.

## Default journey values

- Starting weight: **101.4 kg**
- Latest seeded weight: **94.55 kg**
- Target weight: **76 kg**
- Weight lost at the seeded values: **6.85 kg**
- Remaining at the seeded values: **18.55 kg**
- Progress at the seeded values: **26.8%**

The target is editable in the app under the settings icon. These defaults are starter values, not a live connection to Samsung Health.

## Features

- Dark dashboard with current weight, weight lost, remaining weight and goal progress
- Editable starting and target weights
- Manual weigh-in logging with input validation
- One entry per calendar day; a new entry for the same day replaces that day's entry
- Local weigh-in history stored with Android SharedPreferences
- Trend chart for up to the latest 12 saved weigh-ins
- CSV export of weigh-in history using Android's document picker
- Android home-screen widget that refreshes when a weigh-in or goal is saved
- GitHub Actions workflow that builds a debug APK and uploads it as a workflow artifact

## Build locally

Open this folder in Android Studio (recent stable version), allow Gradle sync, then choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**. The project uses Android Gradle Plugin 8.7.3, Gradle 8.9, Java 17, compile SDK 35 and min SDK 26.

## Build through GitHub Actions

Push the project to the `main` branch or run **Android APK** from the Actions tab using **Run workflow**. When the workflow succeeds, open its run and download the `fitness-journey-debug-apk` artifact.

## Install the widget

Long-press the Samsung home screen, choose **Widgets > Fitness Journey > Fitness Progress**, then resize it to 4 columns by 1 row if One UI offers that size.

## Data and limitations

- Weigh-ins and goals are stored locally on the device. There is no account, cloud sync or automatic backup service in this version.
- The app does not read Samsung Health data automatically. Add weigh-ins manually.
- A scheduled Sunday notification reminder is not included in this upgrade yet.
