package com.anamuslim.app.ui.tasbih

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anamuslim.app.core.ServiceLocator
import com.anamuslim.app.data.tasbih.TasbihCounter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TasbihViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = ServiceLocator.tasbih(application)

    val counters: StateFlow<List<TasbihCounter>> = repo.allCounters.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun increment(id: String) = viewModelScope.launch { repo.increment(id) }
    fun resetCount(id: String) = viewModelScope.launch { repo.resetCount(id) }
    fun addCustom(name: String) = viewModelScope.launch { if (name.isNotBlank()) repo.addCustom(name.trim()) }
    fun delete(id: String) = viewModelScope.launch { repo.delete(id) }
    fun rename(id: String, newName: String) = viewModelScope.launch { if (newName.isNotBlank()) repo.rename(id, newName.trim()) }
}
