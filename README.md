# Expense Tracker

A native Android app that builds your expense history automatically by reading bank and payment SMS messages on your device. Everything runs offline, and all data is stored encrypted on the phone.

Built with Kotlin, Jetpack Compose and Material 3.

## Features

**Automatic SMS tracking**
- On first launch, pick a start month (January 2025 onward) and the app parses all transaction SMS from that date.
- New SMS are picked up in real time by a broadcast receiver, or on demand with the **Update** button on the Overview screen.
- A rule-based classifier (`SMSClassifier`) scores each message on amount patterns, banking keywords and transaction verbs, then extracts the amount, merchant name and debit/credit type.

**Overview**
- Donut chart of this month's spending against the monthly budget.
- Expenses, Budget and Safe-to-Spend figures (remaining budget per day left in the month).
- Top categories and top transactions for the month.
- Tap the budget to set it (default ₹2,00,000).

**Transactions**
- Full transaction list with search, filters by category and by month, and sorting.
- Edit a transaction: rename it (up to 40 characters), change its category, or mark it as expense or non-expense.
- Add transactions manually from the **Add Transaction** tab.
- Delete transactions.

**Categories**
- 13 built-in categories, each with a Material icon: Bills, Grocery, Dining, Entertainment, Shopping, Health, Investment, Transfer, Transport, Utilities, Education, Personal Care and Uncategorized.
- Category suggestions for new transactions, based on how you've categorized similar merchants before.

**Analytics**
- Month-by-month view with a month selector.
- At-a-glance expenses vs. budget, a bar chart and a per-category breakdown.

**Profile**
- Change your profile name.
- Delete all data and reset the app to first-run setup.

Amounts are shown in Indian Rupees without decimals, and dates use Indian Standard Time. The theme is blue and follows the system light or dark setting.

## Privacy and storage

- SMS are read and processed only on the device. The app makes no network requests.
- Transactions, budgets and user settings are saved as JSON, encrypted with AES-256 (CBC/PKCS5Padding) in the app's private storage (`EncryptedStorage`).
- The encryption key is generated on first run and stored in the app's private files directory. It is not stored in the Android Keystore.

## Requirements

| | |
|---|---|
| Android | 8.0 (API 26) or later |
| Target SDK | 35 (compiles against SDK 36) |
| Permissions | `READ_SMS`, `RECEIVE_SMS` |
| Build tools | Android Studio (recent), JDK 17, Gradle 8.13 (wrapper), AGP 8.11, Kotlin 2.2 |

## Getting started

1. Clone the repository and open the folder in Android Studio.
2. Let Gradle sync. Android Studio updates `local.properties` with your SDK path.
3. Connect a device or start an emulator, then click **Run**.
4. Grant SMS permission, enter your name and choose a start month. The app parses your history and opens the Overview.

> Note: emulators have no real bank SMS, so test on a physical device or send test messages through the emulator's extended controls.

### Command line

```bash
./gradlew assembleDebug     # build a debug APK
./gradlew installDebug      # install on a connected device
./gradlew test              # run unit tests
```

Release builds have R8 minification enabled (`app/proguard-rules.pro`).

## Project structure

```
app/src/main/java/com/expensetracker/
├── MainActivity.kt
├── data/
│   ├── ml/SMSClassifier.kt          # transaction detection, parsing, category suggestions
│   ├── models/                      # Transaction, Category, Budget, User
│   ├── repository/ExpenseRepository.kt   # single source of truth, exposes StateFlows
│   └── storage/EncryptedStorage.kt  # AES-encrypted JSON files
├── navigation/
│   ├── ExpenseTrackerNavHost.kt     # setup → main, profile
│   └── DashboardContainer.kt        # bottom-nav host: Overview, Add, Analytics, Transactions
├── receiver/SMSReceiver.kt          # real-time SMS_RECEIVED handling
├── ui/
│   ├── setup/                       # first-run setup (permissions, name, start month)
│   ├── overview/                    # dashboard
│   ├── transactions/                # list, edit sheet, add transaction
│   ├── analytics/                   # monthly trends
│   ├── profile/                     # name, delete all data
│   ├── components/                  # DonutChart, BarChart, DateRangePicker, nav bar, header
│   └── theme/
└── utils/SMSProcessor.kt            # reads the SMS inbox and feeds the classifier
```

Screens use ViewModels where present, and all of them read from `ExpenseRepository`.

## Changelog

See [change log for expense tracker project.txt](change%20log%20for%20expense%20tracker%20project.txt) for release notes. Version 1.0 was released on 29 Aug 2025. Recent changes:

- Removed the login, category and budget screens. The budget is now set from the Overview.
- Renamed the Historical screen to Analytics and improved it.
- Added manual transaction entry and per-transaction icons.
