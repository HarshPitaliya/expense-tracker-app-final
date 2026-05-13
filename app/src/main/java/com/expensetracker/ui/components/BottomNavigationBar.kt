package com.expensetracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route


    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Overview") },
            label = { Text("Overview") },
            selected = currentRoute == "overview",
            onClick = {
                navController.navigate("overview") {
                    popUpTo("overview") { inclusive = true }
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Add, contentDescription = "Add transaction") },
            label = { Text("Add Transaction") },
            selected = currentRoute == "add_transaction",
            onClick = {
                navController.navigate("add_transaction")
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Analytics, contentDescription = "Historical Trends") },
            label = { Text("Analytics") },
            selected = currentRoute == "historical",
            onClick = {
                navController.navigate("historical") {
                    popUpTo("overview")
                }
            }
        )
    }
}