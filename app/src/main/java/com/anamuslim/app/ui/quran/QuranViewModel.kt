package com.anamuslim.app.ui.quran

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anamuslim.app.core.ServiceLocator
import com.anamuslim.app.data.quran.QURAN_TOTAL_PAGES
import com.anamuslim.app.data.quran.QuranPageCache
import com.anamuslim.app.data.quran.QuranPageResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class QuranUiState(
    val currentPage: Int = 1,
    val pageData: QuranPageCache? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val autoSaveEnabled: Boolean = true
)

class QuranViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = ServiceLocator.quran(application)

    private val _uiState = MutableStateFlow(QuranUiState())
    val uiState: StateFlow<QuranUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val lastPage = repo.lastSavedPage.first()
            val autoSave = repo.autoSaveEnabled.first()
            _uiState.value = _uiState.value.copy(currentPage = lastPage, autoSaveEnabled = autoSave)
            loadPage(lastPage)
        }
    }

    fun goToPage(page: Int) {
        val safePage = page.coerceIn(1, QURAN_TOTAL_PAGES)
        loadPage(safePage)
    }

    fun nextPage() = goToPage(_uiState.value.currentPage + 1)
    fun previousPage() = goToPage(_uiState.value.currentPage - 1)

    private fun loadPage(page: Int) {
        _uiState.value = _uiState.value.copy(currentPage = page, isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = repo.getPage(page)) {
                is QuranPageResult.Success -> {
                    _uiState.value = _uiState.value.copy(pageData = result.page, isLoading = false)
                    repo.saveCurrentPage(page)
                    repo.prefetchAround(page)
                }
                is QuranPageResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }
}
