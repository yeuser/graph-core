package me.yeuser.graph.core

interface Graph {

  fun getNodeCount(): Long

  fun getEdgeCount(): Long

  fun addEdge(from: Long, to: Long, type: String, weight: Double, biDirectional: Boolean)

  fun getEdge(from: Long, to: Long): GraphEdge

  fun getEdgeConnections(from: Long): Iterator<GraphEdge> {
    return getEdgeConnectionsOfType(from, null)
  }

  fun getEdgeConnectionsOfType(from: Long, type: String?): Iterator<GraphEdge> {
    return getEdgeConnectionsOfTypeAndWeightInRange(from, type, 0.0, 1.0)
  }

  fun getEdgeConnectionsOfTypeAndWeightInRange(from: Long, type: String?,
                                               minWeight: Double, maxWeight: Double): Iterator<GraphEdge>
}
