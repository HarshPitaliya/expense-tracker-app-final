package com.expensetracker.data.models

import java.util.*

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val smsSender: String,
    val smsBody: String,
    val derivedName: String,
    val amount: Double,
    val date: Date,
    val userOverwrittenName: String? = null,
    val categoryId: String? = null,
    val isExpense: Boolean = true,
    val isUndoAvailable: Boolean = true,
    val originalData: TransactionOriginalData? = null
) {
    fun getDisplayName(): String = userOverwrittenName ?: derivedName
}

data class TransactionOriginalData(
    val originalName: String,
    val originalCategoryId: String?,
    val originalIsExpense: Boolean
)