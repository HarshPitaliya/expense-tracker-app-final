package com.expensetracker.data.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val isDefault: Boolean = false
) {
    companion object {
        fun getDefaultCategories(): List<Category> = listOf(
            Category("bills", "Bills", "receipt", true),
            Category("grocery", "Grocery", "shopping_cart", true),
            Category("dining", "Dining", "restaurant", true),
            Category("entertainment", "Entertainment", "movie", true),
            Category("shopping", "Shopping", "shopping_bag", true),
            Category("health", "Health", "local_hospital", true),
            Category("investment", "Investment", "trending_up", true),
            Category("transfer", "Transfer", "swap_horiz", true),
            Category("transport", "Transport", "directions_car", true),
            Category("utilities", "Utilities", "electrical_services", true),
            Category("education", "Education", "school", true),
            Category("personal_care", "Personal Care", "spa", true),
            Category("undefined", "Uncategorized", "help_outline", true)
        )
        
        fun getIconForCategory(iconName: String): ImageVector {
            return when (iconName) {
                "receipt" -> Icons.Default.Receipt
                "shopping_cart" -> Icons.Default.ShoppingCart
                "restaurant" -> Icons.Default.Restaurant
                "movie" -> Icons.Default.Movie
                "shopping_bag" -> Icons.Default.ShoppingBag
                "local_hospital" -> Icons.Default.LocalHospital
                "trending_up" -> Icons.AutoMirrored.Filled.TrendingUp
                "swap_horiz" -> Icons.Default.SwapHoriz
                "directions_car" -> Icons.Default.DirectionsCar
                "electrical_services" -> Icons.Default.ElectricalServices
                "school" -> Icons.Default.School
                "spa" -> Icons.Default.Spa
                "help_outline" -> Icons.AutoMirrored.Filled.HelpOutline
                "add" -> Icons.Default.Add
                "edit" -> Icons.Default.Edit
                "delete" -> Icons.Default.Delete
                "category" -> Icons.Default.Category
                else -> Icons.Default.Category
            }
        }
    }
}