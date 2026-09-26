package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hash_records",
    indices = [
        Index(value = ["hashValue"]),
        Index(value = ["plainText", "algorithm"])
    ]
)
data class HashEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val plainText: String,
    val hashValue: String,
    val algorithm: String,
    val source: String, // "GENERATED", "DECRYPTED", "DICTIONARY"
    val salt: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

@Entity(
    tableName = "custom_words",
    indices = [Index(value = ["word"], unique = true)]
)
data class CustomWordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val word: String,
    val addedAt: Long = System.currentTimeMillis()
)
