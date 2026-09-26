package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [HashEntity::class, CustomWordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ZeetsuDatabase : RoomDatabase() {
    abstract fun hashDao(): HashDao

    companion object {
        @Volatile
        private var INSTANCE: ZeetsuDatabase? = null

        fun getDatabase(context: Context): ZeetsuDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZeetsuDatabase::class.java,
                    "zeetsu_system.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
