package com.example.skybuddy.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skybuddy.data.repository.LayoutNode
import com.example.skybuddy.data.repository.MapLayout
import com.example.skybuddy.data.repository.MapRepository
import com.example.skybuddy.domain.pathfinding.AStarPathfinder
import com.example.skybuddy.domain.state.JourneyManager
import com.example.skybuddy.location.IndoorLocationManager
import com.example.skybuddy.ui.journey.JourneyPhase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val layout: MapLayout? = null,
    val currentFloor: Int = 0,
    val currentPath: List<LayoutNode> = emptyList(),
    val navigationStep: String = "",
    val currentX: Float = 500f,
    val currentY: Float = 900f,
    val currentHeading: Float = 0f
)

@HiltViewModel
class IndoorMapViewModel @Inject constructor(
    private val mapRepository: MapRepository,
    private val journeyManager: JourneyManager,
    private val indoorLocationManager: IndoorLocationManager
) : ViewModel() {

    private val pathfinder = AStarPathfinder()
    private val _internalState = MutableStateFlow(MapUiState())
    
    val uiState: StateFlow<MapUiState> = combine(
        _internalState,
        indoorLocationManager.currentX,
        indoorLocationManager.currentY,
        indoorLocationManager.currentHeading
    ) { state, x, y, heading ->
        state.copy(currentX = x, currentY = y, currentHeading = heading)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MapUiState()
    )

    init {
        loadMap()
        viewModelScope.launch {
            journeyManager.currentPhase.collectLatest { phase ->
                updatePathForPhase(phase)
            }
        }
    }

    private fun loadMap() {
        viewModelScope.launch {
            try {
                val layout = mapRepository.getMapLayout()
                _internalState.update { it.copy(layout = layout) }
                updatePathForPhase(journeyManager.currentPhase.value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updatePathForPhase(phase: JourneyPhase) {
        val layout = _internalState.value.layout ?: return
        var startId = ""
        var goalId = ""
        var stepText = ""
        var floor = 0

        when (phase) {
            JourneyPhase.HOME, JourneyPhase.AIRPORT_ENTRANCE -> {
                startId = "ENTRANCE"
                goalId = "CHECK_IN_A"
                stepText = "Step 1: Proceed to Baggage Drop."
                floor = 0
                indoorLocationManager.calibratePosition(500f, 900f)
            }
            JourneyPhase.BAGGAGE_DROP -> {
                startId = "CHECK_IN_A"
                goalId = "SECURITY_MAIN"
                stepText = "Step 2: Head to Security."
                floor = 0
                indoorLocationManager.calibratePosition(300f, 700f)
            }
            JourneyPhase.SECURITY_CHECKPOINT -> {
                startId = "SECURITY_EXIT"
                goalId = "GATE_1" // Just defaulting to Gate 1 for now
                stepText = "Step 3: Head to Gate 1."
                floor = 1
                indoorLocationManager.calibratePosition(500f, 800f)
            }
            JourneyPhase.GATE -> {
                startId = "GATE_1"
                goalId = "GATE_1"
                stepText = "You have arrived at your gate."
                floor = 1
                indoorLocationManager.calibratePosition(200f, 200f)
            }
        }

        if (startId == goalId) {
            _internalState.update { 
                it.copy(
                    currentFloor = floor, 
                    navigationStep = stepText, 
                    currentPath = emptyList()
                ) 
            }
        } else {
            val path = pathfinder.findPath(layout, floor, startId, goalId)
            _internalState.update { 
                it.copy(
                    currentFloor = floor, 
                    navigationStep = stepText, 
                    currentPath = path
                ) 
            }
        }
    }

    fun setLocation(x: Float, y: Float) {
        indoorLocationManager.calibratePosition(x, y)
    }

    fun simulateStep() {
        indoorLocationManager.onStepDetected()
    }
}
