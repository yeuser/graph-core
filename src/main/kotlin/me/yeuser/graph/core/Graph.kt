package me.yeuser.graph.core

import me.yeuser.graph.blocks.fastmap.FastMapEdgeIndexer
import me.yeuser.graph.blocks.primitivearray.PrimitiveArrayEdgeIndexer

class Graph<T> private constructor(private val edgeIndexer: EdgeIndexer<T>) {

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
        edgeIndexer.add(fromIdx, toIdx, type, weight, biDirectional)
    }

    fun removeEdge(
        from: Long,
        to: Long,
        biDirectional: Boolean
    ) {
        val fromIdx = nodeIndexer.indexOf(from)
        val toIdx = nodeIndexer.indexOf(to)
        edgeIndexer.remove(fromIdx, toIdx, biDirectional)
    }

    fun getEdge(from: Long, to: Long): GraphEdge<T> {
        val fromIdx = nodeIndexer.indexOf(from)
        val toIdx = nodeIndexer.indexOf(to)
        val edge = edgeIndexer.get(fromIdx, toIdx) ?: throw GraphEdgeNotFound(from, to)
        return GraphEdge(from, to, edge.type, edge.weight)
    }

    fun getEdgeConnectionsFrom(
        from: Long,
        type: T? = null,
        minWeight: Double = 0.0,
        maxWeight: Double = 1.0
    ): Sequence<GraphEdge<T>> {
        val fromIdx = nodeIndexer.indexOf(from)
        return edgeIndexer.allFrom(fromIdx, type)
            .filter { edge -> edge.weight in minWeight..maxWeight }
            .map { GraphEdge(from, nodeIndexer.fromIndex(it.to), it.type, it.weight) }
    }

    fun getEdgeConnectionsTo(
        to: Long,
        type: T? = null,
        minWeight: Double = 0.0,
        maxWeight: Double = 1.0
    ): Sequence<GraphEdge<T>> {
        val toIdx = nodeIndexer.indexOf(to)
        return edgeIndexer.allTo(toIdx, type)
            .filter { edge -> edge.weight in minWeight..maxWeight }
            .map { GraphEdge(nodeIndexer.fromIndex(it.from), to, it.type, it.weight) }
    }

    fun getNodeCount(): Int {
        return nodeIndexer.size()
    }

    fun getEdgeCount(): Int {
        return edgeIndexer.count()
    }
}

data class GraphEdge<T> internal constructor(
    val from: Long,
    val to: Long,
    val edgeType: T,
    val weight: Double
)

class GraphEdgeNotFound(val from: Long, val to: Long) : Exception("Graph has no edge from $from to $to.")

interface EdgeIndexer<T> {
    fun add(from: Int, to: Int, type: T, weight: Double, biDirectional: Boolean)
    fun remove(from: Int, to: Int, biDirectional: Boolean)
    fun get(from: Int, to: Int): Edge<T>?
    fun allFrom(from: Int, type: T? = null): Sequence<Edge<T>>
    fun allTo(to: Int, type: T? = null): Sequence<Edge<T>>
    fun count(): Int
}

data class Edge<T>(val from: Int, val to: Int, val type: T, val weight: Double)

abstract class GraphRouter<T>(
    protected val edges: EdgeIndexer<T>
) {
    abstract fun route(
        from: Int,
        to: Int,
        type: T? = null,
        maxW: Double = Double.MAX_VALUE
    ): List<Pair<Int, Double>>
}