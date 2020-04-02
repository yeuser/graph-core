package me.yeuser.graph.core

import com.google.common.base.Preconditions

class GraphInMem(
    expectedNumberOfNodes: Int,
    precision: Int,
    vararg edgeTypes: String
) : Graph<String> {

    init {
        assert(precision > 1) { "Precision must be bigger than 1." }
        assert(edgeTypes.size * precision <= 0xFFFF) {
            "Given number of precision and number of edgeTypes are more than acceptable range."
        }
    }

    private val edgeTypes: Array<String> = edgeTypes.distinct().toTypedArray()
    private val nodeIndexer: INodeIndexer = NodeIndexer(expectedNumberOfNodes)
    private val edgeIndexer: IEdgeIndexer<String> = EdgeIndexer(precision, this.edgeTypes)

    override fun addEdge(
        from: Long,
        to: Long,
        type: String,
        weight: Double,
        biDirectional: Boolean
    ) {
        assert(weight > 0 && weight <= 1) {
            "Weight value of an edge must always be in range (0,1]."
        }
        val fromIdx = nodeIndexer.indexOf(from)
        val toIdx = nodeIndexer.indexOf(to)
        assert(edgeTypes.contains(type)) {
            "Given `type` is unknown!"
        }

        this.edgeIndexer.addEdge(fromIdx, toIdx, type, weight, biDirectional)
    }

    override fun removeEdge(
        from: Long,
        to: Long,
        biDirectional: Boolean
    ) {
        val fromIdx = nodeIndexer.indexOf(from)
        val toIdx = nodeIndexer.indexOf(to)
        this.edgeIndexer.removeEdge(fromIdx, toIdx, biDirectional)
    }

    override fun getEdge(from: Long, to: Long): GraphEdge<String> {
        val fromIdx = nodeIndexer.indexOf(from)
        Preconditions.checkState(fromIdx >= 0, "Given `from` node was not found!")
        val toIdx = nodeIndexer.indexOf(to)
        Preconditions.checkState(toIdx >= 0, "Given `to` node was not found!")
        val (type, weight) = this.edgeIndexer.getEdgeTypeAndWeight(fromIdx, toIdx) ?: throw GraphEdgeNotFound(from, to)
        return GraphEdge(from, to, type, weight)
    }

    override fun getEdgeConnections(
        from: Long,
        type: String?,
        minWeight: Double,
        maxWeight: Double
    ): Sequence<GraphEdge<String>> {
        val fromIdx = nodeIndexer.indexOf(from)
        Preconditions.checkState(fromIdx >= 0, "Given `from` node was not found!")
        Preconditions.checkState(
            type == null || edgeTypes.contains(type),
            "Given `type` is unknown!"
        )
        return edgeIndexer.getConnections(fromIdx)
            .run { if (type == null) this else filter { edge -> edge.type == type } }
            .filter { edge -> edge.weight in minWeight..maxWeight }
            .map { GraphEdge(from, nodeIndexer.fromIndex(it.toIdx), it.type, it.weight) }
    }

    override fun getNodeCount(): Int {
        return nodeIndexer.size()
    }

    override fun getEdgeCount(): Int {
        return this.edgeIndexer.getEdgeCount()
    }
}