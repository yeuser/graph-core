package me.yeuser.graph.core

import com.google.common.base.Preconditions

class GraphInMem(
    expectedNumberOfNodes: Int,
    expectedNumberOfEdges: Int,
    precision: Int,
    vararg edgeTypes: String
) : Graph {

    init {
        assert(precision > 1) { "Precision must be bigger than 1." }
        assert(edgeTypes.size * precision <= 0xFFFF) {
            "Given number of precision and number of edgeTypes are more than acceptable range."
        }
    }

    private val edgeTypes: Array<String> = edgeTypes.distinct().toTypedArray()

    private val nodeIndexer: INodeIndexer = NodeIndexer(expectedNumberOfNodes)
    private val edgeIndexer: IEdgeIndexer<String> = EdgeIndexer(
        expectedNumberOfEdges, precision, this.edgeTypes
    )

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

        this.edgeIndexer.addEdge(weight, type, fromIdx, toIdx, biDirectional)
    }

    override fun getEdge(from: Long, to: Long): GraphEdge {
        val fromIdx = nodeIndexer.indexOf(from)
        Preconditions.checkState(fromIdx >= 0, "Given `from` node was not found!")
        val toIdx = nodeIndexer.indexOf(to)
        Preconditions.checkState(toIdx >= 0, "Given `to` node was not found!")
        val (type, weight) = this.edgeIndexer.getEdgeTypeAndWeight(fromIdx, toIdx)
        return GraphEdge(from, to, type, weight)
    }

    override fun getEdgeConnectionsOfTypeAndWeightInRange(
        from: Long,
        type: String?,
        minWeight: Double,
        maxWeight: Double
    ): Iterator<GraphEdge> {
        val fromIdx = nodeIndexer.indexOf(from)
        Preconditions.checkState(fromIdx >= 0, "Given `from` node was not found!")
        Preconditions.checkState(
            type == null || edgeTypes.contains(type),
            "Given `type` is unknown!"
        )
        return edgeIndexer.getConnectionsByType(type, fromIdx).mapNotNull { toIdx ->
            edgeIndexer.getEdgeTypeAndWeight(fromIdx, toIdx).takeUnless { (_, weight) ->
                weight < minWeight || weight > maxWeight
            }?.let { (type, weight) ->
                GraphEdge(from, nodeIndexer.fromIndex(toIdx), type, weight)
            }
        }.iterator()
    }

    override fun getNodeCount(): Long {
        return nodeIndexer.size()
    }

    override fun getEdgeCount(): Long {
        return this.edgeIndexer.getEdgeCount()
    }
}