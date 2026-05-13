package com.expensetracker.data.ml

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class SMSClassifier {

    // Known SMS senders from the provided list
    private val knownSenders = setOf(
        "VM-HDFCBANK-S", "AD-ICICIT-S", "JM-Pluxee-S", "AX-AXISBK-S",
        "JD-ICICIT-S", "JD-HDFCBANK-S", "VK-AXISBK-S", "VD-Pluxee-S",
        "JM-ICICIT-S", "JX-ICICIT-S", "VM-Pluxee-S", "CP-AXISBK-S",
        "JX-HDFCBK-S", "AX-HDFCBK-S", "JD-AXISBK-S", "AD-HDFCBK-S"
    )

    // Transaction keywords for ML classification
    private val transactionKeywords = setOf(
        "spent", "debited", "debit", "paid", "payment", "transaction",
        "withdrawal", "withdraw", "purchase", "bought", "rs", "inr",
        "amount", "balance", "bal", "avl", "available", "credited",
        "credit", "transfer", "sent", "received", "upi", "card",
        "bank", "account", "acc", "mandate", "autopay", "neft", "rtgs"
    )

    // Bank-specific patterns
    private val bankPatterns = mapOf(
        "HDFC" to setOf("hdfcbank", "hdfc bank", "hdfcbk"),
        "ICICI" to setOf("icicit", "icici bank", "icici"),
        "AXIS" to setOf("axisbk", "axis bank", "axis"),
        "SBI" to setOf("sbi", "state bank"),
        "KOTAK" to setOf("kotak", "kotak bank"),
        "PLUXEE" to setOf("pluxee", "sodexo")
    )

    suspend fun isTransactionSMS(sender: String, messageBody: String): Boolean = withContext(Dispatchers.Default) {
        // First check if sender is in known list
        if (knownSenders.contains(sender)) {
            return@withContext true
        }

        // Check if sender matches bank patterns
        val senderLower = sender.lowercase()
        val isBankSender = bankPatterns.values.any { patterns ->
            patterns.any { pattern -> senderLower.contains(pattern) }
        }

        if (!isBankSender) {
            return@withContext false
        }

        // Use ML-like classification based on keywords and patterns
        val messageWords = messageBody.lowercase().split("\\s+".toRegex())
        val keywordCount = messageWords.count { word ->
            transactionKeywords.any { keyword -> word.contains(keyword) }
        }

        // Check for amount pattern and banking context
        val hasAmountPattern = hasAmountPattern(messageBody)
        val hasBankingContext = hasBankingContext(messageBody)
        val hasTransactionAction = hasTransactionAction(messageBody)

        // ML scoring: combine multiple factors
        val score = calculateTransactionScore(keywordCount, hasAmountPattern, hasBankingContext, hasTransactionAction)
        return@withContext score >= 0.7f // 70% confidence threshold
    }

    private fun calculateTransactionScore(
        keywordCount: Int,
        hasAmountPattern: Boolean,
        hasBankingContext: Boolean,
        hasTransactionAction: Boolean
    ): Float {
        var score = 0f

        // Keyword score (max 0.3)
        score += minOf(keywordCount * 0.1f, 0.3f)

        // Amount pattern (0.4)
        if (hasAmountPattern) score += 0.4f

        // Banking context (0.2)
        if (hasBankingContext) score += 0.2f

        // Transaction action (0.1)
        if (hasTransactionAction) score += 0.1f

        return minOf(score, 1.0f)
    }

    suspend fun parseTransactionDetails(messageBody: String): TransactionData? = withContext(Dispatchers.Default) {
        try {
            val amount = extractAmount(messageBody)
            val merchantName = extractMerchantName(messageBody)
            val transactionType = identifyTransactionType(messageBody)

            if (amount != null && merchantName != null) {
                TransactionData(
                    amount = amount,
                    merchantName = merchantName,
                    description = messageBody,
                    transactionType = transactionType,
                    isDebit = isDebitTransaction(messageBody)
                )
            } else {
                println("DEBUG: Parse failed - amount: $amount, merchant: $merchantName")
                null
            }
        } catch (e: Exception) {
            println("DEBUG: Parse exception: ${e.message}")
            null
        }
    }

    private fun hasAmountPattern(message: String): Boolean {
        val amountPatterns = listOf(
            "rs\\s*\\d+",
            "inr\\s*\\d+",
            "\\d+\\.\\d+",
            "\\d+,\\d+",
            "₹\\s*\\d+",
            "spent.*rs",
            "debited.*rs",
            "credited.*rs"
        )

        return amountPatterns.any { pattern ->
            Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(message).find()
        }
    }

    private fun hasBankingContext(message: String): Boolean {
        val bankingTerms = setOf(
            "bank", "card", "account", "upi", "debit", "credit",
            "transaction", "payment", "balance", "transferred",
            "avl", "lmt", "mandate", "autopay", "neft", "rtgs"
        )

        val messageWords = message.lowercase().split("\\s+".toRegex())
        return messageWords.any { word ->
            bankingTerms.any { term -> word.contains(term) }
        }
    }

    private fun hasTransactionAction(message: String): Boolean {
        val actionTerms = setOf(
            "spent", "debited", "credited", "sent", "received",
            "paid", "withdrawn", "transferred", "purchased"
        )

        val messageLower = message.lowercase()
        return actionTerms.any { term -> messageLower.contains(term) }
    }

    private fun extractAmount(message: String): Double? {
        val amountPatterns = listOf(
            // ✅ FIXED: Better patterns for your SMS formats
            "rs\\.?\\s*(\\d+(?:,\\d+)*(?:\\.\\d+)?)",
            "inr\\s*(\\d+(?:,\\d+)*(?:\\.\\d+)?)",
            "₹\\s*(\\d+(?:,\\d+)*(?:\\.\\d+)?)",
            "debited\\s+rs\\.?\\s*(\\d+(?:,\\d+)*(?:\\.\\d+)?)",
            "spent\\s+rs\\.?\\s*(\\d+(?:,\\d+)*(?:\\.\\d+)?)",
            "credited\\s+rs\\.?\\s*(\\d+(?:,\\d+)*(?:\\.\\d+)?)"
        )

        for (pattern in amountPatterns) {
            val matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(message)
            if (matcher.find()) {
                // Use safe call ?.let to execute code only if matcher.group(1) is not null
                matcher.group(1)?.let { capturedGroup -> // 'it' or 'capturedGroup' will be non-null here
                    val amountStr = capturedGroup.replace(",", "")
                    val amount = amountStr.toDoubleOrNull()
                    println("DEBUG: Amount pattern '$pattern' matched: $amountStr -> $amount")
                    // If amount is not null after parsing, return it.
                    // This ensures we only return if a valid double was parsed.
                    if (amount != null) {
                        return amount
                    }
                    // If amount is null here, it means toDoubleOrNull failed,
                    // so we continue to the next pattern.
                }
                // If matcher.group(1) was null, .let block is skipped, and we continue to the next pattern.
            }
        }
        println("DEBUG: No amount pattern matched for: $message")
        return null
    }

    private fun extractMerchantName(message: String): String? {
        // ✅ FIXED: Patterns that work with your actual SMS formats
        val merchantPatterns = listOf(
            // For "Spent Rs.20000 On HDFC Bank Card 9883 At GYFTRSM12..."
            "at\\s+([A-Z0-9]+[A-Z0-9\\s]*?)(?:\\s+on|\\.|$)",

            // For basic account debit messages, extract a generic name from context
            "(?:ICICI|HDFC|AXIS)\\s+Bank\\s+Acc\\s+[X0-9]+\\s+debited.*?on\\s+\\d+-\\w+.*?(?:at\\s+([A-Z0-9\\s]+?)\\s*[\\.,])?",

            // Generic patterns for merchant extraction
            "(?:spent|debited|paid).*?(?:at|to)\\s+([A-Z][A-Z0-9\\s]{2,20})(?:\\s+on|\\.|$)",

            // UPI and other patterns
            "to\\s+([A-Z][A-Z0-9\\s]{2,20})\\s+(?:via|using|on)",

            // Card transaction patterns
            "card\\s+\\d+\\s+at\\s+([A-Z0-9\\s]{3,20})(?:\\s+on|\\.|$)"
        )

        for (pattern in merchantPatterns) {
            val matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE or Pattern.MULTILINE).matcher(message)
            if (matcher.find() && matcher.groupCount() >= 1) {
                val merchantName = matcher.group(1)?.trim()
                if (merchantName != null && merchantName.isNotBlank() && merchantName.length > 2 && isValidMerchantName(merchantName)) {
                    val cleaned = cleanMerchantName(merchantName)
                    println("DEBUG: Merchant pattern '$pattern' matched: '$merchantName' -> '$cleaned'")
                    return cleaned
                }
            }
        }

        // ✅ FALLBACK: For basic debit messages without specific merchant info
        val messageLower = message.lowercase()
//        return when {
//            messageLower.contains("icici") -> "ICICI Bank Transaction"
//            messageLower.contains("hdfc") -> "HDFC Bank Transaction"
//            messageLower.contains("axis") -> "Axis Bank Transaction"
//            messageLower.contains("card") -> "Card Transaction"
//            else -> "Bank Transaction"
//        }

        return when {
            messageLower.contains("icici") -> "ICICI Bank"
            messageLower.contains("hdfc") -> "HDFC Bank"
            messageLower.contains("axis") -> "Axis Bank"
            messageLower.contains("card") -> "Card"
            else -> "Bank"
        }
    }

    private fun isValidMerchantName(name: String): Boolean {
        // Filter out common false positives
        val invalidPatterns = setOf(
            "bank", "account", "card", "block", "call", "sms", "not you",
            "dispute", "balance", "avl", "lmt", "ref", "rrn", "acc", "on"
        )

        val nameLower = name.lowercase()
        return !invalidPatterns.any { pattern -> nameLower.contains(pattern) }
    }

    private fun cleanMerchantName(name: String): String {
        return name
            .replace(Regex("[^A-Za-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(40) // Limit to 40 characters as per requirement
    }

    private fun identifyTransactionType(message: String): String {
        val messageLower = message.lowercase()
        return when {
            messageLower.contains("upi") -> "UPI"
            messageLower.contains("card") -> "Card"
            messageLower.contains("neft") -> "NEFT"
            messageLower.contains("rtgs") -> "RTGS"
            messageLower.contains("autopay") -> "AutoPay"
            messageLower.contains("mandate") -> "Mandate"
            else -> "Other"
        }
    }

    private fun isDebitTransaction(message: String): Boolean {
        val messageLower = message.lowercase()
        val debitTerms = setOf("spent", "debited", "debit", "withdrawn", "paid")
        val creditTerms = setOf("credited", "credit", "received")

        val hasDebitTerms = debitTerms.any { term -> messageLower.contains(term) }
        val hasCreditTerms = creditTerms.any { term -> messageLower.contains(term) }

        // If both present or neither present, assume debit (most common for expense tracking)
        return if (hasDebitTerms && !hasCreditTerms) true
        else if (!hasDebitTerms && hasCreditTerms) false
        else true // Default to debit for expense tracking
    }

    suspend fun suggestCategory(merchantName: String, existingTransactions: List<Pair<String, String>> = emptyList()): String? = withContext(Dispatchers.Default) {
        val merchantLower = merchantName.lowercase()

        // First check if we have a similar transaction with a category
        for ((existingMerchant, category) in existingTransactions) {
            if (isSimilarMerchant(merchantLower, existingMerchant.lowercase())) {
                return@withContext category
            }
        }

        // Use keyword-based categorization
        val categoryKeywords = mapOf(
            "grocery" to setOf("blinkit", "grofers", "bigbasket", "grocery", "supermarket", "dmart", "more", "reliance fresh"),
            "dining" to setOf("restaurant", "cafe", "food", "zomato", "swiggy", "dominos", "pizza", "kfc", "mcdonalds", "burger"),
            "entertainment" to setOf("movie", "cinema", "netflix", "spotify", "game", "sony pictures", "prime video", "hotstar"),
            "shopping" to setOf("amazon", "flipkart", "myntra", "shopping", "mall", "store", "retail"),
            "bills" to setOf("electricity", "gas", "water", "mobile", "internet", "broadband", "telecom", "jio", "airtel"),
            "health" to setOf("hospital", "medical", "pharmacy", "doctor", "health", "clinic", "apollo", "fortis"),
            "investment" to setOf("zerodha", "mutual", "sip", "investment", "trading", "broker", "stock"),
            "transfer" to setOf("transfer", "upi", "neft", "imps", "sent", "bank transaction"),
            "transport" to setOf("uber", "ola", "taxi", "metro", "bus", "petrol", "diesel", "fuel"),
            "utilities" to setOf("electricity", "water", "gas", "internet", "mobile", "recharge")
        )

        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { keyword -> merchantLower.contains(keyword) }) {
                return@withContext category
            }
        }

        return@withContext null
    }

    private fun isSimilarMerchant(merchant1: String, merchant2: String): Boolean {
        // Simple similarity check - can be enhanced with more sophisticated algorithms
        val words1 = merchant1.split(" ").filter { it.length > 2 }
        val words2 = merchant2.split(" ").filter { it.length > 2 }

        // Check if any significant words match
        return words1.any { word1 ->
            words2.any { word2 ->
                word1 == word2 || word1.contains(word2) || word2.contains(word1)
            }
        }
    }

    data class TransactionData(
        val amount: Double,
        val merchantName: String,
        val description: String,
        val transactionType: String = "Other",
        val isDebit: Boolean = true
    )
}
