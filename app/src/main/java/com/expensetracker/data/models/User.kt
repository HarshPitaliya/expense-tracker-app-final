package com.expensetracker.data.models

import java.util.*

data class User(
    val name: String? = "User",
    val startDate: Date? = null,
    val endDate: Date? = null,
    val lastParsedDate: Date? = null,
    val isFirstTimeUser: Boolean = true,
    val smsPermissionGranted: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    fun isSetupComplete(): Boolean {
        return !isFirstTimeUser && 
               startDate != null && 
               endDate != null && 
               smsPermissionGranted
    }

    fun shouldParseNewSMS(): Boolean {
        return isSetupComplete() && 
               endDate?.after(Date()) == true
    }
}