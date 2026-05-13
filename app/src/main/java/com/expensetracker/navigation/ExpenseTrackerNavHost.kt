package com.expensetracker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.expensetracker.data.repository.ExpenseRepository
import com.expensetracker.ui.profile.ProfileScreen
import com.expensetracker.ui.setup.InitialSetupScreen


@Composable
fun ExpenseTrackerNavHost() {
    val context = LocalContext.current
    val repository = remember { ExpenseRepository.getInstance(context) }
    val navController = rememberNavController()
    val isLoading by repository.isLoading.collectAsState()

    if (isLoading) {
        // Display Loading Indicator or splash UI
    } else {
        val userState by repository.user.collectAsState()
        val startDestination =
            if (userState == null || userState!!.isFirstTimeUser) "setup" else "main"

        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable("setup") {
                InitialSetupScreen(
                    repository = repository,
                    onSetupComplete = {
                        // Optional explicit navigation; session stays authenticated
                        navController.navigate("main") {
                            popUpTo("setup") { inclusive = true }
                        }
                    }
                )
            }
            composable("profile") {
                ProfileScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("main") {
                DashboardContainer(
                    outerNavController = navController,
                    repository = repository
                )
            }
        }
    }
}