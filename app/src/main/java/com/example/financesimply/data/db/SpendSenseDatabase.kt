package com.example.financesimply.data.db

import android.content.Context
import com.example.financesimply.data.db.dao.FavoriteOutletDao
import com.example.financesimply.data.db.dao.TransactionDao
import com.example.financesimply.data.db.dao.WatchedItemDao

/** Entry point kept for existing callers; data now lives in Supabase (PostgreSQL), not local Room/SQLite. */
class SpendSenseDatabase private constructor() {

    private val transactions = TransactionDao()
    private val outlets = FavoriteOutletDao()
    private val watched = WatchedItemDao()

    fun transactionDao(): TransactionDao = transactions
    fun favoriteOutletDao(): FavoriteOutletDao = outlets
    fun watchedItemDao(): WatchedItemDao = watched

    companion object {
        @Volatile
        private var INSTANCE: SpendSenseDatabase? = null

        @Suppress("UNUSED_PARAMETER")
        fun getInstance(context: Context): SpendSenseDatabase =
            INSTANCE ?: synchronized(this) { INSTANCE ?: SpendSenseDatabase().also { INSTANCE = it } }
    }
}
