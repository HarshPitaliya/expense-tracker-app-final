package com.expensetracker.data.storage

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptedStorage(private val context: Context) {
    private val gson = Gson()
    
    private fun getSecretKey(): SecretKey {
        val keyFile = File(context.filesDir, "encryption.key")
        
        return if (keyFile.exists()) {
            try {
                val keyBytes = Base64.decode(keyFile.readText(), Base64.DEFAULT)
                SecretKeySpec(keyBytes, "AES")
            } catch (_: Exception) {
                // If key is corrupted, generate new one
                generateAndSaveKey(keyFile)
            }
        } else {
            generateAndSaveKey(keyFile)
        }
    }

    private fun generateAndSaveKey(keyFile: File): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        val secretKey = keyGenerator.generateKey()
        
        val encodedKey = Base64.encodeToString(secretKey.encoded, Base64.DEFAULT)
        keyFile.writeText(encodedKey)
        
        return secretKey
    }
    
    private fun encrypt(data: String): Pair<String, String> {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data.toByteArray())
        
        return Pair(
            Base64.encodeToString(encryptedData, Base64.DEFAULT),
            Base64.encodeToString(iv, Base64.DEFAULT)
        )
    }
    
    private fun decrypt(encryptedData: String, iv: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val ivSpec = IvParameterSpec(Base64.decode(iv, Base64.DEFAULT))
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), ivSpec)
        
        val decryptedData = cipher.doFinal(Base64.decode(encryptedData, Base64.DEFAULT))
        return String(decryptedData)
    }

    // EncryptedStorage.kt

    suspend fun <T> saveData(filename: String, data: T?) = withContext(Dispatchers.IO) {
        try {
            if (data == null) {
                // If null is passed, delete stored data for this key
                clearKey(filename)
                return@withContext
            }
            val json = gson.toJson(data)
            val (encryptedData, iv) = encrypt(json)
            val file = File(context.filesDir, "$filename.enc")
            val ivFile = File(context.filesDir, "$filename.iv")
            file.writeText(encryptedData)
            ivFile.writeText(iv)
        } catch (e: Exception) {
            e.printStackTrace()
            throw StorageException("Failed to save data: ${e.message}")
        }
    }

    fun clearKey(key: String) {
        try {
            val enc = File(context.filesDir, "$key.enc")
            val iv = File(context.filesDir, "$key.iv")
            if (enc.exists()) enc.delete()
            if (iv.exists()) iv.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    suspend fun <T> loadData(filename: String, typeToken: TypeToken<T>): T? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, "$filename.enc")
            val ivFile = File(context.filesDir, "$filename.iv")
            
            if (!file.exists() || !ivFile.exists()) {
                return@withContext null
            }
            
            val encryptedData = file.readText()
            val iv = ivFile.readText()
            
            val decryptedJson = decrypt(encryptedData, iv)
            
            val type = typeToken.type
            gson.fromJson<T>(decryptedJson, type)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        try {
            val directory = context.filesDir
            directory.listFiles()?.forEach { file ->
                val name = file.name
                if (name.endsWith(".enc") || name.endsWith(".iv")) {
                    file.delete()
                }
            }
            // Ensure a true factory reset
            val keyFile = File(context.filesDir, "encryption.key")
            if (keyFile.exists()) keyFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class StorageException(message: String) : Exception(message)