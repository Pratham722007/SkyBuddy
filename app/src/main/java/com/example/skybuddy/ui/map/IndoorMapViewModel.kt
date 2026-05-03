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
    
    private var globalStartId: String = ""
    private var globalGoalId: String = ""
    
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
        var stepText = ""

        when (phase) {
            JourneyPhase.HOME, JourneyPhase.AIRPORT_ENTRANCE -> {
                globalStartId = "ENTRANCE"
                globalGoalId = "BAGGAGE_DROP"
                stepText = "Step 1: Proceed to Baggage Drop."
            }
            JourneyPhase.BAGGAGE_DROP -> {
                globalStartId = "BAGGAGE_DROP"
                globalGoalId = "SECURITY_CHECK"
                stepText = "Step 2: Head to Security."
            }
            JourneyPhase.SECURITY_CHECKPOINT -> {
                globalStartId = "SECURITY_CHECK"
                globalGoalId = "GATE_4" // Gate 4 is on Floor 1
                stepText = "Step 3: Head to Gate 4. Use the lift."
            }
            JourneyPhase.GATE -> {
                globalStartId = "GATE_4"
                globalGoalId = "GATE_4"
                stepText = "You have arrived at your gate."
            }
        }

        var startFloor = 0
        var startX = 500f
        var startY = 900f
        
        layout.floors.forEach { floor ->
            val node = floor.nodes.find { it.id == globalStartId }
            if (node != null) {
                startFloor = floor.level
                startX = node.x
                startY = node.y
            }
        }
        
        indoorLocationManager.calibratePosition(startX, startY)
        
        _internalState.update { 
            it.copy(
                currentFloor = startFloor,
                navigationStep = stepText
            )
        }
        
        recalculatePath()
    }

    fun setFloor(floor: Int) {
        _internalState.update { it.copy(currentFloor = floor) }
        recalculatePath()
    }

    private fun recalculatePath() {
        val layout = _internalState.value.layout ?: return
        val currentFloor = _internalState.value.currentFloor
        val goalNodeFloor = layout.floors.find { f -> f.nodes.any { it.id == globalGoalId } }?.level ?: currentFloor
        val startNodeFloor = layout.floors.find { f -> f.nodes.any { it.id == globalStartId } }?.level ?: currentFloor

        if (globalStartId == globalGoalId) {
            _internalState.update { it.copy(currentPath = emptyList()) }
            return
        }

        val localStartId = if (currentFloor == startNodeFloor) globalStartId else if (currentFloor == 0) "LIFT_GF" else "LIFT_FF"
        val localGoalId = if (currentFloor == goalNodeFloor) globalGoalId else if (currentFloor == 0) "LIFT_GF" else "LIFT_FF"

        if (localStartId == localGoalId) {
             _internalState.update { it.copy(currentPath = emptyList()) }
             return
        }

        val path = pathfinder.findPath(layout, currentFloor, localStartId, localGoalId)
        _internalState.update { it.copy(currentPath = path) }
    }

    fun setLocation(x: Float, y: Float) {
        indoorLocationManager.calibratePosition(x, y)
    }

    fun simulateStep() {
        indoorLocationManager.onStepDetected()
    }
}
