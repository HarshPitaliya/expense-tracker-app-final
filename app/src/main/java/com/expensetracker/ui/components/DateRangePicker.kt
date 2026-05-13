package com.expensetracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePicker(
    startDate: Date,
    endDate: Date,
    onStartDateSelected: (Date) -> Unit,
    enabled: Boolean = true,
    minDate: Date? = null,
    maxDate: Date? = null
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    var showStartDatePicker by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Start Date
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Start Date",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = enabled) { showStartDatePicker = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.width(16.dp),
                            tint = if (enabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dateFormatter.format(startDate),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (enabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            }

            // End Date (Fixed)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "End Date (Fixed)",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.width(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dateFormatter.format(endDate),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Month–Year dropdown dialog (Final 2)
        if (showStartDatePicker) {
            val bounds = computeBounds(minDate, maxDate)
            MonthYearDropdownDialog(
                initial = startDate,
                minDate = bounds.first,
                maxDate = bounds.second,
                onDismiss = { showStartDatePicker = false },
                onConfirm = { picked ->
                    onStartDateSelected(picked)
                    showStartDatePicker = false
                }
            )
        }
    }
}

/**

Compute safe bounds for the Month–Year picker:

Lower bound: 01 Jan 2025 00:00.

Upper bound: 01 currentMonth currentYear 00:00 (no future).

If external min/max provided, intersect with these safe bounds.
 */
private fun computeBounds(minDate: Date?, maxDate: Date?): Pair<Date, Date> {
    val hardMin = Calendar.getInstance().apply {
        set(2025, Calendar.JANUARY, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.time
    val now = Calendar.getInstance()
    val hardMax = Calendar.getInstance().apply {
        set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    val min = maxOf(hardMin, minDate ?: hardMin)
    val max = minOf(hardMax, maxDate ?: hardMax)
    return Pair(min, max)
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthYearDropdownDialog(
    initial: Date,
    minDate: Date,
    maxDate: Date,
    onDismiss: () -> Unit,
    onConfirm: (Date) -> Unit
) {
    val monthLabels = listOf(
        "Jan","Feb","Mar","Apr","May","Jun",
        "Jul","Aug","Sep","Oct","Nov","Dec"
    )
    val minCal = remember(minDate) { Calendar.getInstance().apply { time = minDate } }
    val maxCal = remember(maxDate) { Calendar.getInstance().apply { time = maxDate } }
    val initCal = remember(initial) { Calendar.getInstance().apply { time = initial } }

    val minYear = minCal.get(Calendar.YEAR)
    val maxYear = maxCal.get(Calendar.YEAR)

    var expandedMonth by remember { mutableStateOf(false) }
    var expandedYear by remember { mutableStateOf(false) }
    var month by remember { mutableStateOf(initCal.get(Calendar.MONTH)) }
    var year by remember { mutableStateOf(initCal.get(Calendar.YEAR)) }

    val years = remember(minYear, maxYear) { (minYear..maxYear).toList() }

    val pickedCal = remember(month, year) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val pickedDate = pickedCal.time

    val isValid = !pickedDate.before(minDate) && !pickedDate.after(maxDate)
    val errorText = "Month selected should be between Jan 2025 and ${monthLabels[maxCal.get(Calendar.MONTH)]} ${maxCal.get(Calendar.YEAR)}."

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Month & Year") },
        text = {
            Column {
                // Month dropdown
                Text("Month", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(0.dp).padding(top = 6.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedMonth,
                    onExpandedChange = { expandedMonth = it }
                ) {
                    OutlinedTextField(
                        value = monthLabels[month],
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true)
                            .fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMonth) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMonth,
                        onDismissRequest = { expandedMonth = false }
                    ) {
                        monthLabels.forEachIndexed { index, label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    month = index
                                    expandedMonth = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.width(0.dp).padding(top = 16.dp))

                // Year dropdown (no future years)
                Text("Year", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(0.dp).padding(top = 6.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedYear,
                    onExpandedChange = { expandedYear = it }
                ) {
                    OutlinedTextField(
                        value = year.toString(),
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true)
                            .fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedYear) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedYear,
                        onDismissRequest = { expandedYear = false }
                    ) {
                        years.forEach { y ->
                            DropdownMenuItem(
                                text = { Text(y.toString()) },
                                onClick = {
                                    year = y
                                    expandedYear = false
                                }
                            )
                        }
                    }
                }

                if (!isValid) {
                    Spacer(Modifier.width(0.dp).padding(top = 12.dp))
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = { onConfirm(pickedDate) }
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

