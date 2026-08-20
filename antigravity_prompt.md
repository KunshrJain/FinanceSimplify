# Prompt for Antigravity

Build an Android app called **SpendSense** (working name) in **Kotlin + Jetpack Compose**. This is a personal, local-first expense tracker that reads transaction notifications and asks the user for context, rather than just logging raw amounts.

## Core architecture
- MVVM, Room for local persistence (no backend, no cloud sync — all data stays on-device)
- A foreground `NotificationListenerService` that listens ONLY to a whitelisted set of package names (GPay, PhonePe, Paytm, and a configurable list of bank apps)
- WorkManager for periodic budget-threshold checks
- DataStore for user preferences (monthly budget, favorite outlets list, addiction-tracking categories)

## Feature 1: Notification capture + parsing
- Implement `NotificationListenerService` that captures `title` + `text` from whitelisted apps
- Build a per-source-app regex parser (I will supply real sample notification strings for each app — do NOT invent formats, ask me for samples per app before writing each parser)
- Extracted fields: amount, merchant/payee name (if present), transaction type (debit/credit), timestamp
- Deduplicate: some apps fire multiple notifications per transaction (e.g. a "processing" then a "success" notification) — parser should recognize and merge these

## Feature 2: Context capture flow
- When a new transaction is parsed, show a lightweight bottom-sheet/dialog (not a full-screen interruption) asking:
  - "What was this for?" — free text, plus autocomplete from previously used tags/categories
  - "Was this for someone else?" — toggle, with optional name field (e.g. "Dad")
  - Option to mark this transaction as **exempt from budget** (e.g. "Dad's train ticket")
- If the user dismisses without answering, log it as "Untagged" and surface a periodic reminder (via WorkManager) to go back and tag pending transactions

## Feature 3: Favorite outlets
- Let user maintain a named list mapping raw merchant strings to friendly names (e.g. "SWIGGY*BLR0234" → "Swiggy") and a default category
- When a transaction's merchant string matches (fuzzy match) a saved outlet, auto-fill the category/name suggestion in the context capture dialog

## Feature 4: Budget tracking
- User sets a monthly budget amount
- Running total = sum of all non-exempt transactions this calendar month
- Local push notification warning when: 80% of budget reached, 100% reached, and each subsequent 10% over
- Budget screen: progress bar, remaining amount, days left in month, projected overspend based on current daily average

## Feature 5: Pattern / "addiction" tracking
- User can flag specific categories or merchants as "watched" (e.g. food delivery, a specific gambling/betting app if it sends notifications, cigarettes if bought via UPI at the same store repeatedly)
- Compute frequency + amount trend over rolling 7/30 days for watched items
- Surface a soft nudge (not judgmental language) when frequency or spend on a watched item spikes — e.g. "You've ordered from Swiggy 5 times this week, up from your usual 2"

## Feature 6: Dashboard
- Monthly overview: total spent, budget remaining, top categories, top outlets
- Simple charts (bar for category breakdown, line for daily spend trend) using Vico or MPAndroidChart
- Filter by exempt/non-exempt, tagged/untagged, date range

## Non-functional requirements
- All data local only — no analytics SDKs, no network calls except none needed at all
- Notification listener must run reliably in background; account for battery optimization / doze mode gracefully (guide user to whitelist the app if needed, don't silently fail)
- Clean permission-request UX: on first launch, explain WHY notification access is needed before deep-linking to system settings (since it can't be requested via normal dialog)

## What I will provide you
- Real sample notification text (title + body + package name) from GPay, PhonePe, Paytm, and my bank app — ask me for these before building each parser
- My preferred initial budget amount and category list

## What NOT to do
- Don't add any backend, cloud sync, or third-party analytics
- Don't guess notification formats — always ask for real samples first
- Don't make the context-capture dialog block/mandatory — it should be dismissible with a graceful "tag later" path
