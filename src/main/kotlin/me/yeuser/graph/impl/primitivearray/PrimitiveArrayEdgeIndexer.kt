package me.yeuser.graph.impl.primitivearray

import me.yeuser.graph.core.AbstractEdgeIndexer

class PrimitiveArrayEdgeIndexer<T>(precision: Int, vararg edgeTypes: T) :
    AbstractEdgeIndexer<T>(precision, *edgeTypes) {

    private val edges = VerticesMap()
    private val edgesReverse = VerticesSet()

    override fun add(fromIdx: Int, toIdx: Int, value: Short) {
        edges.add(fromIdx, toIdx, value)
        edgesReverse.add(toIdx, fromIdx)
    }

    override fun valueOf(fromIdx: Int, toIdx: Int): Short? = edges.get(fromIdx, toIdx)

    override fun del(fromIdx: Int, toIdx: Int) {
        edges.remove(fromIdx, toIdx)
        edgesReverse.remove(toIdx, fromIdx)
    }

    override fun connectionsFrom(fromIdx: Int): Sequence<Pair<Int, Short>>? = edges.get(fromIdx)

    override fun connectionsTo(toIdx: Int): Sequence<Pair<Int, Short>>? =
        edgesReverse.get(toIdx)?.map { it to valueOf(it, toIdx)!! }

    override fun size(): Int = edges.size()
}