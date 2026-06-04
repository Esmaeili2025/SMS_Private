package com.example.utils

import android.content.Context
import android.telephony.SmsManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SmsSender {
    fun sendSms(context: Context, phoneNumber: String, message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                
                // Divide the SMS message if it exceeds the standard limit
                val parts = smsManager.divideMessage(message)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                }
                
                Toast.makeText(
                    context, 
                    "پیامک با موفقیت فعال و به شماره $phoneNumber ارسال شد.", 
                    Toast.LENGTH_LONG
                ).show()
                Log.d("SmsSender", "SMS sent successfully to $phoneNumber: $message")
            } catch (e: Exception) {
                Toast.makeText(
                    context, 
                    "خطا در ارسال پیامک خودکار: ${e.localizedMessage}\nلطفاً دسترسی SMS را بررسی کنید.", 
                    Toast.LENGTH_LONG
                ).show()
                Log.e("SmsSender", "Failed to send SMS to $phoneNumber", e)
            }
        }
    }
}
