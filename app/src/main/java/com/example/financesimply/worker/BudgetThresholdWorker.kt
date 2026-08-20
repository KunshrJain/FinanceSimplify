package com.example.financesimply.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.financesimply.R
import com.example.financesimply.data.db.SpendSenseDatabase
import com.example.financesimply.data.preferences.UserPreferences
import java.util.Calendar

/**
 * Periodic worker that checks monthly spend against budget
 * and fires notifications at 80%, 100%, and each subsequent 10% over.
 * Runs every 2 hours.
 */
class BudgetThresholdWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "budget_threshold_work"
        const val CHANNEL_ID = "spendsense_budget"
        const val NOTIFICATION_ID = 2002
    }

    override suspend fun doWork(): Result {
        val database = SpendSenseDatabase.getInstance(applicationContext)
        val prefs = UserPreferences(applicationContext)

        val budget = prefs.getMonthlyBudgetSync()
        if (budget <= 0) return Result.success()

        // Get current month boundaries
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val monthStart = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        val monthEnd = cal.timeInMillis

        val totalSpent = database.transactionDao().getMonthlyTotalSync(monthStart, monthEnd)
        val percentage = ((totalSpent / budget) * 100).toLong()

        // Determine which threshold to notify at
        val lastNotified = prefs.getLastThresholdSync()
        val currentThreshold = when {
            percentage >= 110 -> (percentage / 10) * 10 // Every 10% over 100
            percentage >= 100 -> 100L
            percentage >= 80 -> 80L
            else -> 0L
        }

        if (currentThreshold > lastNotified && currentThreshold >= 80) {
            val message = when {
                currentThreshold == 80L -> "You've used 80% of your monthly budget (₹${totalSpent.toLong()} / ₹$budget)"
                currentThreshold == 100L -> "You've reached your monthly budget! (₹${totalSpent.toLong()} / ₹$budget)"
                else -> "You're ${currentThreshold}% over budget (₹${totalSpent.toLong()} / ₹$budget)"
            }
            showNotification(message, currentThreshold)
            prefs.setLastBudgetThresholdNotified(currentThreshold)
        }

        return Result.success()
    }

    private fun showNotification(message: String, threshold: Long) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Budget Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when you approach or exceed your monthly budget"
        }
        notificationManager.createNotificationChannel(channel)

        val title = if (threshold >= 100) "⚠️ Budget exceeded" else "📊 Budget warning"

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
