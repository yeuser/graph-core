package me.yeuser.graph.impl.primitivearray

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2ShortAVLTreeMap
import kotlin.math.max
import kotlin.math.min

internal class VerticesMap(
    private val bufferOpsLimit: Int = 8 * 1024,
    private val nodeEdgeExpansionMax: Int = 1024 * 1024,
    private val nodeEdgeExpansionMin: Int = 1024
) {

    private val vertices: Int2ObjectOpenHashMap<MutableMap<Int, Short>> = Int2ObjectOpenHashMap(32 * 1024, 1f)
    private var verticesCompact: Array<BigInt2Short?> = emptyArray()
    private var cOps = 0

    fun add(fromIdx: Int, toIdx: Int, value: Short) {
        vertices.computeIfAbsent(fromIdx) { Int2ShortAVLTreeMap() }[toIdx] = value
        shrink()
    }

    fun remove(fromIdx: Int, toIdx: Int) {
        vertices.computeIfPresent(fromIdx) { _, map -> map.remove(toIdx); map }
        verticesCompact.getOrNull(fromIdx)
            ?.takeIf { it.has(toIdx) }
            ?.removeAll(listOf(toIdx))
        shrink()
    }

    private fun shrink(force: Boolean = false) {
        cOps++
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
                v = BigInt2Short()
                verticesCompact[key] = v
            }
            v.addAll(value)
        }
        vertices.clear()
    }

    fun get(fromIdx: Int): Sequence<Pair<Int, Short>>? {
        val rbTreeSet = sequenceOf(
            vertices.get(fromIdx)?.asSequence()?.map { it.toPair() },
            verticesCompact.getOrNull(fromIdx)?.asSequence()
        ).flatMap { it?.asSequence().orEmpty() }
        shrink()
        return rbTreeSet.takeIf { it.any() }
    }

    fun get(fromIdx: Int, toIdx: Int): Short? {
        val value = vertices[fromIdx]?.get(toIdx)
            ?: verticesCompact.getOrNull(fromIdx)?.getValue(toIdx)
        return value
    }

    fun size(): Int {
        shrink(force = true)
        return verticesCompact.sumBy { it?.size ?: 0 }
    }
}