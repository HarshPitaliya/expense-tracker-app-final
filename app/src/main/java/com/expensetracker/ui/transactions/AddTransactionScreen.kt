package com.expensetracker.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expensetracker.data.repository.ExpenseRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    // Categories are now part of the ViewModel's UI state
    repository: ExpenseRepository, // Repository is still passed for ViewModel factory
    onNavigateBack: () -> Unit,
    viewModel: AddTransactionViewModel = viewModel(factory = AddTransactionViewModelFactory(repository))
) {
    val uiState by viewModel.uiState.collectAsState()

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    // NEW: Local state for the category dropdown menu expansion
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            TopAppBar(
                title = { Text("Add Transaction") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::updateName,
                label = { Text("Transaction Name") },
                placeholder = { Text("E.g. ICICI Bank") },
                isError = uiState.nameError != null,
                supportingText = {
                    uiState.nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.amountText,
                onValueChange = viewModel::updateAmountText,
                label = { Text("Amount") },
                placeholder = { Text("E.g. 1000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = uiState.amountError != null,
                supportingText = {
                    uiState.amountError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Type: ", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = uiState.isExpense,
                    onClick = { viewModel.updateIsExpense(true) },
                    label = { Text("Expense") },
                    enabled = !uiState.isSaving
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = !uiState.isExpense,
                    onClick = { viewModel.updateIsExpense(false) },
                    label = { Text("Income") },
                    enabled = !uiState.isSaving
                )
            }
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = categoryMenuExpanded, // MODIFIED: Use local state for category dropdown
                onExpandedChange = { categoryMenuExpanded = it }, // MODIFIED: Update local state
                modifier = Modifier.fillMaxWidth()
            ) {
                val currentCategoryName =
                    uiState.categories.find { it.id == uiState.selectedCategoryId }?.name ?: "Uncategorized"
                OutlinedTextField(
                    value = currentCategoryName,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) }, // MODIFIED
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true)
                        .fillMaxWidth(),
                    enabled = !uiState.isSaving
                )
                ExposedDropdownMenu(
                    expanded = categoryMenuExpanded, // MODIFIED
                    onDismissRequest = { categoryMenuExpanded = false } // MODIFIED
                ) {
                    DropdownMenuItem(
                        text = { Text("Uncategorized") },
                        onClick = {
                            viewModel.updateSelectedCategoryId(null)
                            categoryMenuExpanded = false // MODIFIED
                        }
                    )
                    uiState.categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = {
                                viewModel.updateSelectedCategoryId(cat.id)
                                categoryMenuExpanded = false // MODIFIED
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = dateFormatter.format(uiState.date),
                onValueChange = { },
                readOnly = true,
                label = { Text("Date") },
                trailingIcon = {
                    // This correctly uses viewModel.showDatePicker(true)
                    IconButton(onClick = { viewModel.showDatePicker(true) }, enabled = !uiState.isSaving) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select Date"
                        )
                    }
                },
                isError = uiState.dateError != null,
                supportingText = {
                    uiState.dateError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            )

            if (uiState.showDatePicker) {
                // Correctly extract the year from Date objects using Calendar
                val minYear = remember {
                    val calendar = Calendar.getInstance()
                    calendar.time = TRANSACTION_MIN_DATE
                    calendar.get(Calendar.YEAR)
                }
                val maxYear = remember {
                    val calendar = Calendar.getInstance()
                    calendar.time = TRANSACTION_MAX_DATE
                    calendar.get(Calendar.YEAR)
                }

                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = uiState.date.time,
                    yearRange = minYear..maxYear
                )

                DatePickerDialog(
                    onDismissRequest = { viewModel.showDatePicker(false) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let {
                                    val pickedDate = it.coerceIn(
                                        TRANSACTION_MIN_DATE.time,
                                        TRANSACTION_MAX_DATE.time
                                    )
                                    viewModel.updateDate(Date(pickedDate))
                                }
                                viewModel.showDatePicker(false)
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.showDatePicker(false) }) {
                            Text("Cancel")
                        }
                    },
                    properties = DialogProperties()
                ) {
                    DatePicker(state = datePickerState)
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isSaving
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { viewModel.saveTransaction(onNavigateBack) },
                    enabled = uiState.isFormValid,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }

            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}