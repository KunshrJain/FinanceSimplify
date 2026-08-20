package com.example.financesimply.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.financesimply.MainActivity
import com.example.financesimply.R
import com.example.financesimply.data.db.SpendSenseDatabase
import com.example.financesimply.data.db.entity.TransactionEntity
import com.example.financesimply.data.db.entity.TransactionStatus
import com.example.financesimply.data.preferences.UserPreferences
import com.example.financesimply.service.parser.ParserRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.security.MessageDigest

/**
 * Foreground notification listener that captures transaction notifications
 * from whitelisted payment/banking apps, parses them, and stores in Room.
 *
 * Deduplication: uses an MD5 hash of (packageName + amount + 60s time bucket)
 * to recognize duplicate notifications for the same transaction (e.g. "processing"
 * followed by "success" notifications).
 */
class SpendSenseNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "SpendSenseNL"
        private const val DEDUP_WINDOW_MS = 60_000L // 60 seconds
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: SpendSenseDatabase
    private lateinit var userPreferences: UserPreferences

    override fun onCreate() {
        super.onCreate()
        database = SpendSenseDatabase.getInstance(applicationContext)
        userPreferences = UserPreferences(applicationContext)
        Log.d(TAG, "SpendSense NotificationListener created")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "SpendSense NotificationListener destroyed")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        val packageName = sbn.packageName ?: return
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Check if this package is whitelisted
        val whitelisted = runBlocking {
            userPreferences.getWhitelistedPackagesSync()
        }
        if (packageName !in whitelisted) return

        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        Log.d(TAG, "Notification from $packageName — title: $title, text: $text")

        // Parse the notification
        val parser = ParserRegistry.getParser(packageName)
        val parsed = parser.parse(packageName, title, text) ?: run {
            Log.d(TAG, "Could not parse notification from $packageName")
            return
        }

        // Generate dedup hash
        val timeBucket = parsed.timestamp / DEDUP_WINDOW_MS
        val hashInput = "$packageName|${parsed.amount}|${parsed.transactionType}|$timeBucket"
        val hash = md5(hashInput)

        serviceScope.launch {
            // Check for duplicate
            val existing = database.transactionDao().findByNotificationHash(hash)
            if (existing != null) {
                Log.d(TAG, "Duplicate notification detected, skipping. Hash: $hash")
                return@launch
            }

            // Check for favorite outlet match to auto-fill merchant name
            val merchantRaw = parsed.merchantName ?: ""
            var merchantFriendly = merchantRaw
            var autoCategory = ""

            if (merchantRaw.isNotBlank()) {
                val outlet = database.favoriteOutletDao().findMatchingOutlet(merchantRaw)
                if (outlet != null) {
                    merchantFriendly = outlet.friendlyName
                    autoCategory = outlet.defaultCategory
                }
            }

            val transaction = TransactionEntity(
                amount = parsed.amount,
                merchantRaw = merchantRaw,
                merchantFriendly = merchantFriendly,
                transactionType = parsed.transactionType,
                category = autoCategory,
                sourceApp = packageName,
                timestamp = parsed.timestamp,
                notificationHash = hash,
                status = if (autoCategory.isNotBlank()) TransactionStatus.TAGGED else TransactionStatus.PENDING
            )

            val id = database.transactionDao().insert(transaction)
            val savedTransaction = transaction.copy(id = id)

            Log.d(TAG, "Transaction saved: ₹${parsed.amount} ${parsed.transactionType} from $packageName")

            // Send Local Notification if pending
            if (savedTransaction.status == TransactionStatus.PENDING) {
                showLocalNotification(savedTransaction)
            }
        }
    }

    private fun showLocalNotification(transaction: TransactionEntity) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "spendsense_pending_transactions"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pending Transactions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for un-tagged transactions"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("transaction_id", transaction.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            transaction.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = "₹${"%,.2f".format(transaction.amount)} at ${transaction.merchantFriendly.ifBlank { "Unknown" }} — tap to add details"
        
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("New Transaction")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(transaction.id.toInt(), builder.build())
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Not needed for our use case
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
