package com.example.skybuddy.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "map_nodes")
data class MapNodeEntity(
    @PrimaryKey val nodeId: String, // e.g., "STV_F1_STARBUCKS", "GATE_4"
    val type: String, // "SHOP", "CHECKPOINT", "GATE"
    val floor: Int,
    val mapX: Float,
    val mapY: Float
)
