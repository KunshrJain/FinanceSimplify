package com.example.financesimply.service.parser

import com.example.financesimply.data.db.entity.TransactionType

/**
 * Parser for Google Pay (GPay) notifications.
 *
 * TODO: Replace placeholder regex with real patterns from user-supplied notification samples.
 * Expected sample format: title + body text from com.google.android.apps.nbu.paisa.user
 */
class GPayParser : NotificationParser {

    // Placeholder patterns — REPLACE with real notification formats
    private val debitPattern = Regex(
        """(?:paid|sent|debited)\s*₹?\s*([\d,]+\.?\d*).*?(?:to\s+(.+?))?(?:\s*$|\s*on|\s*from)""",
        RegexOption.IGNORE_CASE
    )
    private val creditPattern = Regex(
        """(?:received|credited)\s*₹?\s*([\d,]+\.?\d*).*?(?:from\s+(.+?))?(?:\s*$|\s*on|\s*to)""",
        RegexOption.IGNORE_CASE
    )

    override fun parse(packageName: String, title: String, text: String): ParsedTransaction? {
        val combined = "$title $text"

        // Try debit pattern first
        debitPattern.find(combined)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            val merchant = match.groupValues.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() }
            return ParsedTransaction(
                amount = amount,
                merchantName = merchant,
                transactionType = TransactionType.DEBIT
            )
        }

        // Try credit pattern
        creditPattern.find(combined)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            val merchant = match.groupValues.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() }
            return ParsedTransaction(
                amount = amount,
                merchantName = merchant,
                transactionType = TransactionType.CREDIT
            )
        }

        return null
    }
}
