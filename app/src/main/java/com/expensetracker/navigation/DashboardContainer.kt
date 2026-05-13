package com.expensetracker.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.expensetracker.data.repository.ExpenseRepository
import com.expensetracker.ui.analytics.AnalyticsScreen
import com.expensetracker.ui.components.BottomNavigationBar
import com.expensetracker.ui.components.ProfileHeader
import com.expensetracker.ui.overview.OverviewScreen
import com.expensetracker.ui.transactions.AddTransactionScreen
import com.expensetracker.ui.transactions.TransactionScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContainer(
    outerNavController: NavHostController, // Renamed for clarity
    repository: ExpenseRepository,
) {
    val dashboardNavController = rememberNavController()
    Scaffold(
        topBar = {
            ProfileHeader(
                repository = repository,
                onProfileClick = { outerNavController.navigate("profile") }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = dashboardNavController)
        }
    ) { paddingValues ->
        NavHost(
            navController = dashboardNavController,
            startDestination = "overview",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("overview") {
                OverviewScreen(
                    repository = repository,
                    onNavigateToTransactions = { dashboardNavController.navigate("transactions") }
                )
            }

            composable("add_transaction") {
                // REMOVED 'categories' parameter - it's now handled by AddTransactionViewModel
                AddTransactionScreen(
                    repository = repository,
                    onNavigateBack = { dashboardNavController.popBackStack() }
                )
            }


            composable("historical") {
                AnalyticsScreen()
            }

            composable("transactions") {
                TransactionScreen(
                    repository = repository,
                    onBack = { dashboardNavController.popBackStack() }
                )
            }
        }
    }
}