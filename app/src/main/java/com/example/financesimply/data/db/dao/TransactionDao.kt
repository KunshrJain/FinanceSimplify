package com.example.financesimply.data.db.dao

import com.example.financesimply.data.db.entity.TransactionEntity
import com.example.financesimply.data.remote.SupabaseClient as S
import kotlinx.coroutines.flow.Flow

/** Supabase-backed replacement for the former Room DAO (same method names and signatures). */
class TransactionDao {

    private fun range(s: Long, e: Long) = mapOf("p_start" to s, "p_end" to e)
    private fun item(v: String, s: Long, e: Long) = mapOf("p_value" to v, "p_start" to s, "p_end" to e)
    private suspend fun list(q: String) = S.getList<TransactionEntity>("v_android_txn?$q")

    /** Returns -1 when the write could not reach Supabase. */
    suspend fun insert(transaction: TransactionEntity): Long = S.safe(-1L) {
        S.rpc("upsert_transaction", mapOf("p" to transaction), notify = true).trim().toLong()
    }

    suspend fun update(transaction: TransactionEntity) { insert(transaction) }

    fun getAll(): Flow<List<TransactionEntity>> = S.watch { list("order=timestamp.desc") }

    suspend fun getById(id: Long): TransactionEntity? = S.safe(null) { list("id=eq.$id&limit=1").firstOrNull() }

    fun getByDateRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> =
        S.watch { list("timestamp=gte.$startTime&timestamp=lte.$endTime&order=timestamp.desc") }

    fun getPending(): Flow<List<TransactionEntity>> = S.watch { list("status=eq.PENDING&order=timestamp.desc") }

    suspend fun getPendingCount(): Int = S.safe(0) { list("status=eq.PENDING&select=id").size }

    fun getMonthlyIncome(startTime: Long, endTime: Long): Flow<Double> =
        S.watch { S.rpc("monthly_income", range(startTime, endTime)).trim().toDouble() }

    fun getMonthlyTotal(startTime: Long, endTime: Long): Flow<Double> =
        S.watch { getMonthlyTotalSync(startTime, endTime) }

    suspend fun getMonthlyTotalSync(startTime: Long, endTime: Long): Double = S.safe(0.0) {
        S.rpc("monthly_total", range(startTime, endTime)).trim().toDouble()
    }

    fun getCategoryBreakdown(startTime: Long, endTime: Long): Flow<List<CategoryTotal>> =
        S.watch { S.rpcList<CategoryTotal>("category_breakdown", range(startTime, endTime)) }

    fun getTopOutlets(startTime: Long, endTime: Long): Flow<List<MerchantTotal>> =
        S.watch { S.rpcList<MerchantTotal>("top_outlets", range(startTime, endTime)) }

    fun getByMerchantOrCategory(value: String, startTime: Long, endTime: Long): Flow<List<TransactionEntity>> =
        S.watch { S.rpcList<TransactionEntity>("txn_by_item", item(value, startTime, endTime)) }

    suspend fun getFrequencyForItem(value: String, startTime: Long, endTime: Long): Int = S.safe(0) {
        S.rpc("frequency_for_item", item(value, startTime, endTime)).trim().toInt()
    }

    suspend fun getSpendForItem(value: String, startTime: Long, endTime: Long): Double = S.safe(0.0) {
        S.rpc("spend_for_item", item(value, startTime, endTime)).trim().toDouble()
    }

    suspend fun findByNotificationHash(hash: String): TransactionEntity? = S.safe(null) {
        list("notificationHash=eq.${S.enc(hash)}&limit=1").firstOrNull()
    }

    fun getAllCategories(): Flow<List<String>> = S.watch { S.rpcList<String>("all_categories") }

    fun getAllTags(): Flow<List<String>> = S.watch { S.rpcList<String>("all_tags") }

    fun getDailySpendTrend(startTime: Long, endTime: Long): Flow<List<DailySpend>> =
        S.watch { S.rpcList<DailySpend>("daily_spend_trend", range(startTime, endTime)) }

    fun getTransactionsWithSplits(): Flow<List<TransactionEntity>> =
        S.watch { list("splitDetails=not.is.null&order=timestamp.desc") }

    fun getAllMerchantNames(): Flow<List<String>> = S.watch { S.rpcList<String>("all_merchant_names") }
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
