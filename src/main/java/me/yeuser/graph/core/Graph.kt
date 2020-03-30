package me.yeuser.graph.core

interface Graph<T : Comparable<T>> {

    fun getNodeCount(): Int

    fun getEdgeCount(): Int

    fun addEdge(from: Long, to: Long, type: T, weight: Double, biDirectional: Boolean)

    fun getEdge(from: Long, to: Long): GraphEdge<T>

    fun getEdgeConnections(
        from: Long,
        type: T? = null,
        minWeight: Double = 0.0,
        maxWeight: Double = 1.0
    ): Iterator<GraphEdge<T>>
}

data class GraphEdge<T> internal constructor(
    val from: Long,
    val to: Long,
    val edgeType: T,
    val weight: Double
)