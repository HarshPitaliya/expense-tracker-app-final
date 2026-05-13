package com.expensetracker.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.models.User
import com.expensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.user.collect { user ->
                _uiState.update { currentState ->
                    val newEditableName = user?.name ?: ""
                    // If not in edit mode, always sync the name from the repository.
                    // If in edit mode, preserve the user's current input.
                    val shouldSync = !currentState.isEditMode
                    currentState.copy(
                        user = user,
                        editableName = if (shouldSync) newEditableName else currentState.editableName,
                        originalName = newEditableName,
                        nameError = if (shouldSync) null else currentState.nameError // Clear error on sync
                    )
                }
            }
        }
    }

    fun updateEditableName(newName: String) {
        _uiState.update { currentState ->
            val error = if (newName.trim().isBlank()) {
                "Name cannot be blank"
            } else if (newName.length > 50) {
                "Name cannot exceed 50 characters"
            } else {
                null
            }
            currentState.copy(
                editableName = newName,
                nameError = error,
                nameSaveSuccess = false // Reset success on new input
            )
        }
    }

    fun saveUserName() {
        val currentState = _uiState.value
        val trimmedName = currentState.editableName.trim()

        if (trimmedName == currentState.originalName) {
            _uiState.update { it.copy(nameError = "No changes to save.", isEditMode = false) }
            return
        }

        if (currentState.nameError != null) return

        _uiState.update { it.copy(isSavingName = true, nameError = null) }
        viewModelScope.launch {
            try {
                val success = repository.updateUserName(trimmedName)
                _uiState.update {
                    it.copy(
                        isSavingName = false,
                        nameSaveSuccess = success,
                        // ✅ On success, exit edit mode automatically.
                        isEditMode = !success,
                        nameError = if (success) null else "Failed to save name"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSavingName = false,
                        nameError = "Error saving name: ${e.message}"
                    )
                }
            }
        }
    }

    // ✅ NEW: Centralized logic to toggle edit mode.
    fun toggleEditMode() {
        _uiState.update { currentState ->
            val isEnteringEditMode = !currentState.isEditMode
            // If leaving edit mode, reset any changes.
            if (!isEnteringEditMode) {
                currentState.copy(
                    isEditMode = false,
                    editableName = currentState.originalName, // Reset to original
                    nameError = null,
                    nameSaveSuccess = false
                )
            } else {
                currentState.copy(isEditMode = true)
            }
        }
    }

    fun showClearDataDialog(show: Boolean) {
        _uiState.update { it.copy(showClearDataDialog = show) }
    }

    fun clearAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingData = true, nameError = null) }
            try {
                repository.clearAllData()
                _uiState.update { it.copy(isClearingData = false) }
                onComplete() // Callback to navigate or reset UI
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isClearingData = false,
                        // Using nameError field to display a generic error on this screen
                        nameError = "Error clearing data: ${e.message}"
                    )
                }
            }
        }
    }

    fun dismissNameChangedDialog() {
        _uiState.update { it.copy(nameSaveSuccess = false) }
    }
}

// ✅ MODIFIED: Added isEditMode to the UI state.
data class ProfileUiState(
    val user: User? = null,
    val editableName: String = "",
    val originalName: String = "",
    val nameError: String? = null,
    val nameSaveSuccess: Boolean = false,
    val isSavingName: Boolean = false,
    val showClearDataDialog: Boolean = false,
    val isClearingData: Boolean = false,
    val isEditMode: Boolean = false // State to control the edit UI
)

class ProfileViewModelFactory(
    private val repository: ExpenseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
