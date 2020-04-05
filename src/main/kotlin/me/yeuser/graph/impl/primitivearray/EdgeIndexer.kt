package me.yeuser.graph.impl.primitivearray

import me.yeuser.graph.core.AbstractEdgeIndexer

class EdgeIndexer<T>(precision: Int, vararg edgeTypes: T) : AbstractEdgeIndexer<T>(precision, *edgeTypes) {

    private val edges = VerticesMap()

    override fun add(fromIdx: Int, toIdx: Int, value: Short) = edges.add(fromIdx, toIdx, value)

    override fun valueOf(fromIdx: Int, toIdx: Int): Short? = edges.get(fromIdx, toIdx)

    override fun del(fromIdx: Int, toIdx: Int) = edges.remove(fromIdx, toIdx)

    override fun connectionsFrom(fromIdx: Int): Sequence<Pair<Int, Short>>? = edges.get(fromIdx)

    override fun size(): Int = edges.size()
}