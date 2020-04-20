package me.yeuser.graph.blocks.primitivearray

import me.yeuser.graph.blocks.AbstractEdgeIndexer

class PrimitiveArrayEdgeIndexer<T>(precision: Int, vararg edgeTypes: T) :
    AbstractEdgeIndexer<T>(precision, *edgeTypes) {

    private val edges = VerticesMap()
    private val edgesReverse = VerticesSet()

    override fun add(from: Int, to: Int, value: Short) {
        edges.add(from, to, value)
        edgesReverse.add(to, from)
    }

    override fun valueOf(from: Int, to: Int): Short? = edges.get(from, to)

    override fun del(from: Int, to: Int) {
        edges.remove(from, to)
        edgesReverse.remove(to, from)
    }

    override fun connectionsFrom(from: Int): Sequence<Pair<Int, Short>>? = edges.get(from)

    override fun connectionsTo(to: Int): Sequence<Pair<Int, Short>>? =
        edgesReverse.get(to)?.map { it to valueOf(it, to)!! }

    override fun size(): Int = edges.size()
}