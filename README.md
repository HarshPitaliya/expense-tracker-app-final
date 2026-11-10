# Expense Tracker

A native Android app that builds your expense history automatically by reading bank SMS messages on the device. Built with Kotlin and Jetpack Compose (Material 3). All processing and storage happen on the phone; no data leaves the device.

**Version:** 1.0 (Aug 2025)

## Features

- **SMS-based tracking.** On first launch the app reads past SMS messages for a date range you choose, then keeps listening for new ones. Bank messages are turned into transactions automatically.
- **Rule-based SMS detection.** Messages are scored on known bank senders (HDFC, ICICI, Axis, SBI, Kotak, Pluxee), transaction keywords, amount patterns and banking context. A message counts as a transaction at a score of 0.7 or higher.
- **Auto-categorisation.** Merchants are matched to a category from similar past transactions first, then from keyword lists (for example Swiggy → Dining, Uber → Transport). There are 13 default categories, including *Uncategorized*.
- **Overview dashboard.** Monthly budget vs. spend donut chart, a daily "safe to spend" amount (remaining budget ÷ remaining days in the month), top categories and top transactions, an editable monthly budget (default ₹2,00,000), and an **Update** button that pulls in new SMS.
- **Transactions.** Search, filter by category and month, rename (up to 40 characters), change the category, mark as expense or non-expense, and undo edits back to the original SMS-derived values. You can also add transactions by hand.
- **Analytics.** Month-by-month view with expenses vs. budget, an at-a-glance summary and a category breakdown.
- **Profile.** Edit your name, see the parse date range and the last parsed date, and clear all data.
- **Encrypted local storage.** Data is saved as JSON files encrypted with AES (CBC/PKCS5) in the app's private files directory.
- Material 3 theming that follows the system light/dark setting. On Android 12+ it uses dynamic (wallpaper-based) colours; older versions fall back to the built-in blue palette. Dates are handled in IST.

## Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin 2.2 |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| State | ViewModels + `StateFlow`, Kotlin Coroutines |
| Storage | Gson JSON, AES-encrypted files |
| Permissions | Accompanist Permissions |
| Build | Gradle 8.13, Android Gradle Plugin 8.11.1 |
| SDK | minSdk 26, targetSdk 35, compileSdk 36 |

## Project structure

```
app/src/main/java/com/expensetracker/
├── MainActivity.kt
├── navigation/
│   ├── ExpenseTrackerNavHost.kt   # Top-level routes: setup, profile, main
│   └── DashboardContainer.kt      # Bottom-nav tabs: overview, transactions, historical, add_transaction
├── data/
│   ├── models/                    # Transaction, Category, Budget, User
│   ├── repository/                # ExpenseRepository (singleton, StateFlow-backed)
│   ├── storage/                   # EncryptedStorage (AES file storage)
│   └── ml/                        # SMSClassifier (rule-based detection, parsing, categorisation)
├── receiver/
│   └── SMSReceiver.kt             # Processes incoming SMS in real time
├── utils/
│   └── SMSProcessor.kt            # Reads SMS inbox, historical and incremental parsing
└── ui/
    ├── setup/                     # First-run setup: permission + date range
    ├── overview/                  # Dashboard
    ├── transactions/              # List, edit and add transactions
    ├── analytics/                 # Historical trends
    ├── profile/                   # User profile and data reset
    ├── components/                # Donut/bar charts, date range picker, bottom nav, header
    └── theme/                     # Colours, typography, theme
```

## How it works

1. **Setup.** The user grants `READ_SMS` / `RECEIVE_SMS` and picks a start and end date. `SMSProcessor` reads the inbox for that range and passes each message to `SMSClassifier`.
2. **Classification.** `SMSClassifier` decides whether a message is a transaction, then extracts the amount, the merchant and whether it is a debit, and suggests a category.
3. **Storage.** `ExpenseRepository` skips duplicates, stores transactions through `EncryptedStorage` and exposes them as `StateFlow` to the ViewModels.
4. **Ongoing updates.** `SMSReceiver` handles new SMS while the current date is before the chosen end date. The Overview **Update** button also parses anything received since the last parsed date.

## Getting started

### Prerequisites

- Android Studio (a recent stable release that supports AGP 8.11)
- JDK 17 or newer (the copy bundled with Android Studio works)
- An Android device or emulator on API 26+. Use a real device to test SMS parsing; emulators can receive test SMS through the extended controls.

### Build and run

1. Open the project folder in Android Studio and let Gradle sync. The repo includes a `local.properties` that points to a Windows SDK path; Android Studio rewrites it on sync. For command-line builds, set `sdk.dir` in that file (or `ANDROID_HOME`) to your own SDK location.
2. Select a device and click **Run**. Or, from the command line:

```bash
./gradlew assembleDebug
```

```bash
./gradlew installDebug
```

   Only the Unix `gradlew` script is included. On Windows, build from Android Studio or run `gradle wrapper` once to generate `gradlew.bat`.

3. On first launch, grant SMS permission and choose the date range to import.

## Privacy and security notes

- SMS content is processed only on the device. The app makes no network requests.
- The AES key is currently kept in a plain file (`encryption.key`) in the same private directory as the encrypted data. Moving the key to the Android Keystore would make storage meaningfully more secure.
- `android:allowBackup` is enabled, but the backup and data-extraction rules exclude `encryption.key`, `*.enc` and `*.iv`, so the key and transaction data are not included in cloud backups or device transfers.

## Known limitations

- SMS detection and categorisation use fixed rules and keywords tuned for Indian bank SMS formats. Other banks or formats may not be recognised.
- Budgets are a single monthly total; there are no per-category budgets.
- No automated tests are included yet.
- The repo has no `.gitignore`, so build outputs (`app/build/`, `build/`), `local.properties` and `.DS_Store` files are committed.

## Changelog

See [`change log for expense tracker project.txt`](change%20log%20for%20expense%20tracker%20project.txt) for the development history (19–29 Aug 2025).
