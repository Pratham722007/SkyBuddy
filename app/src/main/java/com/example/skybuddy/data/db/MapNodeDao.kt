package com.example.skybuddy.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MapNodeDao {
    @Query("SELECT * FROM map_nodes")
    fun getAllNodes(): Flow<List<MapNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(nodes: List<MapNodeEntity>)
    
    @Query("SELECT * FROM map_nodes WHERE nodeId = :nodeId LIMIT 1")
    suspend fun getNodeById(nodeId: String): MapNodeEntity?
}
