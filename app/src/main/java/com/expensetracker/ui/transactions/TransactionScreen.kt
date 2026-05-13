package com.expensetracker.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    repository: ExpenseRepository, // Repository is still passed for ViewModel factory
    onBack: () -> Unit,
    viewModel: TransactionViewModel = viewModel(factory = TransactionViewModelFactory(repository)) // Inject ViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val bottomSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val currencyFormatter =
        remember { // Remember the formatter to avoid recreation
            NumberFormat.getCurrencyInstance(
                Locale.Builder().setLanguage("en").setRegion("IN").build()
            )
        }

    // Handle sheet visibility
    if (uiState.showAddSheet || uiState.selectedTransaction != null) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    bottomSheetState.hide()
                    viewModel.hideTransactionSheets()
                }
            },
            sheetState = bottomSheetState
        ) {
            if (uiState.showAddSheet) {
                AddTransactionSheet(
                    categories = uiState.categories,
                    onCancel = {
                        scope.launch {
                            bottomSheetState.hide()
                            viewModel.hideTransactionSheets()
                        }
                    },
                    onSave = { name, amount, date, categoryId, isExpense ->
                        viewModel.addTransaction(name, amount, date, categoryId, isExpense)
                        scope.launch { bottomSheetState.hide() } // Hide after saving
                    }
                )
            } else {
                uiState.selectedTransaction?.let { tx ->
                    TransactionActionSheet(
                        transaction = tx,
                        categories = uiState.categories,
                        onSaveAll = { newName, isExpenseDesired, newCategoryId ->
                            viewModel.updateTransaction(tx.id, newName, isExpenseDesired, newCategoryId)
                            scope.launch { bottomSheetState.hide() } // Hide after saving
                        },
                        onDelete = {
                            viewModel.deleteTransaction(tx.id)
                            scope.launch { bottomSheetState.hide() } // Hide after deleting
                        },
                        onClose = {
                            scope.launch {
                                bottomSheetState.hide()
                                viewModel.hideTransactionSheets()
                            }
                        }
                    )
                } ?: Spacer(Modifier.height(1.dp)) // Should not happen if selectedTransaction is not null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Transactions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleSortByValue) {
                        Icon(
                            imageVector = if (uiState.sortByValue) Icons.AutoMirrored.Filled.Sort else Icons.Default.DateRange,
                            contentDescription = "Toggle Sort"
                        )
                    }
                }
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // Search field
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    label = { Text("Search transactions") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )

                // Filters row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category filter
                    var categoryMenuExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = categoryMenuExpanded,
                        onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = uiState.categories.find { it.id == uiState.selectedCategoryId }?.name
                                ?: "All Categories",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = categoryMenuExpanded
                                )
                            },
                            modifier = Modifier
                                .menuAnchor(
                                    type = MenuAnchorType.PrimaryNotEditable,
                                    enabled = true
                                )
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Categories") },
                                onClick = {
                                    viewModel.updateSelectedCategory(null)
                                    categoryMenuExpanded = false
                                }
                            )
                            uiState.categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        viewModel.updateSelectedCategory(category.id)
                                        categoryMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Month filter
                    var monthMenuExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = monthMenuExpanded,
                        onExpandedChange = { monthMenuExpanded = !monthMenuExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        val monthFormatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                        val displayText = if (uiState.selectedMonthYear != null) {
                            val calendar = Calendar.getInstance()
                            calendar.set(uiState.selectedMonthYear!!.second, uiState.selectedMonthYear!!.first - 1, 1)
                            monthFormatter.format(calendar.time)
                        } else "All Months"

                        OutlinedTextField(
                            value = displayText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Month") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = monthMenuExpanded
                                )
                            },
                            modifier = Modifier
                                .menuAnchor(
                                    type = MenuAnchorType.PrimaryNotEditable,
                                    enabled = true
                                )
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = monthMenuExpanded,
                            onDismissRequest = { monthMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Months") },
                                onClick = {
                                    viewModel.updateSelectedMonthYear(null)
                                    monthMenuExpanded = false
                                }
                            )
                            uiState.availableMonths.forEach { (month, year) ->
                                val calendar = Calendar.getInstance()
                                calendar.set(year, month - 1, 1)
                                val monthYearText = monthFormatter.format(calendar.time)
                                DropdownMenuItem(
                                    text = { Text(monthYearText) },
                                    onClick = {
                                        viewModel.updateSelectedMonthYear(Pair(month, year))
                                        monthMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.transactions.isEmpty()) {
                    Text(
                        text = if (uiState.searchQuery.isBlank() && uiState.selectedCategoryId == null && uiState.selectedMonthYear == null)
                            "No transactions found"
                        else "No matching transactions",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentHeight(Alignment.CenterVertically)
                            .padding(horizontal = 16.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.transactions) { tx ->
                            val categoryName = uiState.categories.find { it.id == tx.categoryId }?.name
                                ?: uiState.categories.find { it.id == "undefined" }?.name
                                ?: "Uncategorized"

                            TransactionListItem(
                                transaction = tx,
                                categoryName = categoryName,
                                currencyFormatter = currencyFormatter,
                                onClick = {
                                    viewModel.selectTransactionForEdit(tx)
                                    scope.launch { bottomSheetState.show() }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}