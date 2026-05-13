package com.expensetracker.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expensetracker.data.models.Category
import com.expensetracker.data.repository.ExpenseRepository
import com.expensetracker.ui.components.BarChart
import com.expensetracker.ui.components.CategoryDonutChartWithValues
import java.text.DateFormatSymbols
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    // The repository parameter has been removed.
    // The factory now gets the repository instance directly.
    viewModel: AnalyticsViewModel = viewModel(factory = AnalyticsViewModelFactory(ExpenseRepository.getInstance(LocalContext.current)))
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Analytics and Historical Trends") }) }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            // 1. Bar Chart Card at top
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Monthly Expenses vs Budget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (uiState.historicalExpensesData.isNotEmpty()) {
                        val groupedData = mutableListOf<Float>()
                        val groupedLabels = uiState.historicalMonthLabels.toList()
                        val barColors = listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.primary)
                        for (i in uiState.historicalExpensesData.indices) {
                            groupedData.add(uiState.historicalExpensesData[i])
                            groupedData.add(uiState.historicalBudgetsData[i])
                        }

                        BarChart(
                            data = groupedData,
                            labels = List(groupedData.size) {
                                if (it % 2 == 0) groupedLabels[it / 2] + "\nExp"
                                else groupedLabels[it / 2] + "\nBud"
                            },
                            barColors = barColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            onBarClick = { index ->
                                val monthIndex = index / 2
                                if (monthIndex in groupedLabels.indices) {
                                    val monthLabel = groupedLabels[monthIndex] // e.g., "Jan 24"
                                    val parts = monthLabel.split(" ")
                                    val monthNum = DateFormatSymbols.getInstance(Locale.ENGLISH).shortMonths.indexOf(parts[0]) + 1
                                    val year = ("20" + parts[1]).toInt() // Assuming "24" means 2024
                                    viewModel.updateSelectedMonthYear(monthNum, year)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(MaterialTheme.colorScheme.error, shape = RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Expenses", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.width(24.dp))
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Budget", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No data available for chart",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Month Selector Card with navigation
            uiState.selectedMonth?.let { month ->
                uiState.selectedYear?.let { year ->
                    MonthSelectorCard(
                        selectedMonth = month,
                        selectedYear = year,
                        onMonthChange = viewModel::updateSelectedMonthYear,
                        canGoBack = viewModel.canNavigateBack(month, year),
                        canGoForward = viewModel.canNavigateForward(month, year)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. At a Glance Card for selected month
            uiState.selectedMonth?.let { month ->
                uiState.selectedYear?.let { year ->
                    // The repository is no longer passed here. Data comes from uiState.
                    AtAGlanceCard(
                        spendingTrend = uiState.spendingTrend,
                        budgetStatus = uiState.budgetStatus,
                        daysLeft = uiState.daysLeft,
                        isCurrentMonth = uiState.isCurrentMonth,
                        selectedMonth = month,
                        selectedYear = year
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Category Breakdown Card
            uiState.selectedMonthAnalytics?.let { analytics ->
                CategoryBreakdownCard(
                    analytics = analytics,
                    categories = uiState.categories,
                    currencyFormatter = currencyFormatter
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MonthSelectorCard(
    selectedMonth: Int,
    selectedYear: Int,
    onMonthChange: (Int, Int) -> Unit,
    canGoBack: Boolean,
    canGoForward: Boolean
) {
    // Helper functions are now pure and can remain here or be moved to a utils file.
    fun getPreviousMonth(year: Int, month: Int): Pair<Int, Int> {
        return if (month == 1) Pair(year - 1, 12) else Pair(year, month - 1)
    }

    fun getNextMonth(year: Int, month: Int): Pair<Int, Int> {
        return if (month == 12) Pair(year + 1, 1) else Pair(year, month + 1)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val (prevYear, prevMonth) = getPreviousMonth(selectedYear, selectedMonth)
                onMonthChange(prevMonth, prevYear)
            }, enabled = canGoBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Month"
                )
            }

            Text(
                text = formatMonthYearFull(selectedMonth, selectedYear),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = {
                val (nextYear, nextMonth) = getNextMonth(selectedYear, selectedMonth)
                onMonthChange(nextMonth, nextYear)
            }, enabled = canGoForward) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Month"
                )
            }
        }
    }
}

@Composable
private fun AtAGlanceCard(
    // The signature is now clean, accepting primitive data types.
    spendingTrend: String,
    budgetStatus: String,
    daysLeft: Int,
    isCurrentMonth: Boolean,
    selectedMonth: Int,
    selectedYear: Int
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📊 ${formatMonthYearFull(selectedMonth, selectedYear)} at a Glance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            InsightRow("Spending Trend", spendingTrend)
            InsightRow("Budget Status", budgetStatus)
            InsightRow("Days Left", if (isCurrentMonth) "$daysLeft days" else "Past month")
        }
    }
}

@Composable
private fun CategoryBreakdownCard(
    analytics: ExpenseRepository.MonthlyAnalytics,
    categories: List<Category>,
    currencyFormatter: NumberFormat
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Category Breakdown for ${formatMonthYearFull(analytics.month, analytics.year)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            val categoryData: List<Pair<String, Double>> = analytics.categoryBreakdown.entries
                .map { entry ->
                    val categoryName = categories.find { it.id == entry.key }?.name ?: "Uncategorized"
                    categoryName to entry.value
                }
                .filter { it.second > 0.0 }
                .sortedByDescending { it.second }

            if (categoryData.isEmpty()) {
                Text(
                    "No category expense data for this month.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val vibrantColors = listOf(
                    Color(0xFFE91E63), Color(0xFF2196F3), Color(0xFFFFC107),
                    Color(0xFF4CAF50), Color(0xFFFF5722), Color(0xFF9C27B0),
                    Color(0xFF3F51B5), Color(0xFFFF9800), Color(0xFF009688), Color(0xFFCDDC39)
                )
                CategoryDonutChartWithValues(
                    categoryData = categoryData,
                    colors = categoryData.mapIndexed { index, _ ->
                        vibrantColors[index % vibrantColors.size]
                    },
                    modifier = Modifier.fillMaxWidth(),
                    currencyFormatter = currencyFormatter
                )
            }
        }
    }
}

@Composable
private fun InsightRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "• $label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun formatMonthYearFull(month: Int, year: Int): String {
    val monthName = DateFormatSymbols.getInstance(Locale.ENGLISH).months[month - 1]
    return "$monthName $year"
}
