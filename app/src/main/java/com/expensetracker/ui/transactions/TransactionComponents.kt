package com.expensetracker.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expensetracker.data.models.Category
import com.expensetracker.data.models.Transaction
import java.text.NumberFormat
import java.util.Calendar
import java.util.Date

@Composable
fun TransactionListItem(
    transaction: Transaction,
    categoryName: String,
    currencyFormatter: NumberFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Category.getIconForCategory(Category.getDefaultCategories().find { it.id == transaction.categoryId }?.icon ?: "category"),
                    contentDescription = categoryName,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = transaction.getDisplayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (transaction.isExpense) "-${currencyFormatter.format(transaction.amount)}"
                    else "+${currencyFormatter.format(transaction.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.isExpense) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.size(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${
                        android.text.format.DateFormat.format(
                            "dd MMM yyyy",
                            transaction.date
                        )
                    } • $categoryName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Edit button with text below icon
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onClick() }.padding(end = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Transaction",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Edit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionActionSheet(
    transaction: Transaction,
    categories: List<Category>,
    onSaveAll: (newName: String, isExpense: Boolean, categoryId: String?) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    var renameValue by remember { mutableStateOf(transaction.getDisplayName()) }
    var isExpenseLocal by remember { mutableStateOf(transaction.isExpense) }
    var selectedCategoryId by remember { mutableStateOf(transaction.categoryId) }
    var catMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    // Validation and change detection
    val nameTrimmed = remember(renameValue) { renameValue.trim() }
    val nameValid = remember(nameTrimmed) {
        nameTrimmed.isNotBlank() && Regex("^[A-Za-z0-9 ]+$").matches(nameTrimmed)
    }
    val hasChanges = remember(nameTrimmed, isExpenseLocal, selectedCategoryId) {
        nameTrimmed != transaction.getDisplayName() ||
                isExpenseLocal != transaction.isExpense ||
                selectedCategoryId != transaction.categoryId
    }
    val canSave = nameValid && hasChanges && !saving

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Context header
        Text(
            "Edit Transaction",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        // Name
        OutlinedTextField(
            value = renameValue,
            onValueChange = { if (it.length <= 40) renameValue = it },
            label = { Text("Rename Transaction") },
            supportingText = { Text("${renameValue.length}/40") },
            isError = !nameValid,
            modifier = Modifier.fillMaxWidth()
        )
        if (!nameValid) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Only letters, numbers and spaces are allowed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(12.dp))

        // Change Category
        ExposedDropdownMenuBox(
            expanded = catMenuExpanded,
            onExpandedChange = { catMenuExpanded = !catMenuExpanded }
        ) {
            OutlinedTextField(
                value = categories.find { it.id == selectedCategoryId }?.name ?: "Uncategorized",
                onValueChange = {},
                readOnly = true,
                label = { Text("Change Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catMenuExpanded) },
                modifier = Modifier
                    .menuAnchor(
                        type = MenuAnchorType.PrimaryNotEditable,
                        enabled = true
                    )
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = catMenuExpanded,
                onDismissRequest = { catMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Uncategorized") },
                    onClick = {
                        selectedCategoryId = "undefined"
                        catMenuExpanded = false
                    }
                )
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                categories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat.name) },
                        onClick = {
                            selectedCategoryId = cat.id
                            catMenuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Type toggle
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Change Type: ")
            Spacer(Modifier.width(8.dp))
            AssistChip(
                onClick = { isExpenseLocal = true },
                label = { Text("Expense") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (isExpenseLocal) MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.12f
                    ) else MaterialTheme.colorScheme.surface,
                    labelColor = if (isExpenseLocal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(Modifier.width(8.dp))
            AssistChip(
                onClick = { isExpenseLocal = false },
                label = { Text("Income") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (!isExpenseLocal) MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.12f
                    ) else MaterialTheme.colorScheme.surface,
                    labelColor = if (!isExpenseLocal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            )
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        Spacer(Modifier.height(8.dp))

        // Bottom actions
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onClose,
                enabled = !saving,
                modifier = Modifier.weight(1f)
            ) { Text("Cancel") }

            Button(
                onClick = {
                    saving = true
                    onSaveAll(nameTrimmed, isExpenseLocal, selectedCategoryId)
                    // saving resets when sheet closes from caller
                },
                enabled = canSave,
                modifier = Modifier.weight(1f)
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Save Changes")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Centered red "Delete Transaction" with confirm
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Text(
                    text = "Delete Transaction",
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete transaction?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    categories: List<Category>,
    onCancel: () -> Unit,
    onSave: (name: String, amount: Double, date: Date, categoryId: String?, isExpense: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var date by remember { mutableStateOf(Date()) }
    var catMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Date bounds [01 Jan 2025 .. today]
    val minCal = remember {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, 2025); set(
            Calendar.MONTH,
            Calendar.JANUARY
        ); set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(
            Calendar.MILLISECOND,
            0
        )
        }
    }
    val maxCal = remember {
        Calendar.getInstance().apply {
            time = Date()
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(
            Calendar.MILLISECOND,
            999
        )
        }
    }
    val inRange = date.time in minCal.timeInMillis..maxCal.timeInMillis
    val isValid =
        name.isNotBlank() && amountText.toDoubleOrNull()?.let { it > 0.0 } == true && inRange

    fun Long.toDate(): Date = Date(this)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            "Add Transaction",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 40) name = it },
            label = { Text("Transaction Name e.g. ICICI Bank") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = amountText,
            onValueChange = { input ->
                if (input.matches(Regex("^\\d{0,9}(\\.\\d{0,2})?$"))) amountText = input
            },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
        )

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Type: ")
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = isExpense,
                onClick = { isExpense = true },
                label = { Text("Expense") })
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = !isExpense,
                onClick = { isExpense = false },
                label = { Text("Income") })
        }

        Spacer(Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = catMenuExpanded,
            onExpandedChange = { catMenuExpanded = !catMenuExpanded }
        ) {
            OutlinedTextField(
                value = categories.find { it.id == selectedCategoryId }?.name ?: "Uncategorized",
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catMenuExpanded) },
                modifier = Modifier
                    .menuAnchor(
                        type = MenuAnchorType.PrimaryNotEditable, // Specify the type
                        enabled = true // Usually true, but can be dynamic
                    )
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = catMenuExpanded,
                onDismissRequest = { catMenuExpanded = false }
            ) {
                DropdownMenuItem(text = { Text("Uncategorized") }, onClick = {
                    selectedCategoryId = "undefined"; catMenuExpanded = false
                })
                categories.forEach { cat ->
                    DropdownMenuItem(text = { Text(cat.name) }, onClick = {
                        selectedCategoryId = cat.id; catMenuExpanded = false
                    })
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        // Date row + picker
        OutlinedTextField(
            value = android.text.format.DateFormat.format("dd MMM yyyy", date).toString(),
            onValueChange = { /* Read-only, no action needed */ },
            label = { Text("Date") },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Filled.DateRange, contentDescription = "Select Date")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (showDatePicker) {
            val state = rememberDatePickerState(
                initialSelectedDateMillis = date.time.coerceIn(
                    minCal.timeInMillis,
                    maxCal.timeInMillis
                ),
                yearRange = 2025..Calendar.getInstance().get(Calendar.YEAR)
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            state.selectedDateMillis?.let { picked ->
                                if (picked in minCal.timeInMillis..maxCal.timeInMillis) {
                                    date = picked.toDate()
                                    showDatePicker = false
                                }
                            }
                        }
                    ) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDatePicker = false
                    }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = state)
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: return@Button
                    onSave(name.trim(), amt, date, selectedCategoryId, isExpense)
                },
                enabled = isValid,
                modifier = Modifier.weight(1f)
            ) { Text("Save") }
        }
    }
}






