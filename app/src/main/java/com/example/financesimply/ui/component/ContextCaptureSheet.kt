package com.example.financesimply.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.financesimply.ui.viewmodel.ContextCaptureUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextCaptureSheet(
    state: ContextCaptureUiState,
    onCategoryChanged: (String) -> Unit,
    onTagChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onForSomeoneElseChanged: (Boolean) -> Unit,
    onSomeoneElseNameChanged: (String) -> Unit,
    onExemptChanged: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.isVisible || state.transaction == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "New Transaction",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₹${"%,.2f".format(state.transaction.amount)} • ${
                            state.transaction.merchantFriendly.ifBlank {
                                state.transaction.merchantRaw.ifBlank { "Unknown" }
                            }
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val isCredit = state.transaction.transactionType == com.example.financesimply.data.db.entity.TransactionType.CREDIT

            // Category input with suggestions
            Text(
                text = if (isCredit) "Who sent this?" else "What was this for?",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.categoryInput,
                onValueChange = onCategoryChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Food, Transport, Shopping...") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Category suggestion chips
            if (state.categorySuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val filtered = if (state.categoryInput.isBlank()) {
                    state.categorySuggestions.take(8)
                } else {
                    state.categorySuggestions.filter {
                        it.contains(state.categoryInput, ignoreCase = true)
                    }.take(6)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Using a simple scrollable row of chips
                    filtered.forEach { suggestion ->
                        FilterChip(
                            selected = state.categoryInput == suggestion,
                            onClick = { onCategoryChanged(suggestion) },
                            label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tag/note
            OutlinedTextField(
                value = state.noteInput,
                onValueChange = onNoteChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Note (optional)") },
                placeholder = { Text("Add a note...") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // For someone else toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isCredit) "Was this sent on behalf of someone else?" else "Was this for someone else?")
                }
                Switch(
                    checked = state.isForSomeoneElse,
                    onCheckedChange = onForSomeoneElseChanged
                )
            }

            AnimatedVisibility(
                visible = state.isForSomeoneElse,
                enter = fadeIn() + slideInVertically()
            ) {
                OutlinedTextField(
                    value = state.someoneElseName,
                    onValueChange = onSomeoneElseNameChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    placeholder = { Text("Their name (e.g. Dad)") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Exempt from budget
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exempt from budget")
                }
                Checkbox(
                    checked = state.isExemptFromBudget,
                    onCheckedChange = onExemptChanged
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Tag Later")
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }
            }
        }
    }
}
