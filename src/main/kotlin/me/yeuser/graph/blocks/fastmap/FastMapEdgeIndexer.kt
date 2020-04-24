package me.yeuser.graph.blocks.fastmap

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntRBTreeSet
import it.unimi.dsi.fastutil.ints.IntSet
import it.unimi.dsi.fastutil.longs.Long2ShortMap
import it.unimi.dsi.fastutil.longs.Long2ShortOpenHashMap
import me.yeuser.graph.blocks.AbstractEdgeIndexer

class FastMapEdgeIndexer<T>(
    expectedNumberOfEdges: Int,
    precision: Int,
    vararg edgeTypes: T
) : AbstractEdgeIndexer<T>(precision, *edgeTypes) {

    private val edgeValues: Long2ShortMap = Long2ShortOpenHashMap(expectedNumberOfEdges, 1f)
    private val edges: Int2ObjectMap<IntSet> = Int2ObjectOpenHashMap(4096, 1f)
    private val edgesReverse: Int2ObjectMap<IntSet> = Int2ObjectOpenHashMap(4096, 1f)
    override val minWeight: Double? = 0.0
    override val maxWeight: Double? = 1.0

    override fun add(from: Int, to: Int, value: Short) {
        val fromTo = getFromTo(from, to)
        edgeValues[fromTo] = value
        edges.computeIfAbsent(from) { IntRBTreeSet() }.add(to)
        edgesReverse.computeIfAbsent(to) { IntRBTreeSet() }.add(from)
    }

    override fun valueOf(from: Int, to: Int): Short? {
        val fromTo = getFromTo(from, to)
        if (!edgeValues.containsKey(fromTo)) return null
        return edgeValues.get(fromTo)
    }

    override fun del(from: Int, to: Int) {
        edges[from]?.remove(to)
        edgesReverse[to]?.remove(from)
        edgeValues.remove(getFromTo(from, to))
    }

    override fun connectionsFrom(from: Int): Sequence<Pair<Int, Short>>? {
        return edges[from]?.asSequence()
            ?.map { to -> to to valueOf(from, to)!! }
    }

    override fun connectionsTo(to: Int): Sequence<Pair<Int, Short>>? {
        return edgesReverse[to]?.asSequence()
            ?.map { from -> from to valueOf(from, to)!! }
    }

    override fun size(): Int {
        return edges.size
    }

    private fun getFromTo(from: Int, to: Int): Long {
        return from.toLong() and 0xFFFFFFFFL shl 32 or (to.toLong() and 0xFFFFFFFFL)
    }
}