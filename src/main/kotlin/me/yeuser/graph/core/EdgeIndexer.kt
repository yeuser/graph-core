package me.yeuser.graph.core

import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.math.roundToInt

class EdgeIndexer<T>(
    private var precision: Int,
    private val edgeTypes: Array<T>
) : IEdgeIndexer<T> {

    private val lock: ReadWriteLock = ReentrantReadWriteLock()
    private val edges = VerticesMap()

    override fun addEdge(
        weight: Double,
        type: T,
        fromIdx: Int,
        toIdx: Int,
        biDirectional: Boolean
    ) {
        val edgeType = edgeTypes.indexOfFirst { it == type }
        val typeWeight = getTypeWeight(edgeType, weight)
        addEdgeInternal(fromIdx, toIdx, typeWeight, edges)
        if (biDirectional) {
            addEdgeInternal(toIdx, fromIdx, typeWeight, edges)
        }
    }

    private fun addEdgeInternal(
        fromIdx: Int,
        toIdx: Int,
        typeWeight: Short,
        connectionTypeMap: VerticesMap
    ) {
        lock.writeLock().lock()
        connectionTypeMap.add(fromIdx, toIdx, typeWeight)
        lock.writeLock().unlock()
    }

    private fun getTypeWeight(t: Int, weight: Double): Short {
        val w = (weight * precision).roundToInt() - 1
        return (t * precision + w).toShort()
    }

    override fun getEdgeTypeAndWeight(fromIdx: Int, toIdx: Int): TypeWeight<T> {
        lock.readLock().lock()
        val typeWeight = edges.get(fromIdx, toIdx)
        lock.readLock().unlock()
        if (typeWeight == null) throw RuntimeException("The vertex was not found!")
        return Pair(edgeTypes[getEdgeType(typeWeight)], getWeight(typeWeight))
    }

    private fun getEdgeType(typeWeight: Short): Int {
        return (typeWeight.toInt() and 0xFFFF) / precision
    }

    private fun getWeight(typeWeight: Short): Double {
        return ((typeWeight.toInt() and 0xFFFF) % precision + 1.0) / precision
    }

    override fun getConnections(fromIdx: Int): Sequence<Edge<T>> {
        lock.readLock().lock()
        val cons = edges.get(fromIdx).orEmpty()
            .map { (toIdx, typeWeight) ->
                Edge(fromIdx, toIdx, edgeTypes[getEdgeType(typeWeight)], getWeight(typeWeight))
            }
        lock.readLock().unlock()
        return cons.asSequence()
    }

    override fun getEdgeCount(): Int {
        lock.readLock().lock()
        val size = edges.size()
        lock.readLock().unlock()
        return size
    }
}