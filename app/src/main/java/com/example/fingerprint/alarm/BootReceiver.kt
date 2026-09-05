package com.example.fingerprint.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.fingerprint.auth.AuthSessionManager
import com.example.fingerprint.data.local.FingerprintDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val sessionManager = AuthSessionManager(context)
            if (!sessionManager.isLoggedIn()) {
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                val db = FingerprintDatabase.getInstance(context)
                val entities = db.fingerprintDao().getAllEvents().first()
                val events = entities.map { it.toDomain() }
                AlarmScheduler.scheduleAllAlarms(context, events)
            }
        }
    }
}
