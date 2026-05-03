package com.example.skybuddy.ui.vault

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skybuddy.data.db.AppDatabase
import com.example.skybuddy.data.db.FlightEntity
import com.example.skybuddy.data.db.LuggageEntity
import com.example.skybuddy.data.db.ReceiptEntity
import com.example.skybuddy.data.repository.FlightRepository
import com.example.skybuddy.data.repository.LuggageRepository
import com.example.skybuddy.data.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class VaultUiState(
    val flights: List<FlightEntity> = emptyList(),
    val luggage: List<LuggageEntity> = emptyList(),
    val receipts: List<ReceiptEntity> = emptyList()
)

@HiltViewModel
class DatabaseViewerViewModel @Inject constructor(
    private val flightRepository: FlightRepository,
    private val luggageRepository: LuggageRepository,
    private val receiptRepository: ReceiptRepository,
    private val appDatabase: AppDatabase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(VaultUiState())
    val state: StateFlow<VaultUiState> = _state.asStateFlow()

    init {
        combine(
            flightRepository.observeUpcoming(),
            flightRepository.observePast(),
            luggageRepository.observeAll(),
            receiptRepository.observeAll()
        ) { upcoming, past, luggage, receipts ->
            VaultUiState(
                flights = upcoming + past,
                luggage = luggage,
                receipts = receipts
            )
        }.onEach { _state.value = it }.launchIn(viewModelScope)
    }

    fun clearLuggage() = viewModelScope.launch { luggageRepository.clear() }
    fun clearReceipts() = viewModelScope.launch { receiptRepository.clear() }

    fun clearAllData() = viewModelScope.launch(Dispatchers.IO) {
        // Clear Room DB
        appDatabase.clearAllTables()
        
        // Clear SharedPreferences
        val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory) {
            sharedPrefsDir.deleteRecursively()
        }

        // Clear files except model
        context.filesDir.listFiles()?.forEach { file ->
            if (file.name != com.example.skybuddy.ai.LiteRtLlmEngine.MODEL_FILE) {
                file.deleteRecursively()
            }
        }
        
        // Clear Cache
        context.cacheDir.deleteRecursively()
    }
}
