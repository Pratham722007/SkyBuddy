package com.example.skybuddy.data.repository

import android.content.Context
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
data class MapLayout(
    val floors: List<FloorLayout>
)

@JsonClass(generateAdapter = true)
data class FloorLayout(
    val level: Int,
    val name: String,
    val paths: List<String>,
    val nodes: List<LayoutNode>,
    val edges: List<LayoutEdge>
)

@JsonClass(generateAdapter = true)
data class LayoutNode(
    val id: String,
    val type: String,
    val x: Float,
    val y: Float
)

@JsonClass(generateAdapter = true)
data class LayoutEdge(
    val from: String,
    val to: String,
    val distance: Float
)

@Singleton
class MapRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private var cachedLayout: MapLayout? = null

    suspend fun getMapLayout(): MapLayout = withContext(Dispatchers.IO) {
        cachedLayout?.let { return@withContext it }

        val jsonString = context.assets.open("surat_layout.json")
            .bufferedReader().use { it.readText() }

        val adapter = moshi.adapter(MapLayout::class.java)
        val layout = adapter.fromJson(jsonString) ?: throw IllegalStateException("Failed to parse layout")
        
        cachedLayout = layout
        layout
    }
}
