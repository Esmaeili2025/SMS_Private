package com.example.service

import android.net.Uri
import android.os.Build
import android.telecom.CallRedirectionService
import android.telecom.PhoneAccountHandle
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.db.AppDatabase
import com.example.utils.SmsSender
import kotlinx.coroutines.runBlocking

@RequiresApi(Build.VERSION_CODES.Q)
class CallRedirectionServiceImpl : CallRedirectionService() {

    override fun onPlaceCall(
        handle: Uri,
        initialPhoneAccount: PhoneAccountHandle,
        allowInteractiveResponse: Boolean
    ) {
        // Retrieve characters dialed (e.g., tel:123 or tel:*123#)
        val scheme = handle.scheme
        val schemeSpecificPart = handle.schemeSpecificPart ?: ""
        
        Log.d("CallRedirectionService", "onPlaceCall triggered with scheme: $scheme, specific: $schemeSpecificPart")
        
        // Clean URL encoded characters if present (like %23 for #)
        val dialedNumber = Uri.decode(schemeSpecificPart).trim()
        
        if (dialedNumber.isNotEmpty()) {
            val db = AppDatabase.getDatabase(this)
            val matchedShortcut = runBlocking {
                db.shortcutDao().getShortcutByCode(dialedNumber)
            }
            
            if (matchedShortcut != null) {
                Log.d("CallRedirectionService", "Match found for shortcut: $dialedNumber. Cancelling call and sending SMS.")
                
                // Abort the ongoing system outgoing call request
                cancelCall()
                
                // Dispatch SMS immediately
                SmsSender.sendSms(this, matchedShortcut.phoneNumber, matchedShortcut.message)
                return
            }
        }
        
        // Fallback: Let standard phone numbers and call paths dial through without modification
        placeCallUnmodified()
    }
}
