package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class HashRepository(private val hashDao: HashDao) {

    val allRecords: Flow<List<HashEntity>> = hashDao.getAllRecords()
    val favoriteRecords: Flow<List<HashEntity>> = hashDao.getFavoriteRecords()
    val customWords: Flow<List<CustomWordEntity>> = hashDao.getAllCustomWords()

    fun searchRecords(query: String): Flow<List<HashEntity>> {
        return hashDao.searchRecords(query)
    }

    suspend fun findByHash(hash: String): HashEntity? = withContext(Dispatchers.IO) {
        hashDao.findByHash(hash.trim())
    }

    suspend fun findByHashAndAlgo(hash: String, algo: String): HashEntity? = withContext(Dispatchers.IO) {
        hashDao.findByHashAndAlgo(hash.trim(), algo.trim())
    }

    suspend fun saveRecord(record: HashEntity): Long = withContext(Dispatchers.IO) {
        hashDao.insertRecord(record)
    }

    suspend fun updateRecord(record: HashEntity) = withContext(Dispatchers.IO) {
        hashDao.updateRecord(record)
    }

    suspend fun deleteRecord(id: Long) = withContext(Dispatchers.IO) {
        hashDao.deleteRecordById(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        hashDao.clearAllRecords()
    }

    suspend fun getCustomWords(): List<String> = withContext(Dispatchers.IO) {
        hashDao.getAllCustomWordsList()
    }

    suspend fun addCustomWord(word: String) = withContext(Dispatchers.IO) {
        if (word.isNotBlank()) {
            hashDao.insertCustomWord(CustomWordEntity(word = word.trim()))
        }
    }

    suspend fun deleteCustomWord(id: Long) = withContext(Dispatchers.IO) {
        hashDao.deleteCustomWord(id)
    }
}
