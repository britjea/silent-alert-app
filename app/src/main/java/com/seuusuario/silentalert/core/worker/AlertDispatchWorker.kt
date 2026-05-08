package com.seuusuario.silentalert.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.seuusuario.silentalert.domain.usecase.TriggerSilentAlertUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs the alert dispatch off the main thread and survives process death.
 * Enqueue via [AlertDispatchWorker.buildRequest] so WorkManager retries on
 * network failure before giving up.
 */
@HiltWorker
class AlertDispatchWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val triggerAlert: TriggerSilentAlertUseCase
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return triggerAlert()
            .fold(
                onSuccess = { Result.success() },
                onFailure = {
                    if (runAttemptCount < MAX_RETRIES) Result.retry()
                    else Result.failure()
                }
            )
    }

    companion object {
        const val TAG = "AlertDispatchWorker"
        private const val MAX_RETRIES = 3

        fun buildRequest(): androidx.work.OneTimeWorkRequest =
            androidx.work.OneTimeWorkRequestBuilder<AlertDispatchWorker>()
                .addTag(TAG)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()
    }
}
