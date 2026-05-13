package com.expensetracker.data.models

import java.util.UUID

data class Budget(
    val id: String = UUID.randomUUID().toString(),
    val month: Int, // 1-12
    val year: Int,
    val totalBudget: Double = 200_000.0 // Default ₹2 lakh
)