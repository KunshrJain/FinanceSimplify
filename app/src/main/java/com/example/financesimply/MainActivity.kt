package com.example.financesimply

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.financesimply.ui.SpendSenseApp
import com.example.financesimply.ui.theme.SpendSenseTheme
import com.example.financesimply.worker.AddictionCheckWorker
import com.example.financesimply.worker.BudgetThresholdWorker
import com.example.financesimply.worker.UntaggedReminderWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private var targetTransactionId by mutableStateOf<Long?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // We just request it, if denied the user won't get notifications
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Handle initial intent if launched from notification
        handleIntent(intent)

        // Schedule periodic workers
        scheduleWorkers()

        setContent {
            SpendSenseTheme {
                SpendSenseApp(
                    targetTransactionId = targetTransactionId,
                    onTransactionHandled = { targetTransactionId = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val id = intent?.getLongExtra("transaction_id", -1L) ?: -1L
        if (id != -1L) {
            targetTransactionId = id
        }
    }

    private fun scheduleWorkers() {
        val workManager = WorkManager.getInstance(applicationContext)

        // Untagged reminder — every 6 hours
        val untaggedWork = PeriodicWorkRequestBuilder<UntaggedReminderWorker>(
            6, TimeUnit.HOURS
        ).build()
        workManager.enqueueUniquePeriodicWork(
            UntaggedReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            untaggedWork
        )

        // Budget threshold check — every 2 hours
        val budgetWork = PeriodicWorkRequestBuilder<BudgetThresholdWorker>(
            2, TimeUnit.HOURS
        ).build()
        workManager.enqueueUniquePeriodicWork(
            BudgetThresholdWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            budgetWork
        )

        // Addiction/pattern check — daily
        val addictionWork = PeriodicWorkRequestBuilder<AddictionCheckWorker>(
            1, TimeUnit.DAYS
        ).build()
        workManager.enqueueUniquePeriodicWork(
            AddictionCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            addictionWork
        )
    }
}
