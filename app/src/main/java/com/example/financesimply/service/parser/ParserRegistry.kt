package com.example.financesimply.service.parser

/**
 * Registry that maps app package names to their notification parsers.
 * Extensible — add new packages and parsers as needed.
 */
object ParserRegistry {

    private val gpayParser = GPayParser()
    private val phonePeParser = PhonePeParser()
    private val paytmParser = PaytmParser()
    private val hdfcBankParser = HdfcBankParser()
    private val genericBankParser = GenericBankParser()

    private val parserMap = mutableMapOf<String, NotificationParser>(
        "com.google.android.apps.nbu.paisa.user" to gpayParser,
        "com.phonepe.app" to phonePeParser,
        "net.one97.paytm" to paytmParser,
        // SMS/Messages apps — HDFC bank alerts arrive via SMS
        "com.google.android.apps.messaging" to hdfcBankParser,  // Google Messages
        "com.samsung.android.messaging" to hdfcBankParser,       // Samsung Messages
        "com.android.mms" to hdfcBankParser                      // AOSP/default SMS
    )

    /**
     * Get the parser for a given package name.
     * Falls back to [GenericBankParser] for any whitelisted package
     * that doesn't have a specific parser.
     */
    fun getParser(packageName: String): NotificationParser {
        return parserMap[packageName] ?: genericBankParser
    }

    /**
     * Register a custom parser for a specific package name.
     */
    fun registerParser(packageName: String, parser: NotificationParser) {
        parserMap[packageName] = parser
    }

    /**
     * Check if a package has a dedicated parser (vs falling back to generic).
     */
    fun hasDedicatedParser(packageName: String): Boolean {
        return parserMap.containsKey(packageName)
    }
}
