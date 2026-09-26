package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HashDao {

    @Query("SELECT * FROM hash_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<HashEntity>>

    @Query("SELECT * FROM hash_records WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteRecords(): Flow<List<HashEntity>>

    @Query("SELECT * FROM hash_records WHERE LOWER(hashValue) = LOWER(:hash) LIMIT 1")
    suspend fun findByHash(hash: String): HashEntity?

    @Query("SELECT * FROM hash_records WHERE LOWER(hashValue) = LOWER(:hash) AND LOWER(algorithm) = LOWER(:algo) LIMIT 1")
    suspend fun findByHashAndAlgo(hash: String, algo: String): HashEntity?

    @Query("SELECT * FROM hash_records WHERE plainText = :text AND LOWER(algorithm) = LOWER(:algo) LIMIT 1")
    suspend fun findByTextAndAlgo(text: String, algo: String): HashEntity?

    @Query("SELECT * FROM hash_records WHERE plainText LIKE '%' || :query || '%' OR hashValue LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchRecords(query: String): Flow<List<HashEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: HashEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecords(records: List<HashEntity>)

    @Update
    suspend fun updateRecord(record: HashEntity)

    @Query("DELETE FROM hash_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("DELETE FROM hash_records")
    suspend fun clearAllRecords()

    // Custom Words / Dictionary expansion
    @Query("SELECT * FROM custom_words ORDER BY addedAt DESC")
    fun getAllCustomWords(): Flow<List<CustomWordEntity>>

    @Query("SELECT word FROM custom_words")
    suspend fun getAllCustomWordsList(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCustomWord(word: CustomWordEntity): Long

    @Query("DELETE FROM custom_words WHERE id = :id")
    suspend fun deleteCustomWord(id: Long)
}
