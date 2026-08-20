# FinanceSimply (SpendSense)

FinanceSimply is a privacy-first, on-device Android application designed to track and manage your expenses intelligently without compromising your financial data.

## Features

- **Local First & Privacy Focused**: All financial data stays strictly on your device. SpendSense does not use a backend server or cloud sync, ensuring zero data leakage.
- **Premium Dynamic Dark Mode**: Features a highly legible, beautifully designed deep charcoal surface with indigo primary accents and semantic color coding for budgets.
- **Split with Friends**: Easily split expenses and capture precise "budget impacts" so you only track what you actually spent. The app automatically calculates "Owed to You" from pending splits.
- **Smart Context Capture**: Whenever an expense occurs, an intuitive bottom sheet allows you to effortlessly classify the transaction, add notes, and define splits.
- **Customizable Monthly Budgeting**: Set your monthly budgets and visually track how much is remaining.
- **Addiction & Pattern Tracking**: Monitor specific categories or merchants to track spending addictions.
- **No Manual Entry Required (Upcoming)**: Import historical expenses securely.

## Tech Stack

- **Platform**: Android
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Local Database**: Room Database (SQLite)
- **JSON Serialization**: Gson

## Technical Architecture & Codebase Structure

The application strictly follows the **MVVM (Model-View-ViewModel)** architectural pattern. Since the app is local-first, the architecture is designed around Jetpack Room as the single source of truth, heavily leveraging Kotlin Coroutines and `StateFlow` for reactive UI updates.

### 1. Data Layer (`com.example.financesimply.data`)
- **`SpendSenseDatabase`**: The central Room database instance. Currently on Schema Version 3. Destructive migrations are enabled during active development.
- **`TransactionEntity`**: The core data model. It tracks the raw merchant name, amount, category, tags, and specific variables like `budgetImpactAmount` (which differs from `amount` if a bill was split).
- **`SplitParticipant`**: A data class detailing split bills (e.g., who owes what). It is stored alongside transactions using **`Converters.kt`** (Gson serialization).
- **`TransactionDao`**: Handles all SQL operations. It uses Flow-based queries for real-time UI updates (e.g., automatically summing up total expenses vs. budgets).

### 2. UI Layer (`com.example.financesimply.ui`)
- **Jetpack Compose**: The entire app is written in Jetpack Compose, emphasizing a Premium Dark Mode aesthetic (`Color.kt`, `Theme.kt`).
- **`SpendSenseApp.kt`**: The root Compose navigation host. It also hosts the global `ContextCaptureSheet` overlay.

#### Screens (`ui.screen`)
- **`DashboardScreen`**: The main hub. It renders budget progression bars, the "Owed to You" summary, and quick stats.
- **`TransactionsScreen`**: A chronologically ordered feed of transactions. It utilizes `FilterChips` to let users quickly pivot between "All", "Untagged", and "Splits".
- **`SettingsScreen`**: Manages user budgets, favorite outlets (Regex mappings), monitored packages, and addiction trackers.

#### Components (`ui.component`)
- **`ContextCaptureSheet`**: The dynamic bottom sheet that appears when a transaction is detected (or tapped). It allows users to categorize, tag, and execute "Split Bill" mathematics instantly.
- **`TransactionCard`**: The reusable Compose element for rendering individual transactions. It has built-in logic to visually differentiate Split transactions and standard transactions.

### 3. State Management (`ui.viewmodel`)
- **`DashboardViewModel`**: Computes macro-level math. Queries the DB to determine total amount spent in a month, remaining budget, and pending split money owed to the user.
- **`ContextCaptureViewModel`**: Handles the volatile state of tagging a transaction. It encapsulates the math for automatically dividing split bills evenly among participants before committing to the DB.
- **`TransactionListViewModel`**: Exposes the stream of all transactions to the `TransactionsScreen` and manages active view filters.

### 4. Background Services (`worker`)
- **`UntaggedReminderWorker`**: Uses Android's WorkManager to check the local database for pending/untagged transactions. If you ignore notifications, this worker will politely remind you to tag them to keep your budget accurate.

## Getting Started

Since FinanceSimply requires specific Android permissions to intercept transaction notifications and read SMS (upcoming feature), this application is designed to be sideloaded.

1. Clone this repository.
2. Open the project in Android Studio.
3. Run the app on your physical device or emulator.
4. Grant the necessary **Notification Access** permission when prompted to enable automatic transaction capture.

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.
