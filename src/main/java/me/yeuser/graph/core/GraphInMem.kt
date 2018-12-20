package me.yeuser.graph.core

import com.google.common.base.Preconditions

class GraphInMem(
  expectedNumberOfNodes: Int,
  expectedNumberOfEdges: Int,
  precision: Int,
  vararg edgeTypes: String
) : Graph {
  private val nodeIndexer: INodeIndexer = NodeIndexer(expectedNumberOfNodes)
  private val edgeIndexer: IEdgeIndexer =
    EdgeIndexer(expectedNumberOfEdges, precision, edgeTypes.size)
  private val edgeTypeMap: Map<String, Int>
  private val edgeTypes: Array<String>

  init {
    assert(precision > 1) { "Precision must be bigger than 1." }
    assert(edgeTypes.size * precision <= 0xFFFF) {
      "Given number of precision and number of edgeTypes are more than acceptable range."
    }
    this.edgeTypeMap = (0 until edgeTypes.size).associateBy { i -> edgeTypes[i] }
    this.edgeTypes = edgeTypes.map { it }.toTypedArray()
  }

  override fun addEdge(from: Long, to: Long, type: String, weight: Double, biDirectional: Boolean) {
    assert(weight > 0 && weight <= 1) {
      "Weight value of an edge must always be in range (0,1]."
    }
    val fromIdx = nodeIndexer.indexOf(from)
    val toIdx = nodeIndexer.indexOf(to)
    assert(edgeTypeMap.contains(type)) {
      "Given `type` is unknown!"
    }
    val edgeType = edgeTypeMap[type]!!
    this.edgeIndexer.addEdge(weight, edgeType, fromIdx, toIdx, biDirectional)
  }

  override fun getEdge(from: Long, to: Long): GraphEdge {
    val fromIdx = nodeIndexer.indexOf(from)
    Preconditions.checkState(fromIdx >= 0, "Given `from` node was not found!")
    val toIdx = nodeIndexer.indexOf(to)
    Preconditions.checkState(toIdx >= 0, "Given `to` node was not found!")
    val (type, weight) = this.edgeIndexer.getEdgeTypeAndWeight(fromIdx, toIdx)
    return GraphEdge(from, to, edgeTypes[type], weight)
  }

  override fun getEdgeConnectionsOfTypeAndWeightInRange(
    from: Long, type: String?,
    minWeight: Double, maxWeight: Double
  ): Iterator<GraphEdge> {
    val fromIdx = nodeIndexer.indexOf(from)
    Preconditions.checkState(fromIdx >= 0, "Given `from` node was not found!")
    Preconditions.checkState(type == null || edgeTypeMap.contains(type), "Given `type` is unknown!")
    return edgeIndexer.getConnectionsByType(if (type == null) -1 else edgeTypeMap[type]!!, fromIdx)
      .asSequence()
      .flatMap { it.get(fromIdx).asSequence() }
      .map { toIdx ->
        edgeIndexer.tw(fromIdx, toIdx, minWeight, maxWeight)?.let { (type, weight) ->
          GraphEdge(from, nodeIndexer.fromIndex(toIdx), edgeTypes[type], weight)
        }
      }
      .filterNotNull()
      .iterator()
  }

  override fun getNodeCount(): Long {
    return nodeIndexer.size()
  }

  override fun getEdgeCount(): Long {
    return this.edgeIndexer.getEdgeCount()
  }
}