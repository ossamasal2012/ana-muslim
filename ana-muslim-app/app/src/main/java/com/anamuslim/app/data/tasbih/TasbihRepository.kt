package com.anamuslim.app.data.tasbih

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.tasbihDataStore by preferencesDataStore(name = "ana_muslim_tasbih")
private val TASBIH_LIST_KEY = stringPreferencesKey("tasbih_list_json")

@Serializable
data class TasbihCounter(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val count: Int = 0,
    val isDefault: Boolean = false
)

class TasbihRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private fun defaultCounters(names: List<String>) = names.map { TasbihCounter(name = it, isDefault = true) }

    /** التسبيحات الافتراضية العشر عند أول تشغيل للتطبيق (يحدَّد نصها من موارد اللغة عند أول استدعاء). */
    suspend fun ensureSeeded(defaultNames: List<String>) {
        context.tasbihDataStore.edit { prefs ->
            if (prefs[TASBIH_LIST_KEY] == null) {
                prefs[TASBIH_LIST_KEY] = json.encodeToString(defaultCounters(defaultNames))
            }
        }
    }

    val allCounters: Flow<List<TasbihCounter>> = context.tasbihDataStore.data.map { prefs ->
        val raw = prefs[TASBIH_LIST_KEY] ?: return@map emptyList()
        try {
            json.decodeFromString<List<TasbihCounter>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun updateList(transform: (List<TasbihCounter>) -> List<TasbihCounter>) {
        context.tasbihDataStore.edit { prefs ->
            val current = prefs[TASBIH_LIST_KEY]?.let {
                try { json.decodeFromString<List<TasbihCounter>>(it) } catch (e: Exception) { emptyList() }
            } ?: emptyList()
            prefs[TASBIH_LIST_KEY] = json.encodeToString(transform(current))
        }
    }

    suspend fun increment(id: String) = updateList { list ->
        list.map { if (it.id == id) it.copy(count = it.count + 1) else it }
    }

    suspend fun resetCount(id: String) = updateList { list ->
        list.map { if (it.id == id) it.copy(count = 0) else it }
    }

    suspend fun addCustom(name: String) = updateList { list ->
        list + TasbihCounter(name = name, isDefault = false)
    }

    suspend fun rename(id: String, newName: String) = updateList { list ->
        list.map { if (it.id == id) it.copy(name = newName) else it }
    }

    suspend fun delete(id: String) = updateList { list ->
        list.filterNot { it.id == id }
    }
}
