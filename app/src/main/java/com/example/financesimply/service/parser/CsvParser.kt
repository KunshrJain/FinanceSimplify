package com.example.financesimply.service.parser

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Parses bank account statement CSV files.
 *
 * Supports multiple Indian bank formats including HDFC, SBI, ICICI, Axis, Kotak, and others.
 * 
 * Features:
 * - Auto-detects column headers from many bank formats
 * - Handles both separate Debit/Credit columns and single Amount+Type columns
 * - Deduplicates using RefNo
 * - Auto-categorizes based on narration keywords and UPI payee extraction
 * - Handles quoted fields and commas in amounts
 * - Extracts merchant-friendly names from UPI narrations
 */
class CsvParser(private val context: Context) {

    data class CsvTransaction(
        val date: String,
        val description: String,
        val refNo: String,
        val valueDate: String,
        val debit: Double?,
        val credit: Double?,
        val balance: Double?,
        val category: String,
        val merchantName: String,
        val rawRow: List<String>
    )

    data class CsvResult(
        val headers: List<String>,
        val transactions: List<CsvTransaction>,
        val totalDebit: Double,
        val totalCredit: Double,
        val error: String? = null
    )

    /**
     * Extract a human-readable merchant/payee name from a bank narration string.
     * Handles UPI, NEFT, IMPS, and plain-text narrations.
     */
    fun extractMerchantName(narration: String): String {
        val trimmed = narration.trim()

        // UPI format: UPI/txnId/PAYEE_NAME/BANK/...
        // or: UPI-PAYEE_NAME-BANK-txnId
        if (trimmed.uppercase().startsWith("UPI")) {
            val slashParts = trimmed.split("/")
            if (slashParts.size >= 3) {
                val payee = slashParts[2].trim()
                if (payee.isNotBlank() && payee.length > 1) {
                    return cleanMerchantName(payee)
                }
            }
            val dashParts = trimmed.split("-")
            if (dashParts.size >= 2) {
                val payee = dashParts[1].trim()
                if (payee.isNotBlank() && payee.length > 1) {
                    return cleanMerchantName(payee)
                }
            }
        }

        // NEFT format: NEFT/txnId/SENDER_NAME/...
        if (trimmed.uppercase().startsWith("NEFT") || trimmed.uppercase().startsWith("IMPS")) {
            val parts = trimmed.split("/")
            if (parts.size >= 3) {
                val name = parts[2].trim()
                if (name.isNotBlank()) {
                    return cleanMerchantName(name)
                }
            }
        }

        // For anything else, just clean it up — take first 40 chars, trim trailing numbers/codes
        return cleanMerchantName(trimmed.take(40))
    }

    private fun cleanMerchantName(raw: String): String {
        return raw
            .replace(Regex("\\d{10,}"), "")  // Remove long number sequences (txn IDs, phone numbers)
            .replace(Regex("@\\w+"), "")       // Remove UPI handles like @okicici
            .replace(Regex("[*#]+"), " ")       // Replace * and # with space
            .replace(Regex("\\s+"), " ")        // Collapse multiple spaces
            .trim()
            .take(30)
            .trimEnd('-', '/', ' ', '.')
            .ifBlank { raw.take(30) }
    }

    /**
     * Auto-categorize a transaction based on narration keywords.
     * Handles UPI narrations by first extracting the payee name.
     */
    fun categorize(narration: String): String {
        val desc = narration.uppercase()

        // Extract payee from UPI narrations for better matching
        val payeeName = if (desc.startsWith("UPI")) {
            extractMerchantName(narration).uppercase()
        } else {
            ""
        }
        val searchText = "$desc $payeeName"

        return when {
            // Income
            searchText.contains("SALARY") || searchText.contains("SAL CR") ||
                searchText.contains("SAL CREDIT") -> "Income"

            // Food & Delivery
            searchText.contains("SWIGGY") || searchText.contains("ZOMATO") ||
                searchText.contains("FOOD") || searchText.contains("DUNZO") ||
                searchText.contains("BLINKIT") || searchText.contains("ZEPTO") ||
                searchText.contains("INSTAMART") || searchText.contains("BIGBASKET") ||
                searchText.contains("GROFERS") || searchText.contains("DOMINOS") ||
                searchText.contains("MCDONALDS") || searchText.contains("KFC") ||
                searchText.contains("BURGER KING") || searchText.contains("PIZZA HUT") ||
                searchText.contains("STARBUCKS") || searchText.contains("CAFE") ||
                searchText.contains("RESTAURANT") || searchText.contains("HOTEL") ||
                searchText.contains("EATERY") || searchText.contains("BAKERY") ||
                searchText.contains("CHAAYOS") || searchText.contains("HALDIRAM") -> "Food"

            // Subscriptions
            searchText.contains("NETFLIX") || searchText.contains("HOTSTAR") ||
                searchText.contains("SPOTIFY") || searchText.contains("PRIME VIDEO") ||
                searchText.contains("YOUTUBE") || searchText.contains("DISNEY") ||
                searchText.contains("APPLE.COM") || searchText.contains("GOOGLE PLAY") ||
                searchText.contains("SUBSCRIPTION") || searchText.contains("MEMBERSHIP") ||
                searchText.contains("PREMIUM") || searchText.contains("JIOCINEMA") ||
                searchText.contains("SONYLIV") || searchText.contains("GAANA") ||
                searchText.contains("WYNK") || searchText.contains("CRED") -> "Subscription"

            // Cash
            searchText.contains("ATM") || searchText.contains("CASH WDL") ||
                searchText.contains("CASH WITHDRAWAL") || searchText.contains("SELF WDL") -> "Cash"

            // Transport
            searchText.contains("UBER") || searchText.contains("OLA") ||
                searchText.contains("RAPIDO") || searchText.contains("METRO") ||
                searchText.contains("IRCTC") || searchText.contains("RAILWAY") ||
                searchText.contains("PETROL") || searchText.contains("FUEL") ||
                searchText.contains("IOCL") || searchText.contains("BPCL") ||
                searchText.contains("HPCL") || searchText.contains("PARKING") ||
                searchText.contains("TOLL") || searchText.contains("FASTAG") ||
                searchText.contains("INDIGO") || searchText.contains("SPICEJET") ||
                searchText.contains("MAKEMYTRIP") || searchText.contains("GOIBIBO") ||
                searchText.contains("REDBUS") || searchText.contains("FLIGHT") -> "Transport"

            // Shopping
            searchText.contains("AMAZON") || searchText.contains("FLIPKART") ||
                searchText.contains("MYNTRA") || searchText.contains("MEESHO") ||
                searchText.contains("AJIO") || searchText.contains("NYKAA") ||
                searchText.contains("TATACLIQ") || searchText.contains("SNAPDEAL") ||
                searchText.contains("SHOP") || searchText.contains("MALL") ||
                searchText.contains("MART") || searchText.contains("STORE") ||
                searchText.contains("RELIANCE") || searchText.contains("DMART") ||
                searchText.contains("BIG BAZAAR") || searchText.contains("CROMA") ||
                searchText.contains("DECATHLON") -> "Shopping"

            // Bills & Utilities
            searchText.contains("ELECTRICITY") || searchText.contains("WATER") ||
                searchText.contains("GAS") || searchText.contains("BROADBAND") ||
                searchText.contains("MOBILE RECHARGE") || searchText.contains("RECHARGE") ||
                searchText.contains("AIRTEL") || searchText.contains("JIO ") ||
                searchText.contains("VI ") || searchText.contains("BSNL") ||
                searchText.contains("WIFI") || searchText.contains("INTERNET") ||
                searchText.contains("BILL PAYMENT") || searchText.contains("UTILITY") ||
                searchText.contains("POSTPAID") || searchText.contains("PREPAID") ||
                searchText.contains("DTH") || searchText.contains("TATA SKY") -> "Bills"

            // Rent & EMI
            searchText.contains("RENT") || searchText.contains("EMI") ||
                searchText.contains("LOAN") || searchText.contains("MORTGAGE") ||
                searchText.contains("INSURANCE") || searchText.contains("LIC") ||
                searchText.contains("BAJAJ FIN") || searchText.contains("HDFC LIFE") -> "Bills"

            // Health
            searchText.contains("HOSPITAL") || searchText.contains("PHARMACY") ||
                searchText.contains("MEDICAL") || searchText.contains("APOLLO") ||
                searchText.contains("PRACTO") || searchText.contains("1MG") ||
                searchText.contains("PHARMEASY") || searchText.contains("NETMEDS") ||
                searchText.contains("DOCTOR") || searchText.contains("CLINIC") ||
                searchText.contains("DIAGNOSTIC") || searchText.contains("LAB") ||
                searchText.contains("HEALTHIAN") -> "Health"

            // Education
            searchText.contains("SCHOOL") || searchText.contains("COLLEGE") ||
                searchText.contains("UNIVERSITY") || searchText.contains("TUITION") ||
                searchText.contains("COURSERA") || searchText.contains("UDEMY") ||
                searchText.contains("UNACADEMY") || searchText.contains("BYJU") ||
                searchText.contains("BOOK") || searchText.contains("EDUCATION") -> "Education"

            // Entertainment
            searchText.contains("PVR") || searchText.contains("INOX") ||
                searchText.contains("BOOKMYSHOW") || searchText.contains("MOVIE") ||
                searchText.contains("GAME") || searchText.contains("PLAY STORE") ||
                searchText.contains("STEAM") || searchText.contains("CINEMA") -> "Entertainment"

            // Investments
            searchText.contains("MUTUAL FUND") || searchText.contains("SIP") ||
                searchText.contains("ZERODHA") || searchText.contains("GROWW") ||
                searchText.contains("UPSTOX") || searchText.contains("STOCK") ||
                searchText.contains("DEMAT") || searchText.contains("INVESTMENT") -> "Investment"

            // Refunds
            searchText.contains("CASHBACK") || searchText.contains("REVERSAL") ||
                searchText.contains("REFUND") || searchText.contains("CREDIT REVERSAL") -> "Refund"

            // Bank charges
            searchText.contains("INTEREST") || searchText.contains("INT.COLL") ||
                searchText.contains("BANK CHARGE") || searchText.contains("SERVICE CHARGE") ||
                searchText.contains("ANNUAL FEE") || searchText.contains("GST") -> "Bank"

            // Generic transfers last (because UPI is very common)
            searchText.contains("UPI") -> "Transfer"
            searchText.contains("NEFT") || searchText.contains("IMPS") ||
                searchText.contains("RTGS") -> "Transfer"

            else -> ""
        }
    }

    /**
     * Parse a CSV file from the given Uri.
     * Auto-detects column positions from the header row.
     */
    fun parse(uri: Uri): CsvResult {
        try {
            val reader = BufferedReader(
                InputStreamReader(context.contentResolver.openInputStream(uri))
            )

            val lines = reader.readLines()
            reader.close()

            if (lines.isEmpty()) {
                return CsvResult(
                    headers = emptyList(),
                    transactions = emptyList(),
                    totalDebit = 0.0,
                    totalCredit = 0.0,
                    error = "File is empty"
                )
            }

            // Find the header row (skip blank lines and metadata lines)
            var headerIndex = -1
            var headers = emptyList<String>()
            for (i in lines.indices) {
                val cells = parseCsvLine(lines[i])
                if (cells.any { it.contains("date", ignoreCase = true) } &&
                    cells.any {
                        it.contains("debit", ignoreCase = true) ||
                        it.contains("credit", ignoreCase = true) ||
                        it.contains("amount", ignoreCase = true) ||
                        it.contains("withdrawal", ignoreCase = true) ||
                        it.contains("deposit", ignoreCase = true) ||
                        it.contains("dr", ignoreCase = true) ||
                        it.contains("cr", ignoreCase = true)
                    }
                ) {
                    headerIndex = i
                    headers = cells.map { it.trim() }
                    break
                }
            }

            if (headerIndex == -1) {
                for (i in lines.indices) {
                    val cells = parseCsvLine(lines[i])
                    if (cells.size >= 3) {
                        headerIndex = i
                        headers = cells.map { it.trim() }
                        break
                    }
                }
            }

            if (headerIndex == -1) {
                return CsvResult(
                    headers = emptyList(),
                    transactions = emptyList(),
                    totalDebit = 0.0,
                    totalCredit = 0.0,
                    error = "Could not find header row"
                )
            }

            // Detect column indices
            val dateCol = findColumn(headers, "date", "txn date", "transaction date", "trans date", "posting date", "txn dt")
            val descCol = findColumn(headers, "narration", "description", "particulars", "details", "remarks",
                "transaction details", "transaction remarks", "narration / details", "payment details", "transaction description")
            val refCol = findColumn(headers, "refno", "ref no", "reference", "chq/ref number", "chq./ref.no.",
                "transaction id", "ref no./cheque no.", "reference no", "utr no", "utr", "txn ref no")
            val valueDateCol = findColumn(headers, "valuedate", "value date", "value dt")
            val debitCol = findColumn(headers, "withdrawal", "debit", "debit amount", "dr", "withdrawal amt",
                "withdrawal amount", "debit(dr.)", "dr amount", "debit (inr)", "withdrawal (inr)")
            val creditCol = findColumn(headers, "deposit", "credit", "credit amount", "cr", "deposit amt",
                "deposit amount", "credit(cr.)", "cr amount", "credit (inr)", "deposit (inr)")
            val balanceCol = findColumn(headers, "balance", "closing balance", "available balance", "running balance",
                "balance (inr)", "available bal")

            // Check for single amount column (some banks use Amount + Dr/Cr indicator)
            val amountCol = findColumn(headers, "amount", "transaction amount", "txn amount", "amount (inr)")
            val typeCol = findColumn(headers, "type", "dr/cr", "dr / cr", "transaction type", "cr/dr", "debit/credit")

            // Parse data rows
            val transactions = mutableListOf<CsvTransaction>()
            val seenRefNos = mutableSetOf<String>()
            var totalDebit = 0.0
            var totalCredit = 0.0

            for (i in (headerIndex + 1) until lines.size) {
                val cells = parseCsvLine(lines[i])
                if (cells.all { it.isBlank() }) continue

                val date = cells.getOrNull(dateCol)?.trim() ?: ""
                val desc = cells.getOrNull(descCol)?.trim() ?: ""
                val refNo = cells.getOrNull(refCol)?.trim() ?: ""
                val valueDate = cells.getOrNull(valueDateCol)?.trim() ?: ""

                var debit: Double? = null
                var credit: Double? = null

                if (debitCol >= 0 || creditCol >= 0) {
                    // Separate debit/credit columns
                    debit = cells.getOrNull(debitCol)?.toAmount()
                    credit = cells.getOrNull(creditCol)?.toAmount()
                } else if (amountCol >= 0) {
                    // Single amount column with type indicator
                    val amount = cells.getOrNull(amountCol)?.toAmount()
                    val typeStr = cells.getOrNull(typeCol)?.trim()?.uppercase() ?: ""
                    if (amount != null) {
                        if (typeStr.contains("CR") || typeStr.contains("CREDIT") || typeStr == "C") {
                            credit = amount
                        } else {
                            debit = amount // Default to debit if unclear
                        }
                    }
                }

                val balance = cells.getOrNull(balanceCol)?.toAmount()

                if (date.isBlank() && desc.isBlank()) continue

                // Deduplicate using RefNo
                if (refNo.isNotBlank() && refNo in seenRefNos) continue
                if (refNo.isNotBlank()) seenRefNos.add(refNo)

                val category = categorize(desc)
                val merchantName = extractMerchantName(desc)

                if (debit != null) totalDebit += debit
                if (credit != null) totalCredit += credit

                transactions.add(
                    CsvTransaction(
                        date = date,
                        description = desc,
                        refNo = refNo,
                        valueDate = valueDate,
                        debit = debit,
                        credit = credit,
                        balance = balance,
                        category = category,
                        merchantName = merchantName,
                        rawRow = cells
                    )
                )
            }

            return CsvResult(
                headers = headers,
                transactions = transactions,
                totalDebit = totalDebit,
                totalCredit = totalCredit
            )
        } catch (e: Exception) {
            return CsvResult(
                headers = emptyList(),
                transactions = emptyList(),
                totalDebit = 0.0,
                totalCredit = 0.0,
                error = "Error reading file: ${e.message}"
            )
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result
    }

    private fun findColumn(headers: List<String>, vararg names: String): Int {
        // Exact match first
        for (name in names) {
            val index = headers.indexOfFirst { it.equals(name, ignoreCase = true) }
            if (index >= 0) return index
        }
        // Then partial match
        for (name in names) {
            val index = headers.indexOfFirst { it.contains(name, ignoreCase = true) }
            if (index >= 0) return index
        }
        return -1
    }

    private fun String.toAmount(): Double? {
        val cleaned = this.trim()
            .replace(",", "")
            .replace("\"", "")
            .replace(" ", "")
            .replace("₹", "")
            .replace("INR", "", ignoreCase = true)
            .replace("Rs.", "", ignoreCase = true)
            .replace("Rs", "", ignoreCase = true)
        if (cleaned.isBlank() || cleaned == "-" || cleaned == "0" || cleaned == "0.00") return null
        return cleaned.toDoubleOrNull()
    }
}
