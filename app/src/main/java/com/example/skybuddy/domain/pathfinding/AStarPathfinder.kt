package com.example.skybuddy.domain.pathfinding

import android.util.Log
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.asAndroidPath
import com.example.skybuddy.data.repository.FloorLayout
import com.example.skybuddy.data.repository.LayoutNode
import com.example.skybuddy.data.repository.MapLayout
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.hypot

class AStarPathfinder {

    data class NodeWrapper(
        val node: LayoutNode,
        var gScore: Float = Float.MAX_VALUE,
        var fScore: Float = Float.MAX_VALUE,
        var previous: LayoutNode? = null
    ) : Comparable<NodeWrapper> {
        override fun compareTo(other: NodeWrapper): Int = fScore.compareTo(other.fScore)
    }

    fun findPath(layout: MapLayout, floorLevel: Int, startId: String, goalId: String): List<LayoutNode> {
        Log.d("AStar", "Finding path from $startId to $goalId on floor $floorLevel")
        val floor = layout.floors.find { it.level == floorLevel } ?: return emptyList()
        val startNode = floor.nodes.find { it.id == startId } ?: return emptyList()
        val goalNode = floor.nodes.find { it.id == goalId } ?: return emptyList()

        // 1. Create Collision Mask
        val mask = createCollisionMask(floor)
        
        // 2. Build Visibility Graph
        val visibilityGraph = buildVisibilityGraph(floor, mask)
        Log.d("AStar", "Visibility graph built. Start node neighbors: ${visibilityGraph[startId]?.size}")

        // 3. A* Search using dynamic graph
        val openSet = PriorityQueue<NodeWrapper>()
        val allNodes = floor.nodes.associateWith { NodeWrapper(it) }

        val startWrapper = allNodes[startNode]!!
        startWrapper.gScore = 0f
        startWrapper.fScore = heuristic(startNode, goalNode)
        openSet.add(startWrapper)

        while (openSet.isNotEmpty()) {
            val current = openSet.poll() ?: break

            if (current.node.id == goalId) {
                val path = reconstructPath(current, allNodes)
                Log.d("AStar", "Path found! Nodes: ${path.size}")
                return path
            }

            val neighbors = visibilityGraph[current.node.id] ?: emptyList()
            for (neighbor in neighbors) {
                val neighborWrapper = allNodes[neighbor]!!
                val tentativeGScore = current.gScore + distance(current.node, neighbor)

                if (tentativeGScore < neighborWrapper.gScore) {
                    neighborWrapper.previous = current.node
                    neighborWrapper.gScore = tentativeGScore
                    neighborWrapper.fScore = tentativeGScore + heuristic(neighbor, goalNode)
                    
                    if (!openSet.contains(neighborWrapper)) {
                        openSet.add(neighborWrapper)
                    }
                }
            }
        }
        Log.d("AStar", "No path found.")
        return emptyList()
    }

    private fun createCollisionMask(floor: FloorLayout): Bitmap {
        // Create an off-screen bitmap for rasterization
        // Using 2000x2000 to cover the max coordinates in our layout.
        val width = 2000
        val height = 2000
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Walkable background
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }

        floor.paths.forEach { layoutPath ->
            val path = PathParser().parsePathString(layoutPath.d).toPath().asAndroidPath()
            
            when (layoutPath.type) {
                "boundary" -> {
                    // Do not draw boundary on mask to avoid blocking nodes placed exactly on the edges
                }
                "wall" -> {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 10f // Reduced from thick boundary
                    canvas.drawPath(path, paint)
                }
                "island" -> {
                    // Solid filled shape
                    paint.style = Paint.Style.FILL
                    canvas.drawPath(path, paint)
                }
            }
        }
        return bitmap
    }

    private fun buildVisibilityGraph(floor: FloorLayout, mask: Bitmap): Map<String, List<LayoutNode>> {
        val graph = mutableMapOf<String, MutableList<LayoutNode>>()
        val nodes = floor.nodes

        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                val n1 = nodes[i]
                val n2 = nodes[j]

                // If line of sight exists, they are neighbors
                if (hasLineOfSight(mask, n1, n2)) {
                    graph.getOrPut(n1.id) { mutableListOf() }.add(n2)
                    graph.getOrPut(n2.id) { mutableListOf() }.add(n1)
                }
            }
        }
        return graph
    }

    private fun hasLineOfSight(mask: Bitmap, n1: LayoutNode, n2: LayoutNode): Boolean {
        // Bresenham's line algorithm checking for Color.BLACK (blocked pixels)
        var x0 = n1.x.toInt()
        var y0 = n1.y.toInt()
        val x1 = n2.x.toInt()
        val y1 = n2.y.toInt()

        val dx = abs(x1 - x0)
        val dy = abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx - dy

        val width = mask.width
        val height = mask.height

        while (true) {
            // Check bounds
            if (x0 in 0 until width && y0 in 0 until height) {
                // Read pixel (if black, we hit a wall)
                val pixelColor = mask.getPixel(x0, y0)
                // Color.BLACK is 0xFF000000.
                // We check if it's not exactly white
                if (pixelColor != Color.WHITE) {
                    return false
                }
            }

            if (x0 == x1 && y0 == y1) break
            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                x0 += sx
            }
            if (e2 < dx) {
                err += dx
                y0 += sy
            }
        }
        return true
    }

    private fun heuristic(a: LayoutNode, b: LayoutNode): Float {
        return hypot(a.x - b.x, a.y - b.y)
    }

    private fun distance(a: LayoutNode, b: LayoutNode): Float {
        return hypot(a.x - b.x, a.y - b.y)
    }

    private fun reconstructPath(current: NodeWrapper, allNodes: Map<LayoutNode, NodeWrapper>): List<LayoutNode> {
        val path = mutableListOf(current.node)
        var curr = current
        while (curr.previous != null) {
            val prevNode = curr.previous!!
            path.add(prevNode)
            curr = allNodes[prevNode]!!
        }
        path.reverse()
        return path
    }
}
