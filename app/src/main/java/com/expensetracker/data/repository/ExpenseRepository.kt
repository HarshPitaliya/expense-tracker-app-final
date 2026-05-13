package com.expensetracker.data.repository

import android.content.Context
import com.expensetracker.data.models.Budget
import com.expensetracker.data.models.Category
import com.expensetracker.data.models.Transaction
import com.expensetracker.data.models.TransactionOriginalData
import com.expensetracker.data.models.User
import com.expensetracker.data.storage.EncryptedStorage
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

class ExpenseRepository private constructor(context: Context) {

    private val storage = EncryptedStorage(context)
    private val mutex = Mutex()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _categories = MutableStateFlow(Category.getDefaultCategories())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        _isLoading.value = true
        try {
            kotlinx.coroutines.runBlocking {
                // ✅ CORRECTED: The type <User> is inferred from the TypeToken, and the function
                // returns User? by default, which correctly matches the _user.value type.
                val loadedUser = storage.loadData("user", object : TypeToken<User>() {})
                if (loadedUser != null) _user.value = loadedUser

                val loadedTransactions = storage.loadData(
                    "transactions", object : TypeToken<List<Transaction>>() {})
                if (loadedTransactions != null) _transactions.value = loadedTransactions

                val loadedCategories = storage.loadData(
                    "categories", object : TypeToken<List<Category>>() {})
                if (loadedCategories != null) _categories.value = loadedCategories
                else _categories.value = Category.getDefaultCategories()

                val loadedBudgets = storage.loadData(
                    "budgets", object : TypeToken<List<Budget>>() {})
                if (loadedBudgets != null) _budgets.value = loadedBudgets
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isLoading.value = false
        }
    }

    // ---------------- User Ops ----------------
    fun getUser(): User? = _user.value

    suspend fun updateLastParsedDate(date: Date) = withContext(Dispatchers.IO) {
        mutex.withLock {
            _user.value?.let {
                val updated = it.copy(lastParsedDate = date, updatedAt = Date())
                _user.value = updated
                storage.saveData("user", updated)
            }
        }
    }

    suspend fun completeUserSetup(name: String?, startDate: Date, endDate: Date) =
        mutex.withLock {
            val currentUser = _user.value ?: User()
            val updatedUser = currentUser.copy(
                name = name,
                startDate = startDate,
                endDate = endDate,
                isFirstTimeUser = false,
                updatedAt = Date()
            )
            _user.value = updatedUser
            storage.saveData("user", updatedUser)
        }

    suspend fun updateUserName(newName: String): Boolean = withContext(Dispatchers.IO) {
        if (newName.isBlank() || newName.length > 50) return@withContext false
        mutex.withLock {
            val current = _user.value ?: return@withContext false
            val updated = current.copy(name = newName, updatedAt = Date())
            _user.value = updated
            storage.saveData("user", updated)
            return@withContext true
        }
    }

    // ---------------- Transaction Ops ----------------

    private fun isDuplicateTransaction(new: Transaction): Boolean {
        return _transactions.value.any {
            it.smsSender == new.smsSender &&
                    it.amount == new.amount &&
                    it.date == new.date
        }
    }

    fun getAllTransactionsForCategorization(): List<Pair<String, String>> {
        return _transactions.value
            .filter { it.categoryId != null }
            .map { Pair(it.getDisplayName(), it.categoryId!!) }
    }


    suspend fun addTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!isDuplicateTransaction(transaction)) {
                val updated = _transactions.value + transaction
                _transactions.value = updated
                storage.saveData("transactions", updated)
            }
        }
    }

    suspend fun deleteTransaction(transactionId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val updated = _transactions.value.filter { it.id != transactionId }
            _transactions.value = updated
            storage.saveData("transactions", updated)
        }
    }

    suspend fun renameTransaction(transactionId: String, newName: String): Boolean =
        withContext(Dispatchers.IO) {
            if (newName.length > 40 || newName.isBlank()) return@withContext false
            if (!Regex("^[A-Za-z0-9 ]+$").matches(newName)) return@withContext false

            mutex.withLock {
                val tx =
                    _transactions.value.find { it.id == transactionId } ?: return@withContext false
                val original = tx.originalData ?: TransactionOriginalData(
                    originalName = tx.derivedName,
                    originalCategoryId = tx.categoryId,
                    originalIsExpense = tx.isExpense
                )
                val updatedTx = tx.copy(
                    userOverwrittenName = newName,
                    originalData = original,
                    isUndoAvailable = true
                )
                _transactions.value =
                    _transactions.value.map { if (it.id == tx.id) updatedTx else it }
                storage.saveData("transactions", _transactions.value)
                return@withContext true
            }
        }

    suspend fun changeCategoryForTransaction(transactionId: String, categoryId: String?) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val tx = _transactions.value.find { it.id == transactionId } ?: return@withContext
                val original = tx.originalData ?: TransactionOriginalData(
                    originalName = tx.derivedName,
                    originalCategoryId = tx.categoryId,
                    originalIsExpense = tx.isExpense
                )
                val updatedTx = tx.copy(
                    categoryId = categoryId,
                    originalData = original,
                    isUndoAvailable = true
                )
                _transactions.value =
                    _transactions.value.map { if (it.id == tx.id) updatedTx else it }
                storage.saveData("transactions", _transactions.value)
            }
        }

    suspend fun toggleTransactionExpenseStatus(transactionId: String) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val tx = _transactions.value.find { it.id == transactionId } ?: return@withContext
                val original = tx.originalData ?: TransactionOriginalData(
                    originalName = tx.derivedName,
                    originalCategoryId = tx.categoryId,
                    originalIsExpense = tx.isExpense
                )
                val updatedTx = tx.copy(
                    isExpense = !tx.isExpense,
                    originalData = original,
                    isUndoAvailable = true
                )
                _transactions.value =
                    _transactions.value.map { if (it.id == tx.id) updatedTx else it }
                storage.saveData("transactions", _transactions.value)
            }
        }

    fun searchTransactions(query: String): List<Transaction> {
        if (query.isBlank()) return _transactions.value
        val queryLower = query.lowercase()
        return _transactions.value.filter { transaction ->
            val displayName = transaction.getDisplayName()
            displayName.lowercase().contains(queryLower) ||
                    transaction.amount.toString().contains(query) ||
                    getCategoryById(transaction.categoryId ?: "")
                        ?.name?.lowercase()?.contains(queryLower) == true ||
                    transaction.smsSender.lowercase().contains(queryLower)
        }
    }

    fun getTransactionsForMonth(month: Int, year: Int) = _transactions.value.filter {
        Calendar.getInstance().apply { time = it.date }.let { c ->
            c.get(Calendar.MONTH) + 1 == month && c.get(Calendar.YEAR) == year
        }
    }

    private fun getExpensesForMonth(month: Int, year: Int) =
        getTransactionsForMonth(month, year).filter { it.isExpense }

    fun getTotalExpenseForMonth(month: Int, year: Int) =
        getExpensesForMonth(month, year).sumOf { it.amount }

    fun getCategoryById(categoryId: String) = _categories.value.find { it.id == categoryId }

    // ---------------- Budget Ops ----------------

    suspend fun setBudget(budget: Budget) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val updated =
                _budgets.value.filterNot { it.month == budget.month && it.year == budget.year } + budget
            _budgets.value = updated
            storage.saveData("budgets", updated)
        }
    }

    fun getBudgetForMonth(month: Int, year: Int): Budget {
        return _budgets.value.find { it.month == month && it.year == year }
            ?: Budget(month = month, year = year, totalBudget = 200_000.0)
    }

    // ---------------- Analytics Ops ----------------

    fun getHistoricalMonthDetails(month: Int, year: Int): MonthlyAnalytics {
        val expenses = getExpensesForMonth(month, year)
        val income = getTransactionsForMonth(month, year).filter { !it.isExpense }
        val budget = getBudgetForMonth(month, year)
        val categoryBreakdown = expenses.groupBy { it.categoryId ?: "undefined" }
            .mapValues { it.value.sumOf { t -> t.amount } }

        return MonthlyAnalytics(
            month = month,
            year = year,
            totalTransactions = expenses.size + income.size,
            totalExpenses = expenses.sumOf { it.amount },
            totalIncome = income.sumOf { it.amount },
            totalBudget = budget.totalBudget,
            categoryBreakdown = categoryBreakdown,
            topTransactions = expenses.sortedByDescending { it.amount }.take(5),
            averageTransactionAmount = expenses.takeIf { it.isNotEmpty() }?.let {
                it.sumOf { tx -> tx.amount } / it.size
            } ?: 0.0,
            transactionCount = expenses.size
        )
    }

    // ---------------- Data Ops ----------------

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        try {
            mutex.withLock {
                _transactions.value = emptyList()
                _categories.value = Category.getDefaultCategories()
                _budgets.value = emptyList()
                _user.value = null
                storage.clearAllData()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    data class MonthlyAnalytics(
        val month: Int,
        val year: Int,
        val totalTransactions: Int,
        val totalExpenses: Double,
        val totalIncome: Double,
        val totalBudget: Double,
        val categoryBreakdown: Map<String, Double>,
        val topTransactions: List<Transaction>,
        val averageTransactionAmount: Double,
        val transactionCount: Int
    )

    companion object {
        @Volatile
        private var INSTANCE: ExpenseRepository? = null

        fun getInstance(context: Context): ExpenseRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = ExpenseRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
