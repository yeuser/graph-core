package me.yeuser.graph.core

interface Graph {

  fun getNodeCount(): Long

  fun getEdgeCount(): Long

  fun addEdge(from: Long, to: Long, type: String, weight: Double, biDirectional: Boolean)

  fun getEdge(from: Long, to: Long): GraphEdge

  fun getEdgeConnections(from: Long): Iterator<GraphEdge> = getEdgeConnectionsOfType(from, null)

  fun getEdgeConnectionsOfType(
    from: Long, type: String?
  ): Iterator<GraphEdge> = getEdgeConnectionsOfTypeAndWeightInRange(from, type, 0.0, 1.0)

  fun getEdgeConnectionsOfTypeAndWeightInRange(
    from: Long, type: String?, minWeight: Double, maxWeight: Double
  ): Iterator<GraphEdge>
}

data class GraphEdge internal constructor(
  val from: Long, val to: Long, val edgeType: String, val weight: Double
)
