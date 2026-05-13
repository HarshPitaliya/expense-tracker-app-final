package com.expensetracker.ui.setup

import android.Manifest
import android.app.Application
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expensetracker.data.repository.ExpenseRepository
import com.expensetracker.ui.components.DateRangePicker
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InitialSetupScreen(
    repository: ExpenseRepository,
    onSetupComplete: () -> Unit
) {
    // Get the Application context, which is safe to pass to a ViewModel
    val application = LocalContext.current.applicationContext as Application
    val viewModel: InitialSetupViewModel = viewModel {
        // Pass the Application context to the ViewModel
        InitialSetupViewModel(repository, application)
    }

    val uiState by viewModel.uiState.collectAsState()

    val smsPermissionState = rememberPermissionState(Manifest.permission.READ_SMS)
    val receiveSmsPermissionState = rememberPermissionState(Manifest.permission.RECEIVE_SMS)

    LaunchedEffect(smsPermissionState.status.isGranted, receiveSmsPermissionState.status.isGranted) {
        if (smsPermissionState.status.isGranted && receiveSmsPermissionState.status.isGranted) {
            viewModel.updatePermissionGranted(true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to Expense Tracker",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Let's set up your expense tracking",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Setup Form Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Name Input Section
                Text(
                    text = "Personal Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::updateName,
                    label = { Text("Your Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !uiState.isLoading
                )

                Spacer(modifier = Modifier.height(24.dp))

                // SMS Permission Section
                Text(
                    text = "SMS Permissions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "We need permission to read SMS messages to automatically track your expenses from bank notifications.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (!smsPermissionState.status.isGranted || !receiveSmsPermissionState.status.isGranted) {
                    Button(
                        onClick = {
                            smsPermissionState.launchPermissionRequest()
                            receiveSmsPermissionState.launchPermissionRequest()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text("Grant SMS Permissions")
                    }

                    if (smsPermissionState.status.shouldShowRationale ||
                        receiveSmsPermissionState.status.shouldShowRationale) {
                        Text(
                            text = "SMS permissions are required to automatically parse transaction messages. Please grant the permission to continue.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Test SMS Access Button
                    OutlinedButton(
                        onClick = { viewModel.validateSMSAccess() },
                        enabled = !uiState.isValidatingSMS,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isValidatingSMS) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Validating...")
                        } else {
                            Text("Test SMS Access")
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = "✓ SMS Permissions Granted",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Test SMS Access Button
                    OutlinedButton(
                        onClick = { viewModel.validateSMSAccess() },
                        enabled = !uiState.isValidatingSMS,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isValidatingSMS) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Validating...")
                        } else {
                            Text("Test SMS Access")
                        }
                    }
                }

                // SMS Validation Message
                uiState.smsValidationMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 24.dp),
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )

                // Date Range Section
                Text(
                    text = "Date Range",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Select the start date from which you want to parse SMS messages for transactions (from 1 Jan 2025).",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                DateRangePicker(
                    startDate = uiState.startDate,
                    endDate = END_DATE,
                    onStartDateSelected = viewModel::updateStartDate,
                    enabled = !uiState.isLoading
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Progress Section
                if (uiState.isLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.processingProgress > 0) {
                            LinearProgressIndicator(
                                progress = { uiState.processingProgress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Text(
                            text = uiState.processingMessage.takeIf { it.isNotBlank() } ?: "Setting up...",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Complete Setup Button
                Button(
                    onClick = { viewModel.completeSetup(onSetupComplete) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.canCompleteSetup && !uiState.isLoading
                ) {
                    Text("Complete Setup")
                }

                uiState.errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}