package com.example.financesimply.service.parser

import com.example.financesimply.data.db.entity.TransactionType

/**
 * Parser for HDFC Bank SMS/notification alerts.
 *
 * Real sample format (from user):
 * ```
 * Sent Rs.90.00
 * From HDFC Bank A/C *7370
 * To NEW J N Store
 * On 20/08/26
 * Ref 202333535619
 * Not You?
 * Call 18002586161/SMS BLOCK UPI to 7308080808
 * ```
 *
 * Handles both "Sent" (debit) and "Received"/"Credited" (credit) patterns.
 */
class HdfcBankParser : NotificationParser {

    // Debit pattern: "Sent Rs.{amount} ... To {merchant}"
    private val debitPattern = Regex(
        """Sent\s+Rs\.?([\d,]+\.?\d*)\s*.*?To\s+(.+?)(?:\s*On|\s*$)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    // Credit pattern: "Received Rs.{amount} ... From {sender}"
    private val creditPattern = Regex(
        """(?:Received|Credited)\s+Rs\.?([\d,]+\.?\d*)\s*.*?(?:From|from)\s+(.+?)(?:\s*On|\s*$)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    // Alternative debit: "debited Rs.{amount} ... at {merchant}" (for card txns)
    private val debitAltPattern = Regex(
        """(?:debited|debit)\s+(?:Rs\.?|INR)\s*([\d,]+\.?\d*).*?(?:at|to|for)\s+(.+?)(?:\s*On|\s*Avl|\s*$)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    // Alternative credit: "credited Rs.{amount} to A/C"
    private val creditAltPattern = Regex(
        """(?:credited|credit)\s+(?:Rs\.?|INR)\s*([\d,]+\.?\d*).*?(?:from|by)\s+(.+?)(?:\s*On|\s*Avl|\s*$)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    override fun parse(packageName: String, title: String, text: String): ParsedTransaction? {
        val combined = "$title $text"

        // Try primary debit pattern first (matches user's exact format)
        debitPattern.find(combined)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            val merchant = match.groupValues[2].trim().takeIf { it.isNotBlank() }
            return ParsedTransaction(
                amount = amount,
                merchantName = merchant,
                transactionType = TransactionType.DEBIT
            )
        }

        // Try primary credit pattern
        creditPattern.find(combined)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            val merchant = match.groupValues[2].trim().takeIf { it.isNotBlank() }
            return ParsedTransaction(
                amount = amount,
                merchantName = merchant,
                transactionType = TransactionType.CREDIT
            )
        }

        // Try alternative debit
        debitAltPattern.find(combined)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            val merchant = match.groupValues[2].trim().takeIf { it.isNotBlank() }
            return ParsedTransaction(
                amount = amount,
                merchantName = merchant,
                transactionType = TransactionType.DEBIT
            )
        }

        // Try alternative credit
        creditAltPattern.find(combined)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            val merchant = match.groupValues[2].trim().takeIf { it.isNotBlank() }
            return ParsedTransaction(
                amount = amount,
                merchantName = merchant,
                transactionType = TransactionType.CREDIT
            )
        }

        return null
    }
}
