package me.yeuser.graph.core

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2ShortAVLTreeMap
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.math.max
import kotlin.math.min

class VerticesMap(
    private val bufferOpsLimit: Int = 8 * 1024,
    private val nodeEdgeExpansionMax: Int = 1024 * 1024,
    private val nodeEdgeExpansionMin: Int = 1024
) {

    private val lock: ReadWriteLock = ReentrantReadWriteLock()
    private val vertices: Int2ObjectOpenHashMap<MutableMap<Int, Short>> = Int2ObjectOpenHashMap(32 * 1024, 1f)
    private var verticesCompact: Array<BigInt2Short?> = emptyArray()
    private var cOps = 0

    fun add(fromIdx: Int, toIdx: Int, value: Short) {
        lock.writeLock().lock()
        vertices.computeIfAbsent(fromIdx) { Int2ShortAVLTreeMap() }[toIdx] = value
        lock.writeLock().unlock()
        shrink()
    }

    fun remove(fromIdx: Int, toIdx: Int) {
        lock.writeLock().lock()
        vertices.computeIfPresent(fromIdx) { _, map -> map.remove(toIdx);map }
        verticesCompact.getOrNull(fromIdx)
            ?.takeIf { it.has(toIdx) }
            ?.addAll(mapOf(toIdx to (-1).toShort()))
        lock.writeLock().unlock()
        shrink()
    }

    private fun shrink(force: Boolean = false) {
        lock.writeLock().lock()
        cOps++
        if (force || cOps > bufferOpsLimit) {
            compress()
            cOps = 0
        }
        lock.writeLock().unlock()
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
        lock.readLock().lock()
        val rbTreeSet = sequenceOf(
            vertices.get(fromIdx)?.asSequence()?.map { it.toPair() },
            verticesCompact.takeIf { it.size > fromIdx }
                ?.get(fromIdx)
                ?.asSequence()
        ).flatMap { it?.asSequence().orEmpty() }
        lock.readLock().unlock()
        shrink()
        return rbTreeSet.takeIf { it.any() }
    }

    fun get(fromIdx: Int, toIdx: Int): Short? {
        lock.readLock().lock()
        val value = vertices[fromIdx]?.get(toIdx)
            ?: verticesCompact.getOrNull(fromIdx)?.getValue(toIdx)
        lock.readLock().unlock()
        return value
    }

    fun size(): Int {
        shrink(force = true)
        lock.readLock().lock()
        val size = verticesCompact.sumBy { it?.size ?: 0 }
        lock.readLock().unlock()
        return size
    }
}