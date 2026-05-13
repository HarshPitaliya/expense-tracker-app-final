package com.expensetracker.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.expensetracker.data.ml.SMSClassifier
import com.expensetracker.data.models.Transaction
import com.expensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class SMSProcessor(
    private val context: Context,
    private val repository: ExpenseRepository
) {
    private val classifier = SMSClassifier()

    suspend fun processHistoricalSMS(
        startDate: Date,
        endDate: Date,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<List<Transaction>> = withContext(Dispatchers.IO) {
        try {
            if (!hasSMSPermission()) {
                return@withContext Result.failure(Exception("SMS permission not granted"))
            }

            val transactions = mutableListOf<Transaction>()
            val smsMessages = getSMSMessages(startDate, endDate)

            val existingTransactions = repository.getAllTransactionsForCategorization()

            smsMessages.forEachIndexed { index, smsData ->

                if (classifier.isTransactionSMS(smsData.sender, smsData.body)) {
                    val transactionData = classifier.parseTransactionDetails(smsData.body)
                    transactionData?.let { data ->
                        val suggestedCategory = classifier.suggestCategory(
                            data.merchantName,
                            existingTransactions
                        )
                        val transaction = Transaction(
                            smsSender = smsData.sender,
                            smsBody = smsData.body,
                            derivedName = data.merchantName,
                            amount = data.amount,
                            date = smsData.date.toIST(),
                            categoryId = suggestedCategory,
                            isExpense = data.isDebit
                        )
                        repository.addTransaction(transaction)
                        transactions.add(transaction)
                    }
                } else {
                    println("DEBUG: Failed to parse transaction details for SMS from ${smsData.sender}: '${smsData.body}'")
                }
                onProgress(index + 1, smsMessages.size)
            }

            repository.updateLastParsedDate(endDate)
            return@withContext Result.success(transactions)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure(e)
        }
    }


    suspend fun processNewSMS(lastParsedDate: Date): Result<List<Transaction>> =
        withContext(Dispatchers.IO) {
            try {
                val currentDate = Date()
                return@withContext processHistoricalSMS(lastParsedDate, currentDate)
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }

    suspend fun processSingleSMS(
        sender: String,
        messageBody: String,
        receivedDate: Date = Date()
    ): Result<Transaction?> = withContext(Dispatchers.IO) {
        try {
            if (classifier.isTransactionSMS(sender, messageBody)) {
                val transactionData = classifier.parseTransactionDetails(messageBody)
                transactionData?.let { data ->
                    val existingTransactions = repository.getAllTransactionsForCategorization()
                    val suggestedCategory = classifier.suggestCategory(
                        data.merchantName,
                        existingTransactions
                    )
                    val transaction = Transaction(
                        smsSender = sender,
                        smsBody = messageBody,
                        derivedName = data.merchantName,
                        amount = data.amount,
                        date = receivedDate.toIST(),
                        categoryId = suggestedCategory,
                        isExpense = data.isDebit
                    )
                    repository.addTransaction(transaction)
                    return@withContext Result.success(transaction)
                }
            }
            return@withContext Result.success(null)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    fun hasSMSPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun getSMSMessages(startDate: Date, endDate: Date): List<SMSData> =
        withContext(Dispatchers.IO) {
            val messages = mutableListOf<SMSData>()
            try {
                val uri = "content://sms/inbox".toUri()
                val selection = "date >= ? AND date <= ?"
                val selectionArgs = arrayOf(
                    startDate.time.toString(),
                    endDate.time.toString()
                )
                val cursor: Cursor? = context.contentResolver.query(
                    uri,
                    arrayOf("address", "body", "date"),
                    selection,
                    selectionArgs,
                    "date DESC"
                )
                cursor?.use {
                    val addressIndex = it.getColumnIndex("address")
                    val bodyIndex = it.getColumnIndex("body")
                    val dateIndex = it.getColumnIndex("date")
                    while (it.moveToNext()) {
                        val address = it.getString(addressIndex) ?: ""
                        val body = it.getString(bodyIndex) ?: ""
                        val dateMillis = it.getLong(dateIndex)
                        messages.add(
                            SMSData(
                                sender = address,
                                body = body,
                                date = Date(dateMillis)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext messages
        }

    suspend fun validateSMSAccess(): SMSValidationResult = withContext(Dispatchers.IO) {
        try {
            if (!hasSMSPermission()) {
                return@withContext SMSValidationResult.PermissionDenied
            }

            val testMessages = getSMSMessages(
                Date(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L), // last 30 days
                Date()
            )

            if (testMessages.isEmpty()) {
                return@withContext SMSValidationResult.NoMessages
            }

            val transactionMessages = testMessages.count { smsData ->
                classifier.isTransactionSMS(smsData.sender, smsData.body)
            }

            return@withContext SMSValidationResult.Success(
                totalMessages = testMessages.size,
                transactionMessages = transactionMessages
            )
        } catch (e: Exception) {
            return@withContext SMSValidationResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getSMSStatistics(days: Int = 30): SMSStatistics = withContext(Dispatchers.IO) {
        try {
            val endDate = Date()
            val startDate = Date(endDate.time - days * 24 * 60 * 60 * 1000L)
            val messages = getSMSMessages(startDate, endDate)
            val transactionMessages = messages.filter { smsData ->
                classifier.isTransactionSMS(smsData.sender, smsData.body)
            }

            val senderCounts = messages.groupBy { it.sender }.mapValues { it.value.size }
            val transactionSenderCounts =
                transactionMessages.groupBy { it.sender }.mapValues { it.value.size }

            return@withContext SMSStatistics(
                totalMessages = messages.size,
                transactionMessages = transactionMessages.size,
                uniqueSenders = senderCounts.size,
                topSenders = senderCounts.toList().sortedByDescending { it.second }.take(10),
                topTransactionSenders = transactionSenderCounts.toList()
                    .sortedByDescending { it.second }.take(10),
                dateRange = Pair(startDate, endDate)
            )
        } catch (e: Exception) {
            return@withContext SMSStatistics(
                totalMessages = 0,
                transactionMessages = 0,
                uniqueSenders = 0,
                topSenders = emptyList(),
                topTransactionSenders = emptyList(),
                dateRange = Pair(Date(), Date()),
                error = e.message
            )
        }
    }

    data class SMSData(
        val sender: String,
        val body: String,
        val date: Date
    )

    sealed class SMSValidationResult {
        object PermissionDenied : SMSValidationResult()
        object NoMessages : SMSValidationResult()
        data class Success(val totalMessages: Int, val transactionMessages: Int) :
            SMSValidationResult()
        data class Error(val message: String) : SMSValidationResult()
    }

    data class SMSStatistics(
        val totalMessages: Int,
        val transactionMessages: Int,
        val uniqueSenders: Int,
        val topSenders: List<Pair<String, Int>>,
        val topTransactionSenders: List<Pair<String, Int>>,
        val dateRange: Pair<Date, Date>,
        val error: String? = null
    )

    fun Date.toIST(): Date {
        val istTimeZone = TimeZone.getTimeZone("Asia/Kolkata")
        val calendar = Calendar.getInstance(istTimeZone)
        calendar.time = this
        return calendar.time
    }
}
