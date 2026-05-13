package com.expensetracker.ui.overview

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.models.Category
import com.expensetracker.data.models.Transaction
import com.expensetracker.data.repository.ExpenseRepository
import com.expensetracker.utils.SMSProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class OverviewViewModel(
    private val repository: ExpenseRepository,
    application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverviewUiState())
    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()

    private val smsProcessor = SMSProcessor(application.applicationContext, repository)
    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val topItemsLimit: Int = 3 // Used to display only this many items in top categories and transactions.

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                repository.transactions,
                repository.user,
                repository.budgets,
                repository.categories
            ) { transactions, user, budgets, categories ->
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH) + 1
                val currentYear = calendar.get(Calendar.YEAR)

                // ✅ FIXED: Use reactive data from StateFlow parameters
                val currentMonthTransactions = getTransactionsForMonth(transactions, currentMonth, currentYear)
                val currentMonthExpenses = currentMonthTransactions.filter { it.isExpense }
                val totalExpenses = currentMonthExpenses.sumOf { it.amount }

                val budget = budgets.find { it.month == currentMonth && it.year == currentYear }?.totalBudget ?: 200_000.0
                val safeToSpend = calculateSafeToSpend(totalExpenses, budget)

                val topCategories = getTopCategories(currentMonthExpenses, categories)
                val topTransactions = currentMonthExpenses.sortedByDescending { it.amount }.take(topItemsLimit)

                val budgetPercentage = if (budget > 0) {
                    ((totalExpenses / budget) * 100).toInt()
                } else 0

                val lastUpdateMessage = if (user?.lastParsedDate != null) {
                    "Last updated: ${dateFormatter.format(user.lastParsedDate)}"
                } else {
                    "No updates yet"
                }

                OverviewUiState(
                    totalExpenses = totalExpenses,
                    totalBudget = budget,
                    safeToSpend = safeToSpend,
                    budgetPercentage = budgetPercentage,
                    topCategories = topCategories,
                    topTransactions = topTransactions,
                    lastUpdateMessage = lastUpdateMessage
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun getTransactionsForMonth(transactions: List<Transaction>, month: Int, year: Int): List<Transaction> {
        val calendar = Calendar.getInstance()
        return transactions.filter { transaction ->
            calendar.time = transaction.date
            calendar.get(Calendar.MONTH) + 1 == month &&
                    calendar.get(Calendar.YEAR) == year
        }
    }

    private fun getTopCategories(
        expenses: List<Transaction>,
        categories: List<Category>
    ): List<Pair<Category, Double>> {
        val categoryExpenses = mutableMapOf<String, Double>()
        expenses.forEach { transaction ->
            val categoryId = transaction.categoryId ?: "undefined"
            categoryExpenses[categoryId] = (categoryExpenses[categoryId] ?: 0.0) + transaction.amount
        }
        return categoryExpenses.entries
            .sortedByDescending { it.value }
            .take(topItemsLimit)
            .mapNotNull { entry ->
                categories.find { it.id == entry.key }?.let { category ->
                    Pair(category, entry.value)
                }
            }
    }

    private fun calculateSafeToSpend(totalExpenses: Double, budget: Double): Double {
        val calendar = Calendar.getInstance()
        val remainingBudget = budget - totalExpenses
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val remainingDays = daysInMonth - currentDay + 1

        return if (remainingDays > 0) remainingBudget / remainingDays else 0.0
    }

    fun updateTransactions() {
        _uiState.value = _uiState.value.copy(isUpdating = true)
        viewModelScope.launch {
            try {
                val user = repository.user.value
                val lastParsedDate = user?.lastParsedDate ?: user?.startDate ?: Date()

                smsProcessor.processNewSMS(lastParsedDate).fold(
                    onSuccess = { transactions ->
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            lastUpdateMessage = "Updated: ${transactions.size} new transactions"
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            lastUpdateMessage = "Update failed: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    lastUpdateMessage = "Update failed: ${e.message}"
                )
            }
        }
    }
}

data class OverviewUiState(
    val totalExpenses: Double = 0.0,
    val totalBudget: Double = 500000.0,
    val safeToSpend: Double = 0.0,
    val budgetPercentage: Int = 0,
    val topCategories: List<Pair<Category, Double>> = emptyList(),
    val topTransactions: List<Transaction> = emptyList(),
    val lastUpdateMessage: String = "",
    val isUpdating: Boolean = false
)
