package com.example.financesimply.data.db.dao

import com.example.financesimply.data.db.entity.WatchedItemEntity
import com.example.financesimply.data.remote.SupabaseClient as S
import kotlinx.coroutines.flow.Flow

/** Supabase-backed watchlist (exclusive-arc table watched_items via v_android_watched). */
class WatchedItemDao {

    suspend fun insert(item: WatchedItemEntity): Long = S.safe(-1L) {
        S.rpc("add_watched", mapOf("p" to item), notify = true).trim().toLong()
    }

    suspend fun delete(item: WatchedItemEntity) {
        S.safe(Unit) { S.rpc("delete_watched", mapOf("p_id" to item.id), notify = true); Unit }
    }

    fun getAll(): Flow<List<WatchedItemEntity>> =
        S.watch { S.getList<WatchedItemEntity>("v_android_watched?order=value.asc") }

    suspend fun getAllSync(): List<WatchedItemEntity> = S.safe(emptyList()) { S.getList("v_android_watched") }
}
