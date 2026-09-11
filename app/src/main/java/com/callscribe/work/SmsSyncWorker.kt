package com.callscribe.work

import android.Manifest
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.callscribe.capture.Contacts
import com.callscribe.capture.SmsImporter
import com.callscribe.data.AppDatabase
import com.callscribe.data.CaptureStatus
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

        val dao = AppDatabase.get(context).captureDao()

        // Захранва наново източниците, застинали по средата на обработката.
        runCatching {
            dao.pending().forEach { capture ->
                ProcessCaptureWorker.enqueue(context, capture.id)
            }
        }

        // След отваряне на приложението или ръчна проверка пробваме наново и това,
        // което е спряло с грешка — най-често защото ключът още не е бил въведен.
        if (inputData.getBoolean(KEY_RETRY_FAILED, false)) {
            runCatching {
                dao.failed().forEach { capture ->
                    dao.update(capture.copy(status = CaptureStatus.NEW, error = null))
                    ProcessCaptureWorker.enqueue(context, capture.id)
                }
            }
        }

        Result.success()
    }

    companion object {
        private const val KEY_RETRY_FAILED = "retry_failed"
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

        /**
         * Еднократно засичане веднага — при отваряне на приложението или ръчно.
         * За разлика от периодичния цикъл, тук се пробват наново и провалените
         * източници, защото точно тогава потребителят е оправил настройките.
         */
        fun syncNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<SmsSyncWorker>()
                .setInputData(Data.Builder().putBoolean(KEY_RETRY_FAILED, true).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_ONCE, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
