package com.example.financesimply.data.db.dao

import com.example.financesimply.data.db.entity.FavoriteOutletEntity
import com.example.financesimply.data.remote.SupabaseClient as S
import kotlinx.coroutines.flow.Flow

/** Supabase-backed outlets: merchants + merchant_aliases exposed through v_android_outlets. */
class FavoriteOutletDao {

    suspend fun insert(outlet: FavoriteOutletEntity): Long = S.safe(-1L) {
        S.rpc("upsert_outlet", mapOf("p" to outlet), notify = true).trim().toLong()
    }

    suspend fun update(outlet: FavoriteOutletEntity) { insert(outlet) }

    suspend fun delete(outlet: FavoriteOutletEntity) {
        S.safe(Unit) { S.rpc("delete_outlet", mapOf("p_id" to outlet.id), notify = true); Unit }
    }

    fun getAll(): Flow<List<FavoriteOutletEntity>> =
        S.watch { S.getList<FavoriteOutletEntity>("v_android_outlets?order=friendlyName.asc") }

    suspend fun getAllSync(): List<FavoriteOutletEntity> = S.safe(emptyList()) { S.getList("v_android_outlets") }

    suspend fun findMatchingOutlet(merchantRaw: String): FavoriteOutletEntity? = S.safe(null) {
        S.rpcList<FavoriteOutletEntity>("find_matching_outlet", mapOf("p_raw" to merchantRaw)).firstOrNull()
    }
}
