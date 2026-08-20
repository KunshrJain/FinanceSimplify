package com.example.financesimply.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financesimply.ui.component.BudgetProgressBar
import com.example.financesimply.ui.theme.BudgetDanger
import com.example.financesimply.ui.theme.BudgetSafe
import com.example.financesimply.ui.theme.ChartColors
import com.example.financesimply.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your monthly spending overview",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Income overview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Income",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "₹${formatCompact(state.monthlyIncome)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = com.example.financesimply.ui.theme.BudgetSafe
                )
            }
        }

        if (state.owedToYou > 0) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Owed to you (Splits)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${formatCompact(state.owedToYou)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            androidx.compose.material3.HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        }

        // Budget progress
        item {
            BudgetProgressBar(
                spent = state.monthlyTotal,
                budget = state.budget
            )
        }

        // Stats cards row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    label = "Remaining",
                    value = "₹${formatCompact(state.budgetRemaining)}",
                    color = if (state.budgetRemaining > 0) BudgetSafe else BudgetDanger
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CalendarMonth,
                    label = "Days Left",
                    value = "${state.daysLeftInMonth}",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Speed,
                    label = "Daily Avg",
                    value = "₹${formatCompact(state.dailyAverage)}",
                    color = MaterialTheme.colorScheme.tertiary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    label = "Projected Over",
                    value = if (state.projectedOverspend > 0) "₹${formatCompact(state.projectedOverspend)}" else "—",
                    color = if (state.projectedOverspend > 0) BudgetDanger else BudgetSafe
                )
            }
        }

        // Category breakdown
        if (state.categoryBreakdown.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "By Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        state.categoryBreakdown.forEachIndexed { index, item ->
                            val color = ChartColors[index % ChartColors.size]
                            val percentage = if (state.monthlyTotal > 0) {
                                (item.total / state.monthlyTotal * 100).toInt()
                            } else 0

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = color,
                                        modifier = Modifier.size(12.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.category,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Row {
                                    Text(
                                        text = "₹${formatCompact(item.total)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$percentage%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Mini progress bar
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                shape = RoundedCornerShape(2.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = if (state.monthlyTotal > 0) (item.total / state.monthlyTotal).toFloat() else 0f)
                                            .height(4.dp),
                                        shape = RoundedCornerShape(2.dp),
                                        color = color
                                    ) {}
                                }
                            }

                            if (index < state.categoryBreakdown.lastIndex) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }

        // Top outlets
        if (state.topOutlets.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Top Outlets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        state.topOutlets.forEachIndexed { index, outlet ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = ChartColors[index % ChartColors.size].copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = ChartColors[index % ChartColors.size]
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = outlet.merchant,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${outlet.count} transactions",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = "₹${formatCompact(outlet.total)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatCompact(amount: Double): String {
    return when {
        amount >= 100000 -> "${(amount / 100000).let { if (it == it.toLong().toDouble()) "${it.toLong()}L" else "%.1fL".format(it) }}"
        amount >= 1000 -> "${(amount / 1000).let { if (it == it.toLong().toDouble()) "${it.toLong()}K" else "%.1fK".format(it) }}"
        amount == amount.toLong().toDouble() -> "%,d".format(amount.toLong())
        else -> "%,.0f".format(amount)
    }
}
