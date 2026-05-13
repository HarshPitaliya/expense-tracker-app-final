package com.expensetracker.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.models.Category
import com.expensetracker.data.models.Transaction
import com.expensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class TransactionViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Combine all relevant data flows: repository's data and the ViewModel's internal state (for filters/sort)
            combine(
                repository.transactions,
                repository.categories,
                repository.user,
                _uiState // Crucial to react to filter/sort changes from the UI
            ) { transactions, categories, user, currentState ->
                val availableMonths = generateAvailableMonths(user?.startDate)

                // Apply filters and sorting based on currentState (which holds the filter/sort criteria)
                var filteredList = transactions.toList()

                // Search filter
                if (currentState.searchQuery.isNotBlank()) {
                    // Use repository's search which works on _all_ transactions, then apply other filters
                    filteredList = repository.searchTransactions(currentState.searchQuery)
                }

                // Category filter
                currentState.selectedCategoryId?.let { categoryId ->
                    filteredList = filteredList.filter { it.categoryId == categoryId }
                }

                // Month/Year filter
                currentState.selectedMonthYear?.let { (month, year) ->
                    val calendar = Calendar.getInstance()
                    filteredList = filteredList.filter { transaction ->
                        calendar.time = transaction.date
                        val txMonth = calendar.get(Calendar.MONTH) + 1
                        val txYear = calendar.get(Calendar.YEAR)
                        txMonth == month && txYear == year
                    }
                }

                // Sort
                filteredList = if (currentState.sortByValue) {
                    filteredList.sortedByDescending { it.amount }
                } else {
                    filteredList.sortedByDescending { it.date }
                }

                // Return a new UiState with the filtered transactions and updated availableMonths/categories
                // but crucially, preserving the current filter/sort criteria from currentState
                currentState.copy(
                    transactions = filteredList,
                    categories = categories,
                    availableMonths = availableMonths,
                    // The rest of the state (searchQuery, sortByValue, etc.) is preserved implicitly
                    // by using currentState.copy() and then explicitly setting derived values
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun generateAvailableMonths(userStartDate: Date?): List<Pair<Int, Int>> {
        val monthSet = mutableListOf<Pair<Int, Int>>()
        val calendar = Calendar.getInstance()
        val today = Date()

        val userStart = userStartDate ?: Calendar.getInstance().apply {
            set(2025, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        calendar.time = today
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startCalendar = Calendar.getInstance()
        startCalendar.time = userStart
        startCalendar.set(Calendar.DAY_OF_MONTH, 1)
        startCalendar.set(Calendar.HOUR_OF_DAY, 0)
        startCalendar.set(Calendar.MINUTE, 0)
        startCalendar.set(Calendar.SECOND, 0)
        startCalendar.set(Calendar.MILLISECOND, 0)

        while (!calendar.before(startCalendar)) {
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            monthSet.add(Pair(month, year))
            calendar.add(Calendar.MONTH, -1)
        }
        return monthSet
    }

    // --- State Update Functions ---
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        // No explicit applyFiltersAndSort() needed, combine will react to _uiState change
    }

    fun toggleSortByValue() {
        _uiState.value = _uiState.value.copy(sortByValue = !_uiState.value.sortByValue)
    }

    fun updateSelectedCategory(categoryId: String?) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
    }

    fun updateSelectedMonthYear(monthYear: Pair<Int, Int>?) {
        _uiState.value = _uiState.value.copy(selectedMonthYear = monthYear)
    }

    fun hideTransactionSheets() {
        _uiState.value = _uiState.value.copy(showAddSheet = false, selectedTransaction = null)
    }

    fun selectTransactionForEdit(transaction: Transaction) {
        _uiState.value = _uiState.value.copy(selectedTransaction = transaction, showAddSheet = false)
    }

    // --- Repository interactions ---
    fun addTransaction(name: String, amount: Double, date: Date, categoryId: String?, isExpense: Boolean) {
        viewModelScope.launch {
            repository.addTransaction(
                Transaction(
                    smsSender = "MANUAL",
                    smsBody = "Manual entry",
                    derivedName = name,
                    amount = amount,
                    date = date,
                    categoryId = categoryId,
                    isExpense = isExpense,
                    isUndoAvailable = false,
                    originalData = null
                )
            )
            hideTransactionSheets()
        }
    }

    fun updateTransaction(transactionId: String, newName: String, isExpenseDesired: Boolean, newCategoryId: String?) {
        viewModelScope.launch {
            val tx = repository.transactions.value.find { it.id == transactionId }
            if (tx != null) {
                // Rename if changed
                if (newName != tx.getDisplayName()) {
                    repository.renameTransaction(tx.id, newName)
                }

                // Toggle if changed
                if (isExpenseDesired != tx.isExpense) {
                    repository.toggleTransactionExpenseStatus(tx.id)
                }

                // Category if changed
                if (newCategoryId != tx.categoryId) {
                    repository.changeCategoryForTransaction(tx.id, newCategoryId)
                }
            }
            hideTransactionSheets()
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
            hideTransactionSheets()
        }
    }
}

data class TransactionUiState(
    val transactions: List<Transaction> = emptyList(), // Filtered transactions
    val categories: List<Category> = emptyList(),
    val searchQuery: String = "",
    val sortByValue: Boolean = false, // false for date, true for value
    val selectedCategoryId: String? = null,
    val selectedMonthYear: Pair<Int, Int>? = null, // (month, year)
    val availableMonths: List<Pair<Int, Int>> = emptyList(),
    val showAddSheet: Boolean = false,
    val selectedTransaction: Transaction? = null
)

class TransactionViewModelFactory(
    private val repository: ExpenseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            return TransactionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}