# Fitness Journey for Samsung

A native Android fitness tracker built with Kotlin and Jetpack Compose. The app stores weigh-ins locally on the device and includes a compact 1×2 home-screen widget with a circular progress ring.

## Default journey values

Fresh installs, and installs that still have the old untouched seeds, start from:

- Starting weight: **101.0 kg**
- Current weight: **96.6 kg**
- Target weight: **75.0 kg**
- Weight lost: **4.4 kg**
- Remaining: **21.6 kg**
- Progress: **16.9%**

Progress is `(start − current) / (start − target)`, with a guarded denominator, clamped to 0–100%. With these weights that is `4.4 / 26 = 16.9%`.

These are starter values stored on the phone. They are not a live connection to Samsung Health. The target is editable under the settings icon.

### Existing installs

SharedPreferences are not wiped. A one-time migration (`defaults_migrated_v2`) rewrites a weight only when that key is already stored and still equals the old seed (`101.4`, `94.55`, or `76`, within 0.02 kg). Keys that were never saved pick up the new defaults the next time they are read. Weigh-in history is never modified. Once any weigh-in has been saved, the current-weight value is left as logged, even if it is still 94.55 kg — log today's weight to move it.

## Features

- Dark dashboard with current weight, weight lost, remaining weight and goal progress
- Editable starting and target weights
- Manual weigh-in logging with input validation
- One entry per calendar day; a new entry for the same day replaces that day's entry
- Local weigh-in history stored with Android SharedPreferences
- Trend chart for up to the latest 12 saved weigh-ins
- CSV export of weigh-in history using Android's document picker
- Home-screen progress-ring widget (1 row × 2 columns) that refreshes when a weigh-in or goal is saved
- GitHub Actions workflow that builds a debug APK and uploads it as a workflow artifact

## Widget

The widget is a dark stadium-shaped card. Its accent green is `#61C70B`, sampled from the Samsung Health activity ring. A circular ring on the left shows progress, with the percentage in the centre. The right side shows the current weight, kilograms lost, kilograms still to go, and the target. The ring is drawn as a bitmap so the widget stays on the standard RemoteViews path. Tapping the card opens the app.

It requests a 2×1 cell (`targetCellWidth` 2, `targetCellHeight` 1), with `minWidth` 110dp and `minHeight` 40dp so older sizing math also lands on two columns by one row.

## Build locally

Open this folder in Android Studio (recent stable version), allow Gradle sync, then choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**. The project uses Android Gradle Plugin 8.7.3, Gradle 8.9, Java 17, compile SDK 35 and min SDK 26. Version 1.1.2. Debug APKs are signed with the keystore committed at `app/debug.keystore`, so a later build can install over the previous one. The v0.2.0 and v0.2.1 preview APKs were signed by temporary CI keys and cannot be updated in place.

## Build through GitHub Actions

Open a pull request into `main`, push to `main`, or run **Android APK** from the Actions tab using **Run workflow**. When the workflow succeeds, open its run and download the `fitness-journey-debug-apk` artifact.

## Install the widget

Long-press the Samsung home screen, choose **Widgets > Fitness Journey > Fitness Progress**. It is meant for the 1-row × 2-column slot. If One UI snaps it to a different span, long-press the placed widget and drag it back to two columns by one row. Log a weigh-in or save goals in the app and the figures update.

## Data and limitations

- Weigh-ins and goals are stored locally on the device. There is no account, cloud sync or automatic backup service in this version. History can be copied out from the History tab as CSV. The app does not import that file back in.
- The app does not read Samsung Health data automatically. Add weigh-ins manually.
- A scheduled Sunday notification reminder is not included.
