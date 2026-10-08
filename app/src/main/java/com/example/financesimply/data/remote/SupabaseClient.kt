package com.example.financesimply.data.remote

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** Minimal PostgREST client (single-user test project, anon key). */
object SupabaseClient {
    private const val BASE = "https://epsccrintpcctkvbaiin.supabase.co/rest/v1"
    private const val ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVwc2NjcmludHBjY3RrdmJhaWluIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQ1MjA3NTIsImV4cCI6MjEwMDA5Njc1Mn0.kVdE2aWuIoB7UgKtI8PJzTHHs1jQAtsCKcYM8sE9Q_E"
    private val JSON = "application/json".toMediaType()
    private const val POLL_MS = 5_000L

    val gson = Gson()
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()

    /** Emits after every local write so observing flows refresh immediately. */
    val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 8)

    fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    private fun request(path: String): Request.Builder = Request.Builder().url("$BASE/$path")
        .header("apikey", ANON_KEY).header("Authorization", "Bearer $ANON_KEY")

    private suspend fun exec(req: Request): String = withContext(Dispatchers.IO) {
        http.newCall(req).execute().use { r ->
            val body = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw java.io.IOException("Supabase ${r.code}: $body")
            body
        }
    }

    /** GET a table/view with a PostgREST query string, e.g. "v_android_txn?order=timestamp.desc". */
    suspend fun get(path: String): String = exec(request(path).get().build())

    /** Call a Postgres function. Writes should pass notify = true. */
    suspend fun rpc(fn: String, body: Any? = null, notify: Boolean = false): String {
        val json = if (body == null) "{}" else if (body is String) body else gson.toJson(body)
        val out = exec(request("rpc/$fn").post(json.toRequestBody(JSON)).build())
        if (notify) changes.tryEmit(Unit)
        return out
    }

    suspend inline fun <reified T> getList(path: String): List<T> =
        gson.fromJson(get(path), object : TypeToken<List<T>>() {}.type) ?: emptyList()

    suspend inline fun <reified T> rpcList(fn: String, body: Any? = null): List<T> =
        gson.fromJson(rpc(fn, body), object : TypeToken<List<T>>() {}.type) ?: emptyList()

    /** Network/server failures must not crash callers (the Room DAOs never threw); log and return [default]. */
    suspend inline fun <T> safe(default: T, block: () -> T): T = try {
        block()
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        android.util.Log.w("Supabase", e.message ?: e.toString())
        default
    }

    /** Re-queries on every local write and every few seconds; keeps last value when offline. */
    fun <T> watch(query: suspend () -> T): Flow<T> = channelFlow {
        suspend fun emitSafe() { try { send(query()) } catch (_: Exception) { } }
        emitSafe()
        launch { changes.collect { emitSafe() } }
        while (true) { delay(POLL_MS); emitSafe() }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)
}
