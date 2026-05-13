package com.expensetracker.ui.overview

import android.app.Application
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.expensetracker.data.models.Budget
import com.expensetracker.data.models.Category
import com.expensetracker.data.repository.ExpenseRepository
import com.expensetracker.ui.components.SimpleDonutChart
import com.expensetracker.ui.theme.ExpenseRed
import com.expensetracker.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun OverviewScreen(
    repository: ExpenseRepository,
    onNavigateToTransactions: () -> Unit
) {

    val scope = rememberCoroutineScope()
    var showBudgetDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: OverviewViewModel = remember {
        OverviewViewModel(repository, application)
    }

    val uiState by viewModel.uiState.collectAsState()
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
    }


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Budget Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Monthly Budget Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Donut Chart
                    Box(
                        modifier = Modifier.size(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val donutBudgetColor = if (isSystemInDarkTheme()) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            Color.Gray.copy(alpha = 0.5f)
                        }
                        SimpleDonutChart(
                            data = listOf(
                                uiState.totalExpenses.toFloat(),
                                maxOf(0.0, uiState.totalBudget - uiState.totalExpenses).toFloat()
                            ),
                            colors = listOf(
                                ExpenseRed,
                                donutBudgetColor
                                //MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxSize()
                        )

                        // Center content
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = currencyFormatter.format(uiState.totalExpenses),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.budgetPercentage}% used",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Budget Stats
                    // Budget Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        BudgetStatItem(
                            title = "Expenses",
                            value = currencyFormatter.format(uiState.totalExpenses),
                            color = ExpenseRed
                        )

                        VerticalDivider(
                            modifier = Modifier
                                .height(40.dp) // Adjust height as needed
                                .width(5.dp),   // Adjust width as needed
                            thickness = DividerDefaults.Thickness,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )

                        BudgetStatItem(
                            title = "Budget",
                            value = currencyFormatter.format(uiState.totalBudget),
                            onClick = { showBudgetDialog = true }
                        )

                        VerticalDivider(
                            modifier = Modifier
                                .height(40.dp) // Adjust height as needed
                                .width(5.dp),   // Adjust width as needed
                            thickness = DividerDefaults.Thickness,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )

                        BudgetStatItem(
                            title = "Safe to Spend",
                            value = currencyFormatter.format(uiState.safeToSpend) + "/day",
                            color = SuccessGreen
                        )
                    }

// Keep only one instance of the budget dialog outside the Row
                    if (showBudgetDialog) {
                        SimpleBudgetEditDialog(
                            initialBudget = uiState.totalBudget.toInt(),
                            onConfirm = { newBudget ->
                                scope.launch {
                                    val cal = Calendar.getInstance()
                                    val month = cal.get(Calendar.MONTH) + 1
                                    val year = cal.get(Calendar.YEAR)
                                    repository.setBudget(
                                        Budget(
                                            month = month,
                                            year = year,
                                            totalBudget = newBudget.toDouble()
                                        )
                                    )
                                }
                                showBudgetDialog = false
                            },
                            onDismiss = { showBudgetDialog = false }
                        )
                    }


                    Spacer(modifier = Modifier.height(16.dp))

                    // Update button
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.lastUpdateMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.updateTransactions() },
                            enabled = !uiState.isUpdating,
                            modifier = Modifier.height(36.dp)
                        ) {
                            if (uiState.isUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Update")
                            }
                        }
                    }
                }
            }
        }

        // Top Categories Card
        item {
            SectionCard(
                title = "Top Categories this Month",
                onViewAllClick = onNavigateToTransactions
            ) {
                if (uiState.topCategories.isEmpty()) {
                    EmptyStateText("No expenses found for this month")
                } else {
                    uiState.topCategories.forEach { (category, amount) ->
                        CategoryItem(
                            categoryName = category.name,
                            amount = currencyFormatter.format(amount),
                            icon = category.icon
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    text = "For detailed insights, click 'Analytics' at the bottom.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Top Transactions Card
        item {
            SectionCard(
                title = "Top Expenses this Month",
                onViewAllClick = onNavigateToTransactions
            ) {
                if (uiState.topTransactions.isEmpty()) {
                    EmptyStateText("No transactions found for this month")
                } else {
                    uiState.topTransactions.forEach { transaction ->
                        val categoryIconString = Category.getDefaultCategories()
                            .find { it.id == transaction.categoryId }
                            ?.icon ?: "category"
                        TransactionItem(
                            name = transaction.getDisplayName(),
                            amount = currencyFormatter.format(transaction.amount),
                            date = transaction.date,
                            icon = categoryIconString
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetStatItem(
    title: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null
) {
    val isClickable = onClick != null

    Card(
        modifier = if (isClickable) {
            Modifier.clickable { onClick() }
        } else {
            Modifier
        },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isClickable) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Tap to edit",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = value,
                style = if (title == "Safe to Spend") MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}


@Composable
fun SectionCard(
    title: String,
    onViewAllClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onViewAllClick) {
                    Text("View All")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun CategoryItem(
    categoryName: String,
    amount: String,
    icon: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Category.getIconForCategory(icon),
                contentDescription = categoryName,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = categoryName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ExpenseRed
        )
    }
}

@Composable
fun TransactionItem(
    name: String,
    amount: String,
    date: Date,
    icon: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Category.getIconForCategory(icon),
                    contentDescription = name,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = DateFormat.format("dd MMM", date).toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = ExpenseRed
        )
    }
}

@Composable
fun SimpleBudgetEditDialog(
    initialBudget: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var budgetText by remember { mutableStateOf(initialBudget.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val newBudget = budgetText.toIntOrNull() ?: initialBudget
                    onConfirm(newBudget)
                },
                enabled = budgetText.toIntOrNull() != null && budgetText.toIntOrNull()!! > 0
            ) { Text("Save") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Set Monthly Budget") },
        text = {
            OutlinedTextField(
                value = budgetText,
                onValueChange = { budgetText = it.filter { c -> c.isDigit() } },
                label = { Text("Monthly Budget (INR)") }
            )
        }
    )
}


@Composable
fun EmptyStateText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

