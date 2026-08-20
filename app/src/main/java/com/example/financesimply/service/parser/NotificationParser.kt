package com.example.financesimply.service.parser

/**
 * Interface for parsing transaction notifications from payment/banking apps.
 * Each app should have its own implementation with specific regex patterns.
 */
interface NotificationParser {
    /**
     * Attempt to parse a transaction from a notification.
     * @param packageName The source app's package name
     * @param title The notification title
     * @param text The notification body text
     * @return A ParsedTransaction if the notification contains transaction info, null otherwise
     */
    fun parse(packageName: String, title: String, text: String): ParsedTransaction?
}
