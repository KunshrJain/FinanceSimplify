package com.example.financesimply.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.financesimply.data.db.dao.FavoriteOutletDao
import com.example.financesimply.data.db.dao.TransactionDao
import com.example.financesimply.data.db.dao.WatchedItemDao
import com.example.financesimply.data.db.entity.FavoriteOutletEntity
import com.example.financesimply.data.db.entity.TransactionEntity
import com.example.financesimply.data.db.entity.WatchedItemEntity

@Database(
    entities = [
        TransactionEntity::class,
        FavoriteOutletEntity::class,
        WatchedItemEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(com.example.financesimply.data.db.entity.Converters::class)
abstract class SpendSenseDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun favoriteOutletDao(): FavoriteOutletDao
    abstract fun watchedItemDao(): WatchedItemDao

    companion object {
        @Volatile
        private var INSTANCE: SpendSenseDatabase? = null

        fun getInstance(context: Context): SpendSenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpendSenseDatabase::class.java,
                    "spendsense_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
