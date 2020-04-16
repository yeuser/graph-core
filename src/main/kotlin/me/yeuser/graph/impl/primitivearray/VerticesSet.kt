package me.yeuser.graph.impl.primitivearray

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntRBTreeSet
import kotlin.math.max
import kotlin.math.min

internal class VerticesSet(
    private val bufferOpsLimit: Int = 8 * 1024,
    private val nodeEdgeExpansionMax: Int = 1024 * 1024,
    private val nodeEdgeExpansionMin: Int = 1024
) {

    private val vertices: Int2ObjectOpenHashMap<MutableSet<Int>> = Int2ObjectOpenHashMap(32 * 1024, 1f)
    private var verticesCompact: Array<BigIntSet?> = emptyArray()
    private var cOps = 0

    fun add(fromIdx: Int, toIdx: Int) {
        addAll(listOf(fromIdx to toIdx))
    }

    fun addAll(fromToPairs: Iterable<Pair<Int, Int>>) {
        fromToPairs
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.distinct() }
            .forEach { (fromIdx, toIdcs) ->
                vertices.computeIfAbsent(fromIdx) { IntRBTreeSet() }.addAll(toIdcs)
            }
        shrink(ops = fromToPairs.count())
    }

    fun removeAll(fromToPairs: Iterable<Pair<Int, Int>>) {
        fromToPairs
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.distinct() }
            .forEach { (fromIdx, toIdcs) ->
                vertices.computeIfPresent(fromIdx) { _, set -> set.removeAll(toIdcs); set }
                verticesCompact.getOrNull(fromIdx)
                    ?.removeAll(toIdcs)
            }
        shrink(ops = fromToPairs.count())
    }

    fun remove(fromIdx: Int, toIdx: Int) {
        removeAll(setOf(fromIdx to toIdx))
    }

    private fun shrink(force: Boolean = false, ops: Int = 1) {
        cOps += ops
        if (force || cOps > bufferOpsLimit) {
            compress()
            cOps = 0
        }
    }

    private fun compress() {
        vertices.forEach { (key, value) ->
            if (verticesCompact.size <= key) {
                val delta = min(
                    nodeEdgeExpansionMax,
                    max(nodeEdgeExpansionMin, key + 1 - verticesCompact.size) * 2
                )
                verticesCompact = verticesCompact.copyOf(verticesCompact.size + delta)
            }
            var v = verticesCompact[key]
            if (v == null) {
                v = BigIntSet()
                verticesCompact[key] = v
            }
            v.addAll(value)
        }
        vertices.clear()
    }

    fun get(fromIdx: Int): Sequence<Int>? {
        val rbTreeSet = sequenceOf(
            vertices.get(fromIdx)?.asSequence(),
            verticesCompact.getOrNull(fromIdx)?.asSequence()
        ).flatMap { it?.asSequence().orEmpty() }
        shrink()
        return rbTreeSet.takeIf { it.any() }
    }
}