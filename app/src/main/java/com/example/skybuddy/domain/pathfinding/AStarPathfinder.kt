package com.example.skybuddy.domain.pathfinding

import com.example.skybuddy.data.repository.LayoutEdge
import com.example.skybuddy.data.repository.LayoutNode
import com.example.skybuddy.data.repository.MapLayout
import java.util.PriorityQueue
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
        val floor = layout.floors.find { it.level == floorLevel } ?: return emptyList()
        
        val startNode = floor.nodes.find { it.id == startId } ?: return emptyList()
        val goalNode = floor.nodes.find { it.id == goalId } ?: return emptyList()

        val openSet = PriorityQueue<NodeWrapper>()
        val allNodes = floor.nodes.associateWith { NodeWrapper(it) }

        val startWrapper = allNodes[startNode]!!
        startWrapper.gScore = 0f
        startWrapper.fScore = heuristic(startNode, goalNode)
        openSet.add(startWrapper)

        while (openSet.isNotEmpty()) {
            val current = openSet.poll() ?: break

            if (current.node.id == goalId) {
                return reconstructPath(current, allNodes)
            }

            val neighbors = getNeighbors(current.node, floor.edges, floor.nodes)
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
        return emptyList()
    }

    private fun getNeighbors(node: LayoutNode, edges: List<LayoutEdge>, nodes: List<LayoutNode>): List<LayoutNode> {
        val neighbors = mutableListOf<LayoutNode>()
        for (edge in edges) {
            if (edge.from == node.id) {
                nodes.find { it.id == edge.to }?.let { neighbors.add(it) }
            } else if (edge.to == node.id) {
                nodes.find { it.id == edge.from }?.let { neighbors.add(it) }
            }
        }
        return neighbors
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
