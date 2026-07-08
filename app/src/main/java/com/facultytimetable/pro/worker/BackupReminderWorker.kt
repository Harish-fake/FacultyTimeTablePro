package com.facultytimetable.pro.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.facultytimetable.pro.data.local.datastore.AppPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BackupReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val appPreferences: AppPreferences
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("BackupReminder", "Checking if backup reminder is needed...")
        return Result.success()
    }
}
