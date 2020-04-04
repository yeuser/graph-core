package me.yeuser.graph.core

import java.lang.Exception

interface Graph<T : Comparable<T>> {

    fun getNodeCount(): Int

    fun getEdgeCount(): Int

    fun addEdge(from: Long, to: Long, type: T, weight: Double, biDirectional: Boolean)

    fun removeEdge(from: Long, to: Long, biDirectional: Boolean)

    fun getEdge(from: Long, to: Long): GraphEdge<T>

    fun getEdgeConnections(
        from: Long,
        type: T? = null,
        minWeight: Double = 0.0,
        maxWeight: Double = 1.0
    ): Sequence<GraphEdge<T>>
}

data class GraphEdge<T> internal constructor(
    val from: Long,
    val to: Long,
    val edgeType: T,
    val weight: Double
)

class GraphEdgeNotFound(val from: Long, val to: Long) : Exception("Graph has no edge from $from to $to.")