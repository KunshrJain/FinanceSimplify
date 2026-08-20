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

## Getting Started

Since FinanceSimply requires specific Android permissions to intercept transaction notifications and read SMS (upcoming feature), this application is designed to be sideloaded.

1. Clone this repository.
2. Open the project in Android Studio.
3. Run the app on your physical device or emulator.
4. Grant the necessary **Notification Access** permission when prompted to enable automatic transaction capture.

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.
