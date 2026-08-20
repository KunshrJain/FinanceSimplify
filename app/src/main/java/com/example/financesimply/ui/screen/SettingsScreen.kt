package com.example.financesimply.ui.screen

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financesimply.data.db.entity.WatchedItemType
import com.example.financesimply.ui.theme.BudgetSafe
import com.example.financesimply.ui.viewmodel.FavoriteOutletsViewModel
import com.example.financesimply.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    outletsViewModel: FavoriteOutletsViewModel = viewModel()
) {
    val settingsState by settingsViewModel.uiState.collectAsState()
    val outletsState by outletsViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showBudgetDialog by remember { mutableStateOf(false) }
    var showOutletDialog by remember { mutableStateOf(false) }
    var showWatchedDialog by remember { mutableStateOf(false) }
    var showPackageDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Notification Access Status
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                color = if (settingsState.isNotificationAccessGranted)
                    BudgetSafe.copy(alpha = 0.1f)
                else
                    MaterialTheme.colorScheme.errorContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (settingsState.isNotificationAccessGranted)
                                Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (settingsState.isNotificationAccessGranted)
                                BudgetSafe else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Notification Access",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (settingsState.isNotificationAccessGranted)
                                    "Listening for transactions"
                                else
                                    "Required to capture transactions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (!settingsState.isNotificationAccessGranted) {
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                )
                            },
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Enable")
                        }
                    }
                }
            }
        }

        // Budget section
        item {
            SettingsSection(title = "Budget") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Monthly Budget",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "₹${"%,d".format(settingsState.budget)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    OutlinedButton(
                        onClick = { showBudgetDialog = true },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Change")
                    }
                }
            }
        }

        // Favorite Outlets section
        item {
            SettingsSection(
                title = "Favorite Outlets",
                actionIcon = Icons.Default.Add,
                onAction = { showOutletDialog = true }
            ) {
                if (outletsState.outlets.isEmpty()) {
                    Text(
                        text = "Map merchant names to friendly names for auto-tagging",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    outletsState.outlets.forEach { outlet ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = outlet.friendlyName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Pattern: ${outlet.rawMerchantPattern}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (outlet.defaultCategory.isNotBlank()) {
                                    Text(
                                        text = "Category: ${outlet.defaultCategory}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(onClick = { outletsViewModel.deleteOutlet(outlet) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }

        // Watched Items section (Addiction tracking)
        item {
            SettingsSection(
                title = "Watched Items",
                actionIcon = Icons.Default.Add,
                onAction = { showWatchedDialog = true }
            ) {
                if (settingsState.watchedItems.isEmpty()) {
                    Text(
                        text = "Track spending patterns for specific categories or merchants",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    settingsState.watchedItems.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (item.type == WatchedItemType.CATEGORY)
                                        Icons.Default.Visibility else Icons.Default.Store,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = item.value,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = item.type.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { settingsViewModel.removeWatchedItem(item) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        // Whitelisted apps section
        item {
            SettingsSection(
                title = "Monitored Apps",
                actionIcon = Icons.Default.Add,
                onAction = { showPackageDialog = true }
            ) {
                settingsState.whitelistedPackages.forEach { pkg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = friendlyAppName(pkg),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = pkg.split(".").last(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Dialogs
    if (showBudgetDialog) {
        BudgetDialog(
            currentBudget = settingsState.budget,
            onDismiss = { showBudgetDialog = false },
            onSave = { amount ->
                settingsViewModel.updateBudget(amount)
                showBudgetDialog = false
            }
        )
    }

    if (showOutletDialog) {
        OutletDialog(
            onDismiss = { showOutletDialog = false },
            onSave = { pattern, name, category ->
                outletsViewModel.addOutlet(pattern, name, category)
                showOutletDialog = false
            }
        )
    }

    if (showWatchedDialog) {
        WatchedItemDialog(
            onDismiss = { showWatchedDialog = false },
            onSave = { type, value ->
                settingsViewModel.addWatchedItem(type, value)
                showWatchedDialog = false
            }
        )
    }

    if (showPackageDialog) {
        PackageDialog(
            onDismiss = { showPackageDialog = false },
            onSave = { packageName ->
                settingsViewModel.addWhitelistedPackage(packageName)
                showPackageDialog = false
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (actionIcon != null && onAction != null) {
                    IconButton(onClick = onAction) {
                        Icon(
                            actionIcon,
                            contentDescription = "Add",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun BudgetDialog(
    currentBudget: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    var budgetText by remember { mutableStateOf(currentBudget.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Monthly Budget") },
        text = {
            OutlinedTextField(
                value = budgetText,
                onValueChange = { budgetText = it.filter { c -> c.isDigit() } },
                label = { Text("Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                prefix = { Text("₹") }
            )
        },
        confirmButton = {
            Button(onClick = {
                budgetText.toLongOrNull()?.let { onSave(it) }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun OutletDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var pattern by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Favorite Outlet") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Raw merchant pattern") },
                    placeholder = { Text("e.g. SWIGGY*BLR") },
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Friendly name") },
                    placeholder = { Text("e.g. Swiggy") },
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Default category (optional)") },
                    placeholder = { Text("e.g. Food") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (pattern.isNotBlank() && name.isNotBlank()) onSave(pattern, name, category) },
                enabled = pattern.isNotBlank() && name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun WatchedItemDialog(
    onDismiss: () -> Unit,
    onSave: (WatchedItemType, String) -> Unit
) {
    var value by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(WatchedItemType.CATEGORY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Watched Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WatchedItemType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = {
                                Text(type.name.lowercase().replaceFirstChar { it.uppercase() })
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(if (selectedType == WatchedItemType.CATEGORY) "Category name" else "Merchant name") },
                    placeholder = { Text(if (selectedType == WatchedItemType.CATEGORY) "e.g. Food" else "e.g. Swiggy") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (value.isNotBlank()) onSave(selectedType, value) },
                enabled = value.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PackageDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var packageName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Monitored App") },
        text = {
            OutlinedTextField(
                value = packageName,
                onValueChange = { packageName = it },
                label = { Text("Package name") },
                placeholder = { Text("e.g. com.bank.app") },
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = { if (packageName.isNotBlank()) onSave(packageName) },
                enabled = packageName.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun friendlyAppName(packageName: String): String {
    return when (packageName) {
        "com.google.android.apps.nbu.paisa.user" -> "Google Pay"
        "com.phonepe.app" -> "PhonePe"
        "net.one97.paytm" -> "Paytm"
        else -> packageName.split(".").takeLast(2).joinToString(" ")
            .replaceFirstChar { it.uppercase() }
    }
}
