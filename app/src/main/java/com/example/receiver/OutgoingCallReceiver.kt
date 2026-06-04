package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.db.AppDatabase
import com.example.utils.SmsSender
import kotlinx.coroutines.runBlocking

class OutgoingCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_NEW_OUTGOING_CALL) {
            val rawNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER) ?: return
            Log.d("OutgoingCallReceiver", "ACTION_NEW_OUTGOING_CALL received with number: $rawNumber")
            
            val db = AppDatabase.getDatabase(context)
            val matchedShortcut = runBlocking {
                db.shortcutDao().getShortcutByCode(rawNumber.trim())
            }
            
            if (matchedShortcut != null) {
                // Cancel the outgoing call
                resultData = null
                
                // Dispatch SMS immediately
                SmsSender.sendSms(context, matchedShortcut.phoneNumber, matchedShortcut.message)
                Log.d("OutgoingCallReceiver", "Outgoing call cancelled and SMS triggered to ${matchedShortcut.phoneNumber}")
            }
        }
    }
}
