package com.expensetracker.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expensetracker.data.repository.ExpenseRepository
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    repository: ExpenseRepository,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(repository))
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val scrollState = rememberScrollState()

    // REMOVED: Local isEditMode state. It's now driven by the ViewModel.

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- User Info ---
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("User Information", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.editableName,
                        onValueChange = viewModel::updateEditableName,
                        label = { Text("Profile Name") },
                        isError = uiState.nameError != null,
                        supportingText = {
                            uiState.nameError?.let { error ->
                                Text(error, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        // ✅ MODIFIED: The enabled and readOnly states are now controlled by the ViewModel's isEditMode.
                        enabled = uiState.isEditMode,
                        readOnly = !uiState.isEditMode,
                        trailingIcon = {
                            IconButton(
                                // ✅ MODIFIED: onClick now calls the ViewModel to toggle the edit state.
                                onClick = viewModel::toggleEditMode,
                                enabled = !uiState.isSavingName
                            ) {
                                Icon(
                                    // ✅ MODIFIED: Icon is determined by the ViewModel's state.
                                    imageVector = if (uiState.isEditMode) Icons.Default.Close else Icons.Filled.Edit,
                                    contentDescription = if (uiState.isEditMode) "Cancel edit" else "Edit name",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )

                    // ✅ MODIFIED: The save button is only shown when in edit mode for a cleaner UI.
                    if (uiState.isEditMode) {
                        Spacer(Modifier.height(8.dp))
                        val hasChanges = uiState.editableName.trim() != uiState.originalName.trim()
                        val canSave = hasChanges && uiState.nameError == null && !uiState.isSavingName

                        Button(
                            onClick = viewModel::saveUserName,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canSave
                        ) {
                            if (uiState.isSavingName) {
                                Text("Saving...")
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Save Name")
                            }
                        }
                    }
                }
            }

            if (uiState.nameSaveSuccess) {
                AlertDialog(
                    onDismissRequest = viewModel::dismissNameChangedDialog,
                    title = { Text("Success") },
                    text = { Text("Name changed successfully.") },
                    confirmButton = {
                        TextButton(onClick = viewModel::dismissNameChangedDialog) {
                            Text("OK")
                        }
                    }
                )
            }


            // --- Data Access ---
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Data Access", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    ProfileItem("Earliest Parse Date", uiState.user?.startDate?.let { dateFormatter.format(it) } ?: "-")
                    ProfileItem("Expiry Date", uiState.user?.endDate?.let { dateFormatter.format(it) } ?: "-")
                    ProfileItem("Last Parsed Date", uiState.user?.lastParsedDate?.let { dateFormatter.format(it) } ?: "-")
                }
            }

            // --- Actions ---
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Actions", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.showClearDataDialog(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isClearingData
                    ) {
                        if (uiState.isClearingData) {
                            Text("Clearing data...")
                        } else {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Delete All Data")
                        }
                    }
                }
            }

            // --- Clear Data Dialog ---
            if (uiState.showClearDataDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.showClearDataDialog(false) },
                    title = { Text("Clear All Data?") },
                    text = { Text("This will delete all transactions & reset to new user mode.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.clearAllData(onComplete = onBack) // Navigate back after clearing
                            },
                            enabled = !uiState.isClearingData
                        ) { Text("Yes") }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.showClearDataDialog(false) }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}


@Composable
fun ProfileItem(label: String, value: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Preview
@Composable
fun ProfileScreenPreview() {
    val context = LocalContext.current
    val repository = ExpenseRepository.getInstance(context)
    ProfileScreen(repository = repository, onBack = {})
}
