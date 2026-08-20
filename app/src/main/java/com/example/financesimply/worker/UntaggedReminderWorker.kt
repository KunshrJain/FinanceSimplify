package com.example.financesimply.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.financesimply.R
import com.example.financesimply.data.db.SpendSenseDatabase

/**
 * Periodic worker that checks for untagged transactions and
 * reminds the user to categorize them via a local notification.
 * Runs every 6 hours.
 */
class UntaggedReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "untagged_reminder_work"
        const val CHANNEL_ID = "spendsense_untagged"
        const val NOTIFICATION_ID = 2001
    }

    override suspend fun doWork(): Result {
        val database = SpendSenseDatabase.getInstance(applicationContext)
        val untaggedCount = database.transactionDao().getPendingCount()

        if (untaggedCount > 0) {
            showNotification(untaggedCount)
        }

        return Result.success()
    }

    private fun showNotification(count: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Untagged Transactions",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Reminders to tag your transactions"
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Untagged transactions")
            .setContentText("You have $count transaction${if (count > 1) "s" else ""} waiting to be tagged")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
