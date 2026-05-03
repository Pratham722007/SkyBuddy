package com.example.skybuddy.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skybuddy.shared.data.repository.LayoutNode
import com.example.skybuddy.shared.data.repository.MapLayout
import com.example.skybuddy.shared.data.repository.MapRepository
import com.example.skybuddy.shared.domain.pathfinding.AStarPathfinder
import com.example.skybuddy.shared.location.BlockedRegionManager
import com.example.skybuddy.shared.location.IndoorLocationManager
import com.example.skybuddy.domain.state.JourneyManager
import com.example.skybuddy.location.SOSBeaconEmitter
import com.example.skybuddy.ui.journey.JourneyPhase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
    val currentHeading: Float = 0f,
    val blockedNodeIds: Set<String> = emptySet(),
    val sosSent: Boolean = false
)

@HiltViewModel
class IndoorMapViewModel @Inject constructor(
    private val mapRepository: MapRepository,
    private val journeyManager: JourneyManager,
    private val indoorLocationManager: IndoorLocationManager,
    private val blockedRegionManager: BlockedRegionManager,
    private val sosBeaconEmitter: SOSBeaconEmitter
) : ViewModel() {

    private val pathfinder = AStarPathfinder()
    private val _internalState = MutableStateFlow(MapUiState())
    
    private var globalStartId: String = ""
    private var globalGoalId: String = ""
    private var randomGateId: String? = null
    
    val uiState: StateFlow<MapUiState> = combine(
        _internalState,
        indoorLocationManager.currentX,
        indoorLocationManager.currentY,
        indoorLocationManager.currentHeading,
        blockedRegionManager.blockedNodeIds
    ) { state, x, y, heading, blocked ->
        state.copy(
            currentX = x,
            currentY = y,
            currentHeading = heading,
            blockedNodeIds = blocked
        )
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
        // Re-route when blocked regions change
        viewModelScope.launch {
            blockedRegionManager.blockedNodeIds.collectLatest {
                recalculatePath()
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
        
        if (randomGateId == null) {
            val gates = layout.floors.flatMap { it.nodes }.filter { it.type == "GATE" }
            randomGateId = if (gates.isNotEmpty()) gates.random().id else "GATE_C1"
        }
        val targetGate = randomGateId!!

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
                globalGoalId = targetGate
                stepText = "Step 3: Head to Gate ${targetGate.replace("GATE_", "")}."
            }
            JourneyPhase.GATE -> {
                globalStartId = targetGate
                globalGoalId = targetGate
                stepText = "You have arrived at your gate."
            }
        }

        var startFloor = 1
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
        val goalId = globalGoalId

        if (globalStartId == globalGoalId || goalId.isEmpty()) {
            _internalState.update { it.copy(currentPath = emptyList()) }
            return
        }

        val startX = indoorLocationManager.currentX.value
        val startY = indoorLocationManager.currentY.value
        val blocked = blockedRegionManager.blockedNodeIds.value

        viewModelScope.launch(Dispatchers.Default) {
            val path = pathfinder.findPath(layout, currentFloor, startX, startY, goalId, blocked)
            _internalState.update { it.copy(currentPath = path) }
        }
    }

    fun setLocation(x: Float, y: Float) {
        indoorLocationManager.calibratePosition(x, y)
        recalculatePath()
    }

    fun simulateStep() {
        indoorLocationManager.onStepDetected()
        recalculatePath()
    }

    fun sendSOS(typeId: String) {
        val x = indoorLocationManager.currentX.value
        val y = indoorLocationManager.currentY.value
        sosBeaconEmitter.emitSOS(typeId, x, y)
        _internalState.update { it.copy(sosSent = true) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _internalState.update { it.copy(sosSent = false) }
        }
    }
}
