package me.yeuser.graph.core

import com.google.common.base.Preconditions
import it.unimi.dsi.fastutil.longs.Long2ShortMap
import it.unimi.dsi.fastutil.longs.Long2ShortOpenHashMap
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.math.roundToInt

class EdgeIndexer<T>(
    expectedNumberOfEdges: Int,
    private var precision: Int,
    private val edgeTypes: Array<T>
) : IEdgeIndexer<T> {

    private val lock: ReadWriteLock = ReentrantReadWriteLock()
    private val edges: Long2ShortMap = Long2ShortOpenHashMap(expectedNumberOfEdges, 1f)
    private val edgesByType: Array<VerticesMap> =
        (1..edgeTypes.size).map { VerticesMap() }.toTypedArray()

    override fun addEdge(
        weight: Double,
        type: T,
        fromIdx: Int,
        toIdx: Int,
        biDirectional: Boolean
    ) {
        val edgeType = edgeTypes.indexOfFirst { it == type }
        val typeWeight = getTypeWeight(edgeType, weight)
        val connectionTypeMap = edgesByType[edgeType]
        addEdgeInternal(fromIdx, toIdx, typeWeight, connectionTypeMap)
        if (biDirectional) {
            addEdgeInternal(toIdx, fromIdx, typeWeight, connectionTypeMap)
        }
    }

    private fun addEdgeInternal(
        fromIdx: Int,
        toIdx: Int,
        typeWeight: Short,
        connectionTypeMap: VerticesMap
    ) {
        lock.writeLock().lock()
        val fromTo = getFromTo(fromIdx, toIdx)
        edges[fromTo] = typeWeight
        connectionTypeMap.add(fromIdx, toIdx)
        lock.writeLock().unlock()
    }

    private fun getFromTo(fromIdx: Int, toIdx: Int): Long {
        return fromIdx.toLong() and 0xFFFFFFFFL shl 32 or (toIdx.toLong() and 0xFFFFFFFFL)
    }

    private fun getTypeWeight(t: Int, weight: Double): Short {
        val w = (weight * precision).roundToInt() - 1
        return (t * precision + w).toShort()
    }

    override fun getEdgeTypeAndWeight(fromIdx: Int, toIdx: Int): TypeWeight<T> {
        val fromTo = getFromTo(fromIdx, toIdx)
        lock.readLock().lock()
        Preconditions.checkState(edgesByType.any { it.has(fromIdx, toIdx) }, "Given `from->to` edge was not found!")
        val typeWeight = edges.get(fromTo)
        lock.readLock().unlock()
        return Pair(edgeTypes[getEdgeType(typeWeight)], getWeight(typeWeight))
    }

    private fun getEdgeType(typeWeight: Short): Int {
        return (typeWeight.toInt() and 0xFFFF) / precision
    }

    private fun getWeight(typeWeight: Short): Double {
        return ((typeWeight.toInt() and 0xFFFF) % precision + 1.0) / precision
    }

    override fun getConnectionsByType(type: T?, fromIdx: Int): List<Edge<T>> {
        Preconditions.checkState(
            type == null || edgeTypes.contains(type),
            "Given `type` is unknown!"
        )
        lock.readLock().lock()
        val cons =
            edgesByType
                .filterIndexed { index, _ -> type == null || type == edgeTypes[index] }
                .mapNotNull { it.get(fromIdx) }
                .flatten().map {
                    val (t, w) = getEdgeTypeAndWeight(fromIdx, it)
                    Edge(fromIdx, it, t, w)
                }
        lock.readLock().unlock()
        return cons
    }

    override fun getEdgeCount(): Int {
        lock.readLock().lock()
        val size = edgesByType.sumBy { it.size() }
        lock.readLock().unlock()
        return size
    }
}