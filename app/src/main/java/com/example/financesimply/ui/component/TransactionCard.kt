package com.example.financesimply.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.LabelOff
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.financesimply.data.db.entity.TransactionEntity
import com.example.financesimply.data.db.entity.TransactionType
import com.example.financesimply.ui.theme.BudgetSafe
import com.example.financesimply.ui.theme.PureBlack
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionCard(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDebit = transaction.transactionType == TransactionType.DEBIT
    val amountColor by animateColorAsState(
        targetValue = PureBlack,
        animationSpec = tween(300),
        label = "amountColor"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: icon + merchant/category info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Transaction type icon
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = amountColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (isDebit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = if (isDebit) "Debit" else "Credit",
                        tint = amountColor,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = transaction.merchantFriendly.ifBlank {
                            transaction.merchantRaw.ifBlank { "Unknown" }
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (transaction.category.isNotBlank()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Label,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = transaction.category,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.LabelOff,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Untagged",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Status badges row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTimestamp(transaction.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (transaction.splitDetails != null) {
                            Icon(
                                imageVector = Icons.Default.CallSplit,
                                contentDescription = "Split bill",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            val unsettledCount = transaction.splitDetails.count { !it.isSettled }
                            if (unsettledCount > 0) {
                                Text(
                                    text = "$unsettledCount to settle",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            // Right: amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isDebit) "−" else "+"}₹${formatAmount(transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                if (transaction.splitDetails != null) {
                    Text(
                        text = "You paid ₹${formatAmount(transaction.budgetImpactAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        }
    }
}

private fun formatAmount(amount: Double): String {
    return if (amount == amount.toLong().toDouble()) {
        "%,d".format(amount.toLong())
    } else {
        "%,.2f".format(amount)
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
