package com.expensetracker.ui.setup

import android.app.Application // Import Application
import androidx.lifecycle.AndroidViewModel // Change ViewModel to AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.ExpenseRepository
import com.expensetracker.utils.SMSProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

val END_DATE: Date = Calendar
    .getInstance()
    .apply { set(2099, Calendar.DECEMBER, 31, 23, 59, 59); set(Calendar.MILLISECOND, 999) }.time

// Change to AndroidViewModel and accept Application in the constructor
class InitialSetupViewModel(
    private val repository: ExpenseRepository,
    application: Application // Changed type from Context to Application
) : AndroidViewModel(application) { // Pass application to the super constructor

    private val _uiState = MutableStateFlow(InitialSetupUiState())
    val uiState: StateFlow<InitialSetupUiState> = _uiState.asStateFlow()

    // Use getApplication() (provided by AndroidViewModel) to access the Application context
    private val smsProcessor = SMSProcessor(getApplication(), repository)

    init {
        // Set default start/end dates to Jan 1, 2025 - Dec 31, 2099
        val startCalendar = Calendar.getInstance().apply {
            set(2025, Calendar.JANUARY, 1)
        }

        _uiState.value = _uiState.value.copy(
            startDate = startCalendar.time,
            endDate = END_DATE
        )
    }

    fun updatePermissionGranted(granted: Boolean) {
        _uiState.value = _uiState.value.copy(
            smsPermissionGranted = granted,
            canCompleteSetup = granted && isValidDateRange() && isValidName()
        )
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(
            name = name,
            canCompleteSetup = _uiState.value.smsPermissionGranted &&
                    isValidDateRange() && isValidName(name)
        )
    }

    fun updateStartDate(date: Date) {
        _uiState.value = _uiState.value.copy(
            startDate = date,
            canCompleteSetup = _uiState.value.smsPermissionGranted &&
                    isValidDateRange(date, _uiState.value.endDate) && isValidName()
        )
    }

    private fun isValidName(name: String = _uiState.value.name): Boolean {
        return name.isNotBlank() && name.length <= 50
    }

    private fun isValidDateRange(
        startDate: Date = _uiState.value.startDate,
        endDate: Date = _uiState.value.endDate
    ): Boolean {
        val isValidRange = startDate.before(endDate)
        val minDate = Calendar.getInstance().apply {
            set(2024, Calendar.DECEMBER, 31)
        }.time
        val maxDate = END_DATE
        val isInBounds = !startDate.before(minDate) && !endDate.after(maxDate)
        return isValidRange && isInBounds
    }

    fun completeSetup(onComplete: () -> Unit) {
        val currentState = _uiState.value
        if (!currentState.canCompleteSetup) {
            _uiState.value = currentState.copy(
                errorMessage = "Please complete all setup steps"
            )
            return
        }

        _uiState.value = currentState.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                // Save user setup info
                repository.completeUserSetup(
                    name = currentState.name.takeIf { it.isNotBlank() },
                    startDate = currentState.startDate,
                    endDate = currentState.endDate
                )

                val endDateForProcessing = minOf(END_DATE, Date())

                _uiState.value = _uiState.value.copy(
                    processingMessage = "Parsing historical SMS messages..."
                )

                smsProcessor.processHistoricalSMS(
                    startDate = currentState.startDate,
                    endDate = endDateForProcessing
                ) { processed, total ->
                    _uiState.value = _uiState.value.copy(
                        processingProgress = if (total > 0)
                            processed.toFloat() / total else 0f,
                        processingMessage = "Processing SMS $processed of $total..."
                    )
                }.fold(
                    onSuccess = { transactions ->
                        // ✅ Use latest state, not stale currentState
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            processingProgress = 1f,
                            processingMessage = "Setup completed! Found ${transactions.size} transactions."
                        )
                        onComplete()
                    },
                    onFailure = { error ->
                        // ✅ Use latest state, not stale currentState
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Setup failed: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Setup failed: ${e.message}"
                )
            }
        }
    }

    fun validateSMSAccess() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isValidatingSMS = true)
            try {
                when (val result = smsProcessor.validateSMSAccess()) {
                    is SMSProcessor.SMSValidationResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isValidatingSMS = false,
                            smsValidationMessage = "Found ${result.totalMessages} messages, ${result.transactionMessages} appear to be transactions"
                        )
                    }
                    is SMSProcessor.SMSValidationResult.NoMessages -> {
                        _uiState.value = _uiState.value.copy(
                            isValidatingSMS = false,
                            smsValidationMessage = "No SMS messages found in the last 30 days"
                        )
                    }
                    is SMSProcessor.SMSValidationResult.PermissionDenied -> {
                        _uiState.value = _uiState.value.copy(
                            isValidatingSMS = false,
                            smsValidationMessage = "SMS permission is required",
                            smsPermissionGranted = false,
                            canCompleteSetup = false
                        )
                    }
                    is SMSProcessor.SMSValidationResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isValidatingSMS = false,
                            smsValidationMessage = "Error validating SMS: ${result.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isValidatingSMS = false,
                    smsValidationMessage = "Error: ${e.message}"
                )
            }
        }
    }
}

data class InitialSetupUiState(
    val name: String = "",
    val smsPermissionGranted: Boolean = false,
    val startDate: Date = Date(),
    val endDate: Date = END_DATE,
    val isLoading: Boolean = false,
    val isValidatingSMS: Boolean = false,
    val processingProgress: Float = 0f,
    val processingMessage: String = "",
    val canCompleteSetup: Boolean = false,
    val errorMessage: String? = null,
    val smsValidationMessage: String? = null
)