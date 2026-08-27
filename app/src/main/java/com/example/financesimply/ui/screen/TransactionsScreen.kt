package com.example.financesimply.ui.screen

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financesimply.data.db.entity.TransactionEntity
import com.example.financesimply.ui.component.TransactionCard
import com.example.financesimply.ui.viewmodel.TransactionFilter
import com.example.financesimply.ui.viewmodel.TransactionListViewModel

@Composable
fun TransactionsScreen(
    viewModel: TransactionListViewModel = viewModel(),
    onTransactionClick: (TransactionEntity) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                if (state.untaggedCount > 0) {
                    Text(
                        text = "${state.untaggedCount} untagged",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter chips — horizontally scrollable
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.filter == TransactionFilter.ALL,
                onClick = { viewModel.setFilter(TransactionFilter.ALL) },
                label = { Text("All") },
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected = state.filter == TransactionFilter.DEBIT_ONLY,
                onClick = { viewModel.setFilter(TransactionFilter.DEBIT_ONLY) },
                label = { Text("Debits") },
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected = state.filter == TransactionFilter.CREDIT_ONLY,
                onClick = { viewModel.setFilter(TransactionFilter.CREDIT_ONLY) },
                label = { Text("Credits") },
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected = state.filter == TransactionFilter.UNTAGGED,
                onClick = { viewModel.setFilter(TransactionFilter.UNTAGGED) },
                label = { Text("Untagged") },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
            FilterChip(
                selected = state.filter == TransactionFilter.SPLIT,
                onClick = { viewModel.setFilter(TransactionFilter.SPLIT) },
                label = { Text("Splits") },
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected = state.filter == TransactionFilter.EXEMPT,
                onClick = { viewModel.setFilter(TransactionFilter.EXEMPT) },
                label = { Text("Exempt") },
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected = state.filter == TransactionFilter.OWED,
                onClick = { viewModel.setFilter(TransactionFilter.OWED) },
                label = { Text("Owed") },
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Transaction list
        if (state.transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No transactions yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Your transactions will appear here\nonce SpendSense captures them",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = state.transactions,
                    key = { it.id }
                ) { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
