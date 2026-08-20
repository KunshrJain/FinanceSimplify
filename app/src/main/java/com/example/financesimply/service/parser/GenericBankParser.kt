package com.example.financesimply.service.parser

import com.example.financesimply.data.db.entity.TransactionType

/**
 * Generic bank SMS/notification parser.
 * Attempts to parse common Indian bank notification formats.
 *
 * TODO: Add user's specific bank app package names and refine regex
 * with real notification samples.
 */
class GenericBankParser : NotificationParser {

    // Common Indian bank debit notification patterns
    private val debitPatterns = listOf(
        // "Rs.500 debited from A/c XX1234"
        Regex("""(?:Rs\.?|INR|₹)\s*([\d,]+\.?\d*)\s*(?:has been\s+)?debited.*?(?:at\s+(.+?))?(?:\s*$|\s*on|\s*Avl)""", RegexOption.IGNORE_CASE),
        // "Debited Rs 500 from account"
        Regex("""debited\s*(?:Rs\.?|INR|₹)\s*([\d,]+\.?\d*).*?(?:at\s+(.+?))?(?:\s*$|\s*on|\s*Avl)""", RegexOption.IGNORE_CASE),
        // "Payment of Rs 500 done at MERCHANT"
        Regex("""payment\s+of\s*(?:Rs\.?|INR|₹)\s*([\d,]+\.?\d*).*?(?:(?:at|to)\s+(.+?))?(?:\s*$|\s*on)""", RegexOption.IGNORE_CASE),
        // "Txn of Rs.500 at MERCHANT"
        Regex("""txn\s+of\s*(?:Rs\.?|INR|₹)\s*([\d,]+\.?\d*).*?(?:at\s+(.+?))?(?:\s*$|\s*on)""", RegexOption.IGNORE_CASE)
    )

    // Common Indian bank credit notification patterns
    private val creditPatterns = listOf(
        // "Rs.500 credited to A/c XX1234"
        Regex("""(?:Rs\.?|INR|₹)\s*([\d,]+\.?\d*)\s*(?:has been\s+)?credited.*?(?:from\s+(.+?))?(?:\s*$|\s*on|\s*Avl)""", RegexOption.IGNORE_CASE),
        // "Credited Rs 500"
        Regex("""credited\s*(?:Rs\.?|INR|₹)\s*([\d,]+\.?\d*).*?(?:from\s+(.+?))?(?:\s*$|\s*on|\s*Avl)""", RegexOption.IGNORE_CASE)
    )

    override fun parse(packageName: String, title: String, text: String): ParsedTransaction? {
        val combined = "$title $text"

        // Try debit patterns
        for (pattern in debitPatterns) {
            pattern.find(combined)?.let { match ->
                val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return@let
                val merchant = match.groupValues.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() }
                return ParsedTransaction(
                    amount = amount,
                    merchantName = merchant,
                    transactionType = TransactionType.DEBIT
                )
            }
        }

        // Try credit patterns
        for (pattern in creditPatterns) {
            pattern.find(combined)?.let { match ->
                val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return@let
                val merchant = match.groupValues.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() }
                return ParsedTransaction(
                    amount = amount,
                    merchantName = merchant,
                    transactionType = TransactionType.CREDIT
                )
            }
        }

        return null
    }
}
