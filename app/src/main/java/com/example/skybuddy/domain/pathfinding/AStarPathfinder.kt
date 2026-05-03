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
import kotlin.math.hypot

class AStarPathfinder {

    data class GridPoint(val x: Int, val y: Int)

    data class NodeWrapper(
        val point: GridPoint,
        var gScore: Float = Float.MAX_VALUE,
        var fScore: Float = Float.MAX_VALUE,
        var previous: GridPoint? = null
    ) : Comparable<NodeWrapper> {
        override fun compareTo(other: NodeWrapper): Int = fScore.compareTo(other.fScore)
    }

    fun findPath(layout: MapLayout, floorLevel: Int, startX: Float, startY: Float, goalId: String): List<LayoutNode> {
        Log.d("AStar", "Finding path from user($startX, $startY) to $goalId on floor $floorLevel")
        val floor = layout.floors.find { it.level == floorLevel } ?: return emptyList()
        val goalNode = floor.nodes.find { it.id == goalId } ?: return emptyList()
        val startNode = LayoutNode(id = "USER", type = "USER", x = startX, y = startY)

        // 1. Create Collision Mask
        val maskWidth = 2000
        val maskHeight = 2000
        val mask = createCollisionMask(floor, maskWidth, maskHeight)

        // 2. Grid-based A* Search
        val step = 15 // Grid resolution. Smaller = more accurate but slower. 15 is a good balance.
        
        val startGrid = GridPoint((startNode.x / step).toInt(), (startNode.y / step).toInt())
        val goalGrid = GridPoint((goalNode.x / step).toInt(), (goalNode.y / step).toInt())

        val openSet = PriorityQueue<NodeWrapper>()
        val allNodes = mutableMapOf<GridPoint, NodeWrapper>()

        val startWrapper = NodeWrapper(startGrid)
        startWrapper.gScore = 0f
        startWrapper.fScore = heuristic(startGrid, goalGrid)
        allNodes[startGrid] = startWrapper
        openSet.add(startWrapper)

        // 8-way movement
        val directions = listOf(
            GridPoint(0, -1), GridPoint(0, 1), GridPoint(-1, 0), GridPoint(1, 0),
            GridPoint(-1, -1), GridPoint(-1, 1), GridPoint(1, -1), GridPoint(1, 1)
        )

        val maxGridX = maskWidth / step
        val maxGridY = maskHeight / step

        while (openSet.isNotEmpty()) {
            val current = openSet.poll() ?: break

            if (current.point == goalGrid || heuristic(current.point, goalGrid) < 2f) { // Close enough
                val path = reconstructPath(current, allNodes, step)
                // Exact start and end points
                if (path.isNotEmpty()) {
                    path[0] = startNode
                    path[path.size - 1] = goalNode
                } else {
                    path.add(startNode)
                    path.add(goalNode)
                }
                Log.d("AStar", "Path found! Nodes: ${path.size}")
                return path
            }

            for (dir in directions) {
                val neighborPoint = GridPoint(current.point.x + dir.x, current.point.y + dir.y)
                
                // Bounds check
                if (neighborPoint.x < 0 || neighborPoint.y < 0 || neighborPoint.x >= maxGridX || neighborPoint.y >= maxGridY) {
                    continue
                }

                // Collision check (check center of grid cell)
                val pixelX = neighborPoint.x * step + (step / 2)
                val pixelY = neighborPoint.y * step + (step / 2)
                
                if (pixelX < maskWidth && pixelY < maskHeight) {
                    val pixelColor = mask.getPixel(pixelX, pixelY)
                    // If not perfectly white, it's blocked
                    if (pixelColor != Color.WHITE) {
                        continue
                    }
                }

                val neighborWrapper = allNodes.getOrPut(neighborPoint) { NodeWrapper(neighborPoint) }
                
                // Distance is 1 for straight, 1.414 for diagonal
                val dist = if (dir.x != 0 && dir.y != 0) 1.414f else 1.0f
                val tentativeGScore = current.gScore + dist

                if (tentativeGScore < neighborWrapper.gScore) {
                    neighborWrapper.previous = current.point
                    neighborWrapper.gScore = tentativeGScore
                    neighborWrapper.fScore = tentativeGScore + heuristic(neighborPoint, goalGrid)
                    
                    if (!openSet.contains(neighborWrapper)) {
                        openSet.add(neighborWrapper)
                    }
                }
            }
        }
        Log.d("AStar", "No path found.")
        return emptyList()
    }

    private fun createCollisionMask(floor: FloorLayout, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Unwalkable background (outside the boundary)
        canvas.drawColor(Color.BLACK)

        val paint = Paint().apply {
            isAntiAlias = true
        }

        // 1. Draw the walkable boundary first
        floor.paths.forEach { layoutPath ->
            if (layoutPath.type == "boundary") {
                val path = PathParser().parsePathString(layoutPath.d).toPath().asAndroidPath()
                
                // Fill the inside as walkable
                paint.color = Color.WHITE
                paint.style = Paint.Style.FILL
                canvas.drawPath(path, paint)
                
                // Add a white stroke to expand the walkable area slightly,
                // ensuring nodes placed exactly on the border aren't trapped in black pixels
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 20f
                canvas.drawPath(path, paint)
            }
        }

        // 2. Draw walls and islands as unwalkable obstacles
        paint.color = Color.BLACK
        floor.paths.forEach { layoutPath ->
            val path = PathParser().parsePathString(layoutPath.d).toPath().asAndroidPath()
            
            when (layoutPath.type) {
                "wall" -> {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 30f // Thicker stroke for padding
                    canvas.drawPath(path, paint)
                }
                "island" -> {
                    // Solid filled shape
                    paint.style = Paint.Style.FILL
                    canvas.drawPath(path, paint)
                    // Add stroke to provide collision padding around the island
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 30f
                    canvas.drawPath(path, paint)
                }
            }
        }
        return bitmap
    }

    private fun heuristic(a: GridPoint, b: GridPoint): Float {
        return hypot((a.x - b.x).toFloat(), (a.y - b.y).toFloat())
    }

    private fun reconstructPath(current: NodeWrapper, allNodes: Map<GridPoint, NodeWrapper>, step: Int): MutableList<LayoutNode> {
        val path = mutableListOf<LayoutNode>()
        var curr: NodeWrapper? = current
        var idCounter = 0
        while (curr != null) {
            val node = LayoutNode(
                id = "path_${idCounter++}",
                type = "WAYPOINT",
                x = (curr.point.x * step + step / 2).toFloat(),
                y = (curr.point.y * step + step / 2).toFloat()
            )
            path.add(node)
            curr = curr.previous?.let { allNodes[it] }
        }
        path.reverse()
        return path
    }
}
