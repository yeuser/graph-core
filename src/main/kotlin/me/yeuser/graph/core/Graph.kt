package me.yeuser.graph.core

import me.yeuser.graph.impl.fastmap.FastMapEdgeIndexer
import me.yeuser.graph.impl.primitivearray.PrimitiveArrayEdgeIndexer

class Graph<T> private constructor(private val edgeIndexer: IEdgeIndexer<T>) {

    companion object {
        fun <T> createWithPrimitiveArrays(precision: Int, vararg edgeTypes: T): Graph<T> =
            Graph(PrimitiveArrayEdgeIndexer(precision, *edgeTypes))

        fun <T> createWithFastMap(precision: Int, vararg edgeTypes: T): Graph<T> =
            Graph(FastMapEdgeIndexer(10_000, precision, *edgeTypes))
    }

    private val nodeIndexer = NodeIndexer()

    fun addEdge(
        from: Long,
        to: Long,
        type: T,
        weight: Double,
        biDirectional: Boolean
    ) {
        assert(weight > 0 && weight <= 1) {
            "Weight value of an edge must always be in range (0,1]."
        }
        val fromIdx = nodeIndexer.indexOf(from)
        val toIdx = nodeIndexer.indexOf(to)
        this.edgeIndexer.addEdge(fromIdx, toIdx, type, weight, biDirectional)
    }

    fun removeEdge(
        from: Long,
        to: Long,
        biDirectional: Boolean
    ) {
        val fromIdx = nodeIndexer.indexOf(from)
        val toIdx = nodeIndexer.indexOf(to)
        this.edgeIndexer.removeEdge(fromIdx, toIdx, biDirectional)
    }

    fun getEdge(from: Long, to: Long): GraphEdge<T> {
        val fromIdx = nodeIndexer.indexOf(from)
        val toIdx = nodeIndexer.indexOf(to)
        val edge = this.edgeIndexer.getEdge(fromIdx, toIdx) ?: throw GraphEdgeNotFound(from, to)
        return GraphEdge(from, to, edge.type, edge.weight)
    }

    fun getEdgeConnections(
        from: Long,
        type: T? = null,
        minWeight: Double = 0.0,
        maxWeight: Double = 1.0
    ): Sequence<GraphEdge<T>> {
        val fromIdx = nodeIndexer.indexOf(from)
        return edgeIndexer.getConnections(fromIdx, type)
            .filter { edge -> edge.weight in minWeight..maxWeight }
            .map { GraphEdge(from, nodeIndexer.fromIndex(it.toIdx), it.type, it.weight) }
    }

    fun getNodeCount(): Int {
        return nodeIndexer.size()
    }

    fun getEdgeCount(): Int {
        return this.edgeIndexer.getEdgeCount()
    }
}

data class GraphEdge<T> internal constructor(
    val from: Long,
    val to: Long,
    val edgeType: T,
    val weight: Double
)

class GraphEdgeNotFound(val from: Long, val to: Long) : Exception("Graph has no edge from $from to $to.")

interface IEdgeIndexer<T> {
    fun addEdge(fromIdx: Int, toIdx: Int, type: T, weight: Double, biDirectional: Boolean)
    fun removeEdge(fromIdx: Int, toIdx: Int, biDirectional: Boolean)
    fun getEdge(fromIdx: Int, toIdx: Int): Edge<T>?
    fun getConnections(fromIdx: Int, type: T? = null): Sequence<Edge<T>>
    fun getEdgeCount(): Int
}

typealias TypeWeight<T> = Pair<T, Double>

data class Edge<T>(val fromIdx: Int, val toIdx: Int, val type: T, val weight: Double)

interface INodeIndexer {
    fun indexOf(node: Long): Int
    fun fromIndex(index: Int): Long
    fun size(): Int
}