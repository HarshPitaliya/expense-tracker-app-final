package com.expensetracker.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.models.Category
import com.expensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AnalyticsViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Combine relevant flows to update UI state reactively
            combine(
                repository.categories,
                repository.user,
                repository.transactions, // React to any data changes
                _uiState // Observe internal UI state for month/year selection
            ) { categories, user, _, currentUiState ->
                val calendar = Calendar.getInstance()
                val today = Date()

                val userStartDate = user?.startDate ?: Calendar.getInstance().apply {
                    set(2025, Calendar.JANUARY, 1, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                // Generate historical data for the bar chart (last 6 months)
                val historicalExpenses = mutableListOf<Float>()
                val historicalBudgets = mutableListOf<Float>()
                val historicalMonthLabels = mutableListOf<String>()
                val tempCalendarForHistory = Calendar.getInstance()
                tempCalendarForHistory.add(Calendar.MONTH, -5)
                repeat(6) {
                    val month = tempCalendarForHistory.get(Calendar.MONTH) + 1
                    val year = tempCalendarForHistory.get(Calendar.YEAR)
                    historicalExpenses.add(repository.getTotalExpenseForMonth(month, year).toFloat())
                    historicalBudgets.add(repository.getBudgetForMonth(month, year).totalBudget.toFloat())
                    historicalMonthLabels.add(formatMonthYearShort(month, year))
                    tempCalendarForHistory.add(Calendar.MONTH, 1)
                }

                // Determine the selected month and year
                val (finalSelectedMonth, finalSelectedYear) = if (currentUiState.selectedMonth == null || currentUiState.selectedYear == null) {
                    // Initial load: pick the latest month from historical data or current month
                    val lastHistoricalMonthYear = historicalMonthLabels.lastOrNull()
                    if (lastHistoricalMonthYear != null) {
                        val parts = lastHistoricalMonthYear.split(" ")
                        val monthIndex = DateFormatSymbols.getInstance(Locale.ENGLISH).shortMonths.indexOf(parts[0])
                        val year = ("20" + parts[1]).toInt()
                        Pair(monthIndex + 1, year)
                    } else {
                        Pair(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
                    }
                } else {
                    Pair(currentUiState.selectedMonth, currentUiState.selectedYear)
                }

                // Get analytics for the selected month
                val selectedMonthAnalytics = repository.getHistoricalMonthDetails(finalSelectedMonth, finalSelectedYear)

                // --- Calculate At a Glance Metrics ---
                val currentExpenses = selectedMonthAnalytics.totalExpenses
                val (prevMonth, prevYear) = if (finalSelectedMonth == 1) Pair(12, finalSelectedYear - 1) else Pair(finalSelectedMonth - 1, finalSelectedYear)
                val previousExpenses = repository.getTotalExpenseForMonth(prevMonth, prevYear)

                val spendingTrend = if (previousExpenses > 0) {
                    val change = ((currentExpenses - previousExpenses) / previousExpenses * 100)
                    val changeInt = change.toInt()
                    when {
                        changeInt > 0 -> "↗️ +$changeInt% vs last month"
                        changeInt < 0 -> "↘️ $changeInt% vs last month"
                        else -> "➡️ No change vs last month"
                    }
                } else if (currentExpenses > 0) {
                    "↗️ New spending this month"
                } else {
                    "No data available"
                }

                val budget = selectedMonthAnalytics.totalBudget
                val budgetUsed = if (budget > 0) ((currentExpenses / budget) * 100).toInt() else 0
                val budgetStatus = "$budgetUsed% used"

                val currentCalendar = Calendar.getInstance().apply { time = today }
                val isCurrentMonth = finalSelectedMonth == currentCalendar.get(Calendar.MONTH) + 1 && finalSelectedYear == currentCalendar.get(Calendar.YEAR)
                val daysLeft = if (isCurrentMonth) {
                    val daysInMonth = Calendar.getInstance().apply { set(finalSelectedYear, finalSelectedMonth - 1, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)
                    daysInMonth - currentCalendar.get(Calendar.DAY_OF_MONTH)
                } else {
                    0
                }

                // --- Update UI State ---
                currentUiState.copy(
                    selectedMonth = finalSelectedMonth,
                    selectedYear = finalSelectedYear,
                    selectedMonthAnalytics = selectedMonthAnalytics,
                    historicalExpensesData = historicalExpenses,
                    historicalBudgetsData = historicalBudgets,
                    historicalMonthLabels = historicalMonthLabels,
                    categories = categories,
                    userStartDate = userStartDate,
                    currentDate = today,
                    // Populate the new state fields
                    spendingTrend = spendingTrend,
                    budgetStatus = budgetStatus,
                    daysLeft = daysLeft,
                    isCurrentMonth = isCurrentMonth
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun updateSelectedMonthYear(newMonth: Int, newYear: Int) {
        _uiState.value = _uiState.value.copy(
            selectedMonth = newMonth,
            selectedYear = newYear
        )
    }

    fun canNavigateBack(selectedMonth: Int, selectedYear: Int): Boolean {
        val userStartDate = _uiState.value.userStartDate ?: return true
        val calendar = Calendar.getInstance().apply { time = userStartDate }
        val startMonth = calendar.get(Calendar.MONTH) + 1
        val startYear = calendar.get(Calendar.YEAR)
        return selectedYear > startYear || (selectedYear == startYear && selectedMonth > startMonth)
    }

    fun canNavigateForward(selectedMonth: Int, selectedYear: Int): Boolean {
        val currentDate = _uiState.value.currentDate
        val calendar = Calendar.getInstance().apply { time = currentDate }
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)
        return selectedYear < currentYear || (selectedYear == currentYear && selectedMonth < currentMonth)
    }

    private fun formatMonthYearShort(month: Int, year: Int): String {
        val monthName = DateFormatSymbols.getInstance(Locale.ENGLISH).months[month - 1]
        return "${monthName.take(3)} ${year % 100}"
    }
}

data class AnalyticsUiState(
    val selectedMonth: Int? = null,
    val selectedYear: Int? = null,
    val selectedMonthAnalytics: ExpenseRepository.MonthlyAnalytics? = null,
    val historicalExpensesData: List<Float> = emptyList(),
    val historicalBudgetsData: List<Float> = emptyList(),
    val historicalMonthLabels: List<String> = emptyList(),
    val categories: List<Category> = emptyList(),
    val userStartDate: Date? = null,
    val currentDate: Date = Date(),
    // New fields to hold calculated data for the UI
    val spendingTrend: String = "",
    val budgetStatus: String = "",
    val daysLeft: Int = 0,
    val isCurrentMonth: Boolean = false
)

class AnalyticsViewModelFactory(
    private val repository: ExpenseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
            return AnalyticsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}