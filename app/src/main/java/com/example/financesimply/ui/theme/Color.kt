package com.example.financesimply.ui.theme

import androidx.compose.ui.graphics.Color

// ── Pure B&W Palette ──
val PureBlack = Color(0xFF000000)
val SurfaceDark = Color(0xFF111111)       // Near-black for cards
val SurfaceVariantDark = Color(0xFF1A1A1A) // Slightly lighter for nested elements
val PureWhite = Color(0xFFFFFFFF)
val TextGray = Color(0xFF999999)           // Secondary text
val DividerGray = Color(0xFF333333)        // Borders / dividers

// Primary accent — white in dark mode, black in light mode (defined in Theme.kt)
val PrimaryAccent = Color(0xFFFFFFFF)
val PrimaryAccentVariant = Color(0xFFCCCCCC)

// Chart colors (grayscale gradient)
val ChartColors = listOf(
    Color(0xFFFFFFFF), // White
    Color(0xFFBBBBBB), // Light gray
    Color(0xFF888888), // Mid gray
    Color(0xFF555555), // Dark gray
)

// Budget progress colors (semantic — kept for functional meaning)
val BudgetSafe = Color(0xFF10B981)    // Green — under budget
val BudgetWarning = Color(0xFFF59E0B) // Amber — approaching limit
val BudgetDanger = Color(0xFFEF4444)  // Red — near/at limit
val BudgetOver = Color(0xFFB91C1C)    // Deep red — over budget

// Credit/Debit functional colors
val CreditGreen = Color(0xFF10B981)
val DebitWhite = Color(0xFFFFFFFF)
