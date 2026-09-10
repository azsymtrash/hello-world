package com.callscribe.work

import android.Manifest
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.callscribe.capture.Contacts
import com.callscribe.capture.SmsImporter
import com.callscribe.data.AppDatabase
import com.callscribe.data.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Периодично засича нови съобщения сам, без потребителят да натиска бутон,
 * и пуска наново всичко, което е останало необработено (например защото
 * телефонът е бил без мрежа).
 */
class SmsSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        val settings = Settings(context)

        if (settings.consentAccepted && settings.readSms &&
            Contacts.hasPermission(context, Manifest.permission.READ_SMS)
        ) {
            // При първо пускане поглеждаме един ден назад, после само от последното сканиране.
            val since = settings.lastSmsImportAt.takeIf { it > 0L }
                ?: (System.currentTimeMillis() - DEFAULT_LOOKBACK_MS)
            val startedAt = System.currentTimeMillis()
            runCatching { SmsImporter.importSince(context, since) }
                .onSuccess { settings.lastSmsImportAt = startedAt }
        }

        // Захранва наново източниците, застинали заради грешка или липсваща мрежа.
        runCatching {
            AppDatabase.get(context).captureDao().pending().forEach { capture ->
                ProcessCaptureWorker.enqueue(context, capture.id)
            }
        }

        Result.success()
    }

    companion object {
        private const val UNIQUE_PERIODIC = "sms-sync-periodic"
        private const val UNIQUE_ONCE = "sms-sync-once"
        private const val DEFAULT_LOOKBACK_MS = 24L * 60 * 60 * 1000

        /** Пуска фоновия цикъл. WorkManager не позволява интервал под 15 минути. */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SmsSyncWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancelPeriodic(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC)
        }

        /** Еднократно засичане веднага — при отваряне на приложението или ръчно. */
        fun syncNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<SmsSyncWorker>().build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_ONCE, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
