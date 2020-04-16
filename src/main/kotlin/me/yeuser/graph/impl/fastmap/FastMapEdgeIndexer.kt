package me.yeuser.graph.impl.fastmap

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntRBTreeSet
import it.unimi.dsi.fastutil.ints.IntSet
import it.unimi.dsi.fastutil.longs.Long2ShortMap
import it.unimi.dsi.fastutil.longs.Long2ShortOpenHashMap
import me.yeuser.graph.core.AbstractEdgeIndexer

class FastMapEdgeIndexer<T>(
    expectedNumberOfEdges: Int,
    precision: Int,
    vararg edgeTypes: T
) : AbstractEdgeIndexer<T>(precision, *edgeTypes) {

    private val edgeValues: Long2ShortMap = Long2ShortOpenHashMap(expectedNumberOfEdges, 1f)
    private val edges: Int2ObjectMap<IntSet> = Int2ObjectOpenHashMap(4096, 1f)
    private val edgesReverse: Int2ObjectMap<IntSet> = Int2ObjectOpenHashMap(4096, 1f)

    override fun add(fromIdx: Int, toIdx: Int, value: Short) {
        val fromTo = getFromTo(fromIdx, toIdx)
        edgeValues[fromTo] = value
        edges.computeIfAbsent(fromIdx) { IntRBTreeSet() }.add(toIdx)
        edgesReverse.computeIfAbsent(toIdx) { IntRBTreeSet() }.add(fromIdx)
    }

    override fun valueOf(fromIdx: Int, toIdx: Int): Short? {
        val fromTo = getFromTo(fromIdx, toIdx)
        if (!edgeValues.containsKey(fromTo)) return null
        return edgeValues.get(fromTo)
    }

    override fun del(fromIdx: Int, toIdx: Int) {
        edges[fromIdx]?.remove(toIdx)
        edgesReverse[toIdx]?.remove(fromIdx)
        edgeValues.remove(getFromTo(fromIdx, toIdx))
    }

    override fun connectionsFrom(fromIdx: Int): Sequence<Pair<Int, Short>>? {
        return edges[fromIdx]?.asSequence()
            ?.map { toIdx -> toIdx to valueOf(fromIdx, toIdx)!! }
    }

    override fun connectionsTo(toIdx: Int): Sequence<Pair<Int, Short>>? {
        return edgesReverse[toIdx]?.asSequence()
            ?.map { fromIdx -> toIdx to valueOf(fromIdx, toIdx)!! }
    }

    override fun size(): Int {
        return edges.size
    }

    private fun getFromTo(fromIdx: Int, toIdx: Int): Long {
        return fromIdx.toLong() and 0xFFFFFFFFL shl 32 or (toIdx.toLong() and 0xFFFFFFFFL)
    }
}