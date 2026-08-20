package com.example.financesimply.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.financesimply.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getByDateRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun getPending(): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE status = 'PENDING'")
    suspend fun getPendingCount(): Int

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE transactionType = 'CREDIT' 
        AND status != 'DISMISSED'
        AND timestamp BETWEEN :startTime AND :endTime
    """)
    fun getMonthlyIncome(startTime: Long, endTime: Long): Flow<Double>


    @Query("""
        SELECT COALESCE(SUM(budgetImpactAmount), 0.0) FROM transactions 
        WHERE transactionType = 'DEBIT' 
        AND timestamp BETWEEN :startTime AND :endTime
    """)
    fun getMonthlyTotal(startTime: Long, endTime: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(budgetImpactAmount), 0.0) FROM transactions 
        WHERE transactionType = 'DEBIT' 
        AND timestamp BETWEEN :startTime AND :endTime
    """)
    suspend fun getMonthlyTotalSync(startTime: Long, endTime: Long): Double

    @Query("""
        SELECT category, SUM(budgetImpactAmount) as total FROM transactions 
        WHERE transactionType = 'DEBIT' 
        AND timestamp BETWEEN :startTime AND :endTime 
        AND category != '' 
        GROUP BY category 
        ORDER BY total DESC
    """)
    fun getCategoryBreakdown(startTime: Long, endTime: Long): Flow<List<CategoryTotal>>

    @Query("""
        SELECT merchantFriendly as merchant, SUM(budgetImpactAmount) as total, COUNT(*) as count 
        FROM transactions 
        WHERE transactionType = 'DEBIT' 
        AND timestamp BETWEEN :startTime AND :endTime 
        AND merchantFriendly != '' 
        GROUP BY merchantFriendly 
        ORDER BY total DESC 
        LIMIT 10
    """)
    fun getTopOutlets(startTime: Long, endTime: Long): Flow<List<MerchantTotal>>

    @Query("""
        SELECT * FROM transactions 
        WHERE (merchantFriendly = :value OR merchantRaw LIKE '%' || :value || '%' OR category = :value) 
        AND timestamp BETWEEN :startTime AND :endTime 
        ORDER BY timestamp DESC
    """)
    fun getByMerchantOrCategory(value: String, startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE (merchantFriendly = :value OR merchantRaw LIKE '%' || :value || '%' OR category = :value) 
        AND transactionType = 'DEBIT'
        AND timestamp BETWEEN :startTime AND :endTime
    """)
    suspend fun getFrequencyForItem(value: String, startTime: Long, endTime: Long): Int

    @Query("""
        SELECT COALESCE(SUM(budgetImpactAmount), 0.0) FROM transactions 
        WHERE (merchantFriendly = :value OR merchantRaw LIKE '%' || :value || '%' OR category = :value) 
        AND transactionType = 'DEBIT'
        AND timestamp BETWEEN :startTime AND :endTime
    """)
    suspend fun getSpendForItem(value: String, startTime: Long, endTime: Long): Double

    @Query("SELECT * FROM transactions WHERE notificationHash = :hash LIMIT 1")
    suspend fun findByNotificationHash(hash: String): TransactionEntity?

    @Query("""
        SELECT DISTINCT category FROM transactions 
        WHERE category != '' 
        ORDER BY category ASC
    """)
    fun getAllCategories(): Flow<List<String>>

    @Query("""
        SELECT DISTINCT tag FROM transactions 
        WHERE tag != '' 
        ORDER BY tag ASC
    """)
    fun getAllTags(): Flow<List<String>>

    @Query("""
        SELECT date(timestamp / 1000, 'unixepoch', 'localtime') as day, SUM(budgetImpactAmount) as total 
        FROM transactions 
        WHERE transactionType = 'DEBIT' 
        AND timestamp BETWEEN :startTime AND :endTime 
        GROUP BY day 
        ORDER BY day ASC
    """)
    fun getDailySpendTrend(startTime: Long, endTime: Long): Flow<List<DailySpend>>

    @Query("SELECT * FROM transactions WHERE splitDetails IS NOT NULL")
    fun getTransactionsWithSplits(): Flow<List<TransactionEntity>>
}

data class CategoryTotal(
    val category: String,
    val total: Double
)

data class MerchantTotal(
    val merchant: String,
    val total: Double,
    val count: Int
)

data class DailySpend(
    val day: String,
    val total: Double
)
