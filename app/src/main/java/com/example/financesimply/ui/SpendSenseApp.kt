package com.example.financesimply.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.financesimply.service.TransactionEventBus
import com.example.financesimply.ui.component.ContextCaptureSheet
import com.example.financesimply.ui.screen.AddictionScreen
import com.example.financesimply.ui.screen.DashboardScreen
import com.example.financesimply.ui.screen.OnboardingScreen
import com.example.financesimply.ui.screen.SettingsScreen
import com.example.financesimply.ui.screen.TransactionsScreen
import com.example.financesimply.ui.viewmodel.ContextCaptureViewModel
import com.example.financesimply.ui.viewmodel.SettingsViewModel

sealed class Screen(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    data object Transactions : Screen("transactions", "Transactions", Icons.Filled.Receipt, Icons.Outlined.Receipt)
    data object Insights : Screen("insights", "Insights", Icons.Filled.Insights, Icons.Outlined.Insights)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Transactions,
    Screen.Insights,
    Screen.Settings
)

@Composable
fun SpendSenseApp(
    targetTransactionId: Long? = null,
    onTransactionHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val settingsViewModel: SettingsViewModel = viewModel()
    val contextCaptureViewModel: ContextCaptureViewModel = viewModel()

    val settingsState by settingsViewModel.uiState.collectAsState()
    val contextCaptureState by contextCaptureViewModel.uiState.collectAsState()

    // Handle incoming transaction intent
    LaunchedEffect(targetTransactionId) {
        if (targetTransactionId != null) {
            contextCaptureViewModel.showForTransactionId(targetTransactionId)
            onTransactionHandled()
        }
    }

    // Check onboarding status
    if (!settingsState.isLoading && !settingsState.onboardingCompleted) {
        OnboardingScreen(
            onComplete = { budget ->
                settingsViewModel.updateBudget(budget)
                settingsViewModel.setOnboardingCompleted()
            }
        )
        return
    }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.label
                            )
                        },
                        label = {
                            Text(
                                text = screen.label,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen()
                }
                composable(Screen.Transactions.route) {
                    TransactionsScreen(
                        onTransactionClick = { transaction ->
                            contextCaptureViewModel.showForTransaction(transaction)
                        }
                    )
                }
                composable(Screen.Insights.route) {
                    AddictionScreen()
                }
                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
            }
        }

        // Global context capture overlay
        ContextCaptureSheet(
            state = contextCaptureState,
            onCategoryChanged = contextCaptureViewModel::updateCategory,
            onTagChanged = contextCaptureViewModel::updateTag,
            onNoteChanged = contextCaptureViewModel::updateNote,
            onForSomeoneElseChanged = contextCaptureViewModel::updateIsForSomeoneElse,
            onSomeoneElseNameChanged = contextCaptureViewModel::updateSomeoneElseName,
            onExemptChanged = contextCaptureViewModel::updateIsExempt,
            onSave = contextCaptureViewModel::save,
            onDismiss = contextCaptureViewModel::dismiss
        )
    }
}
