package com.expensetracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.expensetracker.data.repository.ExpenseRepository
import com.expensetracker.utils.SMSProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class SMSReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            messages?.forEach { smsMessage ->
                val sender = smsMessage.originatingAddress ?: ""
                val messageBody = smsMessage.messageBody ?: ""
                val receivedDate = Date(smsMessage.timestampMillis)
                Log.d("ExpenseTracker", "SMS from $sender: $messageBody")
                // Process SMS in background
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repository = ExpenseRepository.getInstance(context)
                        val processor = SMSProcessor(context, repository)
                        // Check if user has completed setup
                        val user = repository.getUser()
                        if (user?.shouldParseNewSMS() == true) {
                            val result = processor.processSingleSMS(sender, messageBody, receivedDate)
                            result.fold(
                                onSuccess = { transaction ->
                                    if (transaction != null) {
                                        Log.d("ExpenseTracker", "New transaction created: ${transaction.derivedName} - Rs.${transaction.amount}")
                                    }
                                },
                                onFailure = { error ->
                                    Log.e("ExpenseTracker", "Failed to process SMS: ${error.message}")
                                }
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("ExpenseTracker", "Error in SMS processing: ${e.message}")
                    }
                }
            }
        }
    }
}