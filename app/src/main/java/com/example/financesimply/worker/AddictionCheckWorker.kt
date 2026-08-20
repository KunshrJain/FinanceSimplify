package com.example.financesimply.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.financesimply.R
import com.example.financesimply.data.db.SpendSenseDatabase
import com.example.financesimply.data.db.entity.WatchedItemEntity

/**
 * Daily worker that checks watched (addiction-tracked) items for
 * frequency or spend spikes. Compares current 7-day metrics
 * against the 30-day average and nudges the user if there's a spike.
 */
class AddictionCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "addiction_check_work"
        const val CHANNEL_ID = "spendsense_addiction"
        const val NOTIFICATION_ID_BASE = 3000
        const val SPIKE_MULTIPLIER = 1.5 // 50% above average triggers a nudge
    }

    override suspend fun doWork(): Result {
        val database = SpendSenseDatabase.getInstance(applicationContext)
        val watchedItems = database.watchedItemDao().getAllSync()

        if (watchedItems.isEmpty()) return Result.success()

        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000
        val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000

        watchedItems.forEachIndexed { index, item ->
            checkItem(database, item, sevenDaysAgo, thirtyDaysAgo, now, index)
        }

        return Result.success()
    }

    private suspend fun checkItem(
        database: SpendSenseDatabase,
        item: WatchedItemEntity,
        sevenDaysAgo: Long,
        thirtyDaysAgo: Long,
        now: Long,
        index: Int
    ) {
        val dao = database.transactionDao()

        // 7-day metrics
        val freq7 = dao.getFrequencyForItem(item.value, sevenDaysAgo, now)
        val spend7 = dao.getSpendForItem(item.value, sevenDaysAgo, now)

        // 30-day metrics (weekly average)
        val freq30 = dao.getFrequencyForItem(item.value, thirtyDaysAgo, now)
        val spend30 = dao.getSpendForItem(item.value, thirtyDaysAgo, now)

        // Calculate weekly averages from 30-day data
        val avgWeeklyFreq = freq30 / 4.0 // ~4 weeks in 30 days
        val avgWeeklySpend = spend30 / 4.0

        // Check for frequency spike
        if (avgWeeklyFreq > 0 && freq7 > avgWeeklyFreq * SPIKE_MULTIPLIER) {
            val message = "You've used ${item.value} $freq7 times this week, " +
                    "up from your usual ${avgWeeklyFreq.toInt()}"
            showNudge(item.value, message, NOTIFICATION_ID_BASE + index * 2)
            return // Don't spam with both frequency and spend nudges
        }

        // Check for spend spike
        if (avgWeeklySpend > 0 && spend7 > avgWeeklySpend * SPIKE_MULTIPLIER) {
            val message = "You've spent ₹${spend7.toLong()} on ${item.value} this week, " +
                    "up from your usual ₹${avgWeeklySpend.toLong()}"
            showNudge(item.value, message, NOTIFICATION_ID_BASE + index * 2 + 1)
        }
    }

    private fun showNudge(itemName: String, message: String, notificationId: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Spending Insights",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Gentle nudges about your spending patterns"
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("💡 Spending insight: $itemName")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
