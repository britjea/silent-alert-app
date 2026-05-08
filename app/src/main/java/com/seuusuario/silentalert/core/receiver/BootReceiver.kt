package com.seuusuario.silentalert.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.seuusuario.silentalert.core.worker.AlertDispatchWorker

/**
 * Re-enqueues any pending alert work that was cancelled when the device was
 * powered off. WorkManager normally handles this itself for persisted work,
 * but an explicit receiver guarantees it on devices with aggressive battery
 * management (e.g. some Xiaomi / Huawei ROMs).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val wm = WorkManager.getInstance(context)
        wm.getWorkInfosByTag(AlertDispatchWorker.TAG).get()
            ?.filter { it.state.isFinished.not() }
            ?.ifEmpty { null }
            ?: wm.enqueue(AlertDispatchWorker.buildRequest())
    }
}
