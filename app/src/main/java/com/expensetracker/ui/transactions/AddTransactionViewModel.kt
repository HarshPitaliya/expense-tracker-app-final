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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.UUID

// Common date bounds for transaction entry screens
val TRANSACTION_MIN_DATE: Date = Calendar.getInstance().apply {
    set(2025, Calendar.JANUARY, 1, 0, 0, 0)
    set(Calendar.MILLISECOND, 0)
}.time
val TRANSACTION_MAX_DATE: Date = Calendar.getInstance().apply {
    time = Date()
    set(Calendar.HOUR_OF_DAY, 23)
    set(Calendar.MINUTE, 59)
    set(Calendar.SECOND, 59)
    set(Calendar.MILLISECOND, 999)
}.time

class AddTransactionViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.categories.collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun updateName(name: String) {
        _uiState.update { currentState ->
            val isNameValid = name.trim().isNotEmpty() && name.length <= 40
            currentState.copy(
                name = name,
                isNameValid = isNameValid,
                nameError = if (isNameValid || name.isBlank()) null else "Name must be 1-40 characters."
            )
        }
    }

    fun updateAmountText(input: String) {
        _uiState.update { currentState ->
            val newAmountText = if (input.matches(Regex("^\\d{0,9}(\\.\\d{0,2})?$"))) input else currentState.amountText
            val amount = newAmountText.toDoubleOrNull()
            val isAmountValid = amount != null && amount > 0.0
            currentState.copy(
                amountText = newAmountText,
                isAmountValid = isAmountValid,
                amountError = if (isAmountValid || newAmountText.isBlank()) null else "Enter a valid amount > 0."
            )
        }
    }

    fun updateIsExpense(isExpense: Boolean) {
        _uiState.update { it.copy(isExpense = isExpense) }
    }

    fun updateSelectedCategoryId(categoryId: String?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun updateDate(date: Date) {
        _uiState.update { currentState ->
            val isDateValid = date.time in TRANSACTION_MIN_DATE.time..TRANSACTION_MAX_DATE.time
            currentState.copy(
                date = date,
                isDateValid = isDateValid,
                dateError = if (isDateValid) null else "Date must be between ${android.text.format.DateFormat.format("dd MMM yyyy", TRANSACTION_MIN_DATE)} and ${android.text.format.DateFormat.format("dd MMM yyyy", TRANSACTION_MAX_DATE)}"
            )
        }
    }

    fun showDatePicker(show: Boolean) {
        _uiState.update { it.copy(showDatePicker = show) }
    }

    fun saveTransaction(onSaveComplete: () -> Unit) {
        val currentState = _uiState.value
        if (!currentState.isFormValid) {
            // This should ideally not be reachable if button is disabled, but for safety
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val newTransaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    smsSender = "MANUAL",
                    smsBody = "Manually added transaction",
                    derivedName = currentState.name.trim(),
                    amount = currentState.amountText.toDouble(),
                    date = currentState.date,
                    categoryId = currentState.selectedCategoryId,
                    isExpense = currentState.isExpense,
                    isUndoAvailable = false,
                    originalData = null
                )
                repository.addTransaction(newTransaction)
                _uiState.update { it.copy(isSaving = false) }
                onSaveComplete()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Failed to save transaction: ${e.message}"
                    )
                }
            }
        }
    }
}

data class AddTransactionUiState(
    val name: String = "",
    val amountText: String = "",
    val isExpense: Boolean = true,
    val selectedCategoryId: String? = null,
    val date: Date = Date(), // Default to current date
    val categories: List<Category> = emptyList(),

    val showDatePicker: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,

    // Validation states
    val isNameValid: Boolean = false,
    val amountError: String? = null,
    val isAmountValid: Boolean = false,
    val nameError: String? = null,
    val isDateValid: Boolean = true, // Initially true for current date
    val dateError: String? = null,
) {
    val isFormValid: Boolean
        get() = isNameValid && isAmountValid && isDateValid && !isSaving
}

class AddTransactionViewModelFactory(
    private val repository: ExpenseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddTransactionViewModel::class.java)) {
            return AddTransactionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

